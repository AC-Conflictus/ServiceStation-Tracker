package edu.austincollege.sstation.web;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TC-120: the custom error pages actually render, and API callers get API answers.
 *
 * <p>Deliberately a real-server test rather than {@code MockMvc}. MockMvc stops at the status code
 * and does not perform the ERROR dispatch, so it can prove a request was rejected but not what the
 * user ends up looking at — which is the entire point of this card. Only a servlet container
 * forwards to {@code /error}, resolves {@code templates/error/<status>.html} and renders it.
 *
 * <p>It drives the JDK's own HTTP client rather than {@code TestRestTemplate} for two reasons:
 * redirect following is explicit (so "anonymous users are sent to sign-in" is asserted rather than
 * silently followed), and {@code HttpURLConnection} cannot complete a POST whose body was streamed
 * when the server answers 401 — which is precisely one of the responses under test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class ErrorPageIntegrationTest {

  /**
   * Spring Boot's default error page calls itself the "Whitelabel Error Page". The whole word is
   * matched, not the full phrase: a live check turned up the term shipping to the browser inside an
   * explanatory HTML comment, which the narrower assertion had walked straight past.
   */
  private static final String WHITELABEL = "Whitelabel";

  private static final Pattern CSRF_INPUT =
      Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"");

  @LocalServerPort private int port;

  private HttpClient http;
  private ListAppender<ILoggingEvent> adviceLogs;

  /** A route that always fails, so the 500 path can be exercised without breaking a real one. */
  @TestConfiguration
  static class BoomConfig {
    /**
     * No {@code @Bean} method: member classes of a configuration class are registered on their own,
     * and declaring both gives an ambiguous mapping.
     */
    @RestController
    static class BoomController {
      @GetMapping("/boom")
      String boom() {
        throw new IllegalStateException("deliberate failure from ErrorPageIntegrationTest");
      }
    }
  }

  @BeforeEach
  void setUp() {
    http =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    adviceLogs = new ListAppender<>();
    adviceLogs.start();
    ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(GlobalErrorAdvice.class))
        .addAppender(adviceLogs);
  }

  @AfterEach
  void tearDown() {
    ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(GlobalErrorAdvice.class))
        .detachAppender(adviceLogs);
  }

  // ---------------------------------------------------------------- HTML error pages

  @Test
  void studentOnAnAdminUrlSeesTheCustom403() throws Exception {
    HttpResponse<String> res = browserGet("/admin", signIn("student", "student_secret"));

    assertThat(res.statusCode()).isEqualTo(403);
    assertThat(res.body())
        .doesNotContain(WHITELABEL)
        .contains("You don't have access to that page")
        // Reads as a permissions message, and names the account so the fix is obvious.
        .contains("signed in as")
        .contains("student");
    assertRendersInsideTheAppShell(res.body());
  }

  @Test
  void unknownPathRendersTheCustom404() throws Exception {
    HttpResponse<String> res = browserGet("/no-such-page", signIn("admin", "admin_secret"));

    assertThat(res.statusCode()).isEqualTo(404);
    assertThat(res.body())
        .doesNotContain(WHITELABEL)
        .contains("That page doesn't exist")
        .contains("/no-such-page");
    assertRendersInsideTheAppShell(res.body());
  }

  @Test
  void uncaughtExceptionRendersTheCustom500() throws Exception {
    HttpResponse<String> res = browserGet("/boom", signIn("admin", "admin_secret"));

    assertThat(res.statusCode()).isEqualTo(500);
    assertThat(res.body())
        .doesNotContain(WHITELABEL)
        .contains("Something went wrong on our end")
        .contains("/boom")
        // The page tells the user to report the address, not to read a stack trace.
        .doesNotContain("IllegalStateException")
        .doesNotContain("deliberate failure");
    assertRendersInsideTheAppShell(res.body());
  }

  @Test
  void statusWithoutItsOwnPageFallsBackToTheCatchAll() throws Exception {
    // GET on the POST-only quick-approve endpoint -> 405, which has no error/405.html. It must
    // land on templates/error.html rather than falling through to Whitelabel.
    HttpResponse<String> res = browserGet("/admin/hours/1/status", signIn("admin", "admin_secret"));

    assertThat(res.statusCode()).isEqualTo(405);
    assertThat(res.body())
        .doesNotContain(WHITELABEL)
        .contains(">405<")
        .contains("could not be completed");
    assertRendersInsideTheAppShell(res.body());
  }

  @Test
  void errorPagesDoNotLeakInternals() throws Exception {
    HttpResponse<String> res = browserGet("/boom", signIn("admin", "admin_secret"));

    // server.error.include-message / include-stacktrace stay at their "never" defaults.
    assertThat(res.body())
        .doesNotContain("java.lang.")
        .doesNotContain("org.springframework.")
        .doesNotContain("Exception");
  }

  @Test
  void uncaughtExceptionIsLoggedAtErrorWithTheRequestPath() throws Exception {
    browserGet("/boom", signIn("admin", "admin_secret"));

    assertThat(adviceLogs.list)
        .as("the 500 page shows no detail, so the log is the only record")
        .anySatisfy(
            event -> {
              assertThat(event.getLevel()).isEqualTo(Level.ERROR);
              assertThat(event.getFormattedMessage()).contains("/boom").contains("GET");
              assertThat(event.getThrowableProxy().getMessage()).contains("deliberate failure");
            });
  }

  @Test
  void anonymousBrowserIsSentToSignInNotAnErrorPage() throws Exception {
    // Regression guard: error handling must not swallow Spring Security's authentication entry
    // point. Not being signed in yet is not an error and must not start rendering as one.
    HttpResponse<String> res = browserGet("/admin", null);

    assertThat(res.statusCode()).isEqualTo(302);
    assertThat(res.headers().firstValue("Location")).get().asString().endsWith("/login");
    assertThat(res.body()).doesNotContain("You don't have access to that page");
  }

  // ---------------------------------------------------------------- JSON endpoints

  @Test
  void expiredSessionOnTheJsonEndpointFailsLoudlyAsJson() throws Exception {
    // The regression this fixes. It used to answer 302 -> /login; a browser fetch follows the
    // redirect, gets the sign-in page with status 200, `r.ok` passes, and r.json() dies on
    // "Unexpected token '<'" — a parse error three frames from the real problem, "you're signed
    // out". It must be an unambiguous 401 with a JSON body instead.
    HttpResponse<String> res = postStatusChange(anonymousSession());

    assertThat(res.statusCode()).isEqualTo(401);
    assertThat(res.headers().firstValue("Location")).isEmpty();
    assertThat(res.body())
        .startsWith("{")
        .contains("\"error\":\"Not signed in\"")
        .doesNotContain("<html");
  }

  @Test
  void moderatorDeniedOnTheAdminOnlyEndpointGetsJsonNotAPage() throws Exception {
    HttpResponse<String> res = postStatusChange(signIn("moderator", "moderator_secret"));

    assertThat(res.statusCode()).isEqualTo(403);
    assertThat(res.body())
        .startsWith("{")
        .contains("\"error\":\"Not permitted\"")
        .doesNotContain("<html");
  }

  @Test
  void jsonClientGetsJsonBackFromAnUncaughtException() throws Exception {
    HttpResponse<String> res =
        send(
            request("/boom", signIn("admin", "admin_secret").cookie())
                .header("Accept", "application/json"));

    assertThat(res.statusCode()).isEqualTo(500);
    assertThat(res.body()).startsWith("{").contains("\"path\":\"/boom\"").doesNotContain("<html");
  }

  // ---------------------------------------------------------------- helpers

  /** The navbar and footer come from fragments/layout.html — their presence is the whole point. */
  private void assertRendersInsideTheAppShell(String body) {
    assertThat(body).contains("navbar").contains("Service Station Hours").contains("Back to home");
  }

  private HttpRequest.Builder request(String path, String cookie) {
    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    if (cookie != null) {
      b.header("Cookie", cookie);
    }
    return b;
  }

  /**
   * GETs as a browser would. The Accept header matters: Boot's BasicErrorController negotiates, so
   * a client asking for JSON never reaches these templates at all.
   */
  private HttpResponse<String> browserGet(String path, Session session) throws Exception {
    return send(
        request(path, session == null ? null : session.cookie()).header("Accept", "text/html"));
  }

  private HttpResponse<String> send(HttpRequest.Builder builder) throws Exception {
    return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  /** POSTs to the ADMIN-only quick approve/reject endpoint the way the hours page JS does. */
  private HttpResponse<String> postStatusChange(Session session) throws Exception {
    return send(
        request("/admin/hours/1/status", session.cookie())
            .header("Accept", "application/json")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("X-CSRF-TOKEN", session.csrf())
            .POST(HttpRequest.BodyPublishers.ofString("status=APPROVED")));
  }

  /** A session carrying a valid CSRF token but no authenticated user. */
  private Session anonymousSession() throws Exception {
    HttpResponse<String> loginPage = send(request("/login", null).GET());
    return new Session(requireCookie(loginPage), match(CSRF_INPUT, loginPage.body(), "login form"));
  }

  /** Form-logs in and returns the authenticated session cookie plus a usable CSRF token. */
  private Session signIn(String username, String password) throws Exception {
    HttpResponse<String> loginPage = send(request("/login", null).GET());
    String cookie = requireCookie(loginPage);
    String csrf = match(CSRF_INPUT, loginPage.body(), "login form");

    HttpResponse<String> res =
        send(
            request("/login", cookie)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "username=" + username + "&password=" + password + "&_csrf=" + csrf)));
    assertThat(res.statusCode())
        .as("login as %s should redirect, not re-render the form", username)
        .isEqualTo(302);

    // Session fixation protection issues a new JSESSIONID on successful authentication, and
    // CsrfAuthenticationStrategy replaces the token — so both have to be re-read afterwards.
    // "/" role-routes with a redirect, so follow it to reach a page that actually renders. The
    // token is scraped from the layout's sign-out form, which Thymeleaf gives a hidden _csrf
    // input; every page has one, whereas the <meta> tag is empty for roles that never need it.
    String authenticated = requireCookie(res);
    HttpResponse<String> landing = followRedirects("/", authenticated);
    return new Session(authenticated, match(CSRF_INPUT, landing.body(), "sign-out form"));
  }

  /** Follows up to three redirects, so a role-routed landing page can be read. */
  private HttpResponse<String> followRedirects(String path, String cookie) throws Exception {
    HttpResponse<String> res = send(request(path, cookie).header("Accept", "text/html"));
    for (int hop = 0; hop < 3 && res.statusCode() / 100 == 3; hop++) {
      String location =
          res.headers()
              .firstValue("Location")
              .orElseThrow(() -> new AssertionError("redirect without a Location"));
      res = send(request(URI.create(location).getPath(), cookie).header("Accept", "text/html"));
    }
    assertThat(res.statusCode())
        .as("expected to land on a rendered page from %s", path)
        .isEqualTo(200);
    return res;
  }

  private String requireCookie(HttpResponse<String> res) {
    return res.headers()
        .firstValue("Set-Cookie")
        .map(header -> header.split(";", 2)[0])
        .orElseThrow(() -> new AssertionError("expected a session cookie"));
  }

  private String match(Pattern pattern, String html, String where) throws IOException {
    Matcher m = pattern.matcher(html == null ? "" : html);
    assertThat(m.find()).as("expected a CSRF token in the %s", where).isTrue();
    return m.group(1);
  }

  private record Session(String cookie, String csrf) {}
}
