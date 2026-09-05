package edu.austincollege.sstation.web;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TC-120: the custom error pages actually render, inside the application shell.
 *
 * <p>Deliberately a real-server test rather than {@code MockMvc}. MockMvc stops at the status code
 * and does not perform the ERROR dispatch, so it can prove a request was rejected but not what the
 * user ends up looking at — which is the entire point of this card. Only a servlet container
 * forwards to {@code /error}, resolves {@code templates/error/<status>.html} and renders it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class ErrorPageIntegrationTest {

  /** Marker text from Spring Boot's default error page — must never appear in a response. */
  private static final String WHITELABEL = "Whitelabel Error Page";

  private static final Pattern CSRF =
      Pattern.compile(
          "name=\"_csrf\"[^>]*value=\"([^\"]+)\"|value=\"([^\"]+)\"[^>]*name=\"_csrf\"");

  @Autowired private TestRestTemplate rest;

  private ListAppender<ILoggingEvent> adviceLogs;

  /**
   * A route that always fails, so the 500 path can be exercised without breaking a real one. The
   * nested controller needs no {@code @Bean} method — member classes of a configuration class are
   * themselves registered, and declaring both gives an ambiguous mapping.
   */
  @TestConfiguration
  static class BoomConfig {
    @RestController
    static class BoomController {
      @GetMapping("/boom")
      String boom() {
        throw new IllegalStateException("deliberate failure from ErrorPageIntegrationTest");
      }
    }
  }

  @BeforeEach
  void captureAdviceLogs() {
    adviceLogs = new ListAppender<>();
    adviceLogs.start();
    ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(GlobalErrorAdvice.class))
        .addAppender(adviceLogs);
  }

  @AfterEach
  void releaseAdviceLogs() {
    ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(GlobalErrorAdvice.class))
        .detachAppender(adviceLogs);
  }

  @Test
  void studentOnAnAdminUrlSeesTheCustom403() {
    ResponseEntity<String> res = get("/admin", signIn("student", "student_secret"));

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(res.getBody())
        .doesNotContain(WHITELABEL)
        .contains("You don't have access to that page")
        // Reads as a permissions message, and names the account so the fix is obvious.
        .contains("signed in as")
        .contains("student");
    assertRendersInsideTheAppShell(res.getBody());
  }

  @Test
  void unknownPathRendersTheCustom404() {
    ResponseEntity<String> res = get("/no-such-page", signIn("admin", "admin_secret"));

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(res.getBody())
        .doesNotContain(WHITELABEL)
        .contains("That page doesn't exist")
        .contains("/no-such-page");
    assertRendersInsideTheAppShell(res.getBody());
  }

  @Test
  void statusWithoutItsOwnPageFallsBackToTheCatchAll() {
    // GET on the POST-only quick-approve endpoint -> 405, which has no error/405.html. It must
    // land on templates/error.html rather than falling through to Whitelabel.
    ResponseEntity<String> res = get("/admin/hours/1/status", signIn("admin", "admin_secret"));

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(res.getBody())
        .doesNotContain(WHITELABEL)
        .contains(">405<")
        .contains("could not be completed");
    assertRendersInsideTheAppShell(res.getBody());
  }

  @Test
  void errorPagesDoNotLeakInternals() {
    ResponseEntity<String> res = get("/no-such-page", signIn("admin", "admin_secret"));

    // server.error.include-message / include-stacktrace stay at their "never" defaults.
    assertThat(res.getBody())
        .doesNotContain("java.lang.")
        .doesNotContain("org.springframework.")
        .doesNotContain("Exception");
  }

  @Test
  void anonymousUserLandsOnSignInRatherThanAnErrorPage() {
    // Regression guard: error handling must not swallow Spring Security's authentication entry
    // point. Not being signed in yet is not an error, and must not start rendering as one.
    ResponseEntity<String> res = get("/admin", new HttpHeaders());

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(res.getBody())
        .contains("Sign in")
        .contains("type=\"password\"")
        .doesNotContain("You don't have access to that page");
  }

  @Test
  void uncaughtExceptionRendersTheCustom500() {
    ResponseEntity<String> res = get("/boom", signIn("admin", "admin_secret"));

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(res.getBody())
        .doesNotContain(WHITELABEL)
        .contains("Something went wrong on our end")
        .contains("/boom")
        // The page tells the user to report the address, not to read a stack trace.
        .doesNotContain("IllegalStateException")
        .doesNotContain("deliberate failure");
    assertRendersInsideTheAppShell(res.getBody());
  }

  @Test
  void uncaughtExceptionIsLoggedAtErrorWithTheRequestPath() {
    get("/boom", signIn("admin", "admin_secret"));

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
  void jsonClientGetsJsonBackNotAnHtmlErrorPage() {
    HttpHeaders headers = signIn("admin", "admin_secret");
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    ResponseEntity<String> res =
        rest.exchange("/boom", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(res.getBody())
        .startsWith("{")
        .contains("\"path\":\"/boom\"")
        .doesNotContain("<html");
  }

  /** The navbar and footer come from fragments/layout.html — their presence is the whole point. */
  private void assertRendersInsideTheAppShell(String body) {
    assertThat(body).contains("navbar").contains("Service Station Hours").contains("Back to home");
  }

  /**
   * Requests {@code path} as a browser would. The Accept header matters: Boot's
   * BasicErrorController negotiates, so a client asking for JSON gets a JSON error body and never
   * reaches these templates at all. Asking for HTML is what exercises the pages this card adds.
   */
  private ResponseEntity<String> get(String path, HttpHeaders headers) {
    HttpHeaders browser = new HttpHeaders();
    browser.putAll(headers);
    browser.setAccept(List.of(MediaType.TEXT_HTML));
    return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(browser), String.class);
  }

  /** Form-logs in and returns headers carrying the authenticated session cookie. */
  private HttpHeaders signIn(String username, String password) {
    ResponseEntity<String> loginPage = rest.getForEntity("/login", String.class);
    String cookie = requireCookie(loginPage);

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.COOKIE, cookie);
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("username", username);
    form.add("password", password);
    form.add("_csrf", csrfToken(loginPage.getBody()));

    ResponseEntity<String> res =
        rest.exchange("/login", HttpMethod.POST, new HttpEntity<>(form, headers), String.class);
    assertThat(res.getStatusCode())
        .as("login as %s should redirect, not re-render the form", username)
        .isEqualTo(HttpStatus.FOUND);

    // Session fixation protection issues a new JSESSIONID on successful authentication.
    HttpHeaders authenticated = new HttpHeaders();
    authenticated.add(HttpHeaders.COOKIE, requireCookie(res));
    return authenticated;
  }

  private String requireCookie(ResponseEntity<String> res) {
    List<String> cookies = res.getHeaders().get(HttpHeaders.SET_COOKIE);
    assertThat(cookies).as("expected a session cookie").isNotEmpty();
    return cookies.get(0).split(";", 2)[0];
  }

  private String csrfToken(String html) {
    Matcher m = CSRF.matcher(html == null ? "" : html);
    assertThat(m.find()).as("login page should carry a CSRF token").isTrue();
    return m.group(1) != null ? m.group(1) : m.group(2);
  }
}
