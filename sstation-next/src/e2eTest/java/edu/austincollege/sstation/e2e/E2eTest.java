package edu.austincollege.sstation.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Shared setup for the TC-111 end-to-end suite.
 *
 * <p>These tests drive a real browser against an <em>already-running</em> application over HTTP —
 * by default the compose stack on {@code http://localhost:8080}, overridable with {@code
 * E2E_BASE_URL} to point at a deployed demo instead. Nothing here starts the app; that is the
 * caller's job (CI brings the stack up before invoking {@code e2eTest}).
 *
 * <p>The suite exists because the 158 JUnit tests all stop short of the browser: they exercise
 * services directly, or controllers through MockMvc, which never runs the real filter chain over a
 * real socket and never executes a line of the JavaScript the pages ship. Anything that only breaks
 * in a browser — a WebJar path that 404s, a Chart.js canvas that silently fails to draw, a CSRF
 * token the fetch does not send — is invisible to them by construction.
 *
 * <p>Every test gets a fresh {@link BrowserContext}, so each one starts with an empty cookie jar
 * and its own session. Tests therefore never depend on each other's sign-in state and can run in
 * any order.
 */
abstract class E2eTest {

  /** Where the app is. The compose stack publishes 8080; a deployed demo would override this. */
  protected static final String BASE_URL = env("E2E_BASE_URL", "http://localhost:8080");

  // Demo-profile credentials. The defaults match docker-compose.ci.yml so a local
  // `docker compose -f docker-compose.yml -f docker-compose.ci.yml up` needs no extra setup;
  // DemoAccountSeeder refuses to boot without these being set on the app side (TC-114).
  protected static final String ADMIN_USER = "admin";
  protected static final String ADMIN_PASSWORD =
      env("SSTATION_DEMO_ADMIN_PASSWORD", "ci-demo-admin-pw");
  protected static final String STUDENT_USER = "student";
  protected static final String STUDENT_PASSWORD =
      env("SSTATION_DEMO_STUDENT_PASSWORD", "ci-demo-student-pw");
  protected static final String MODERATOR_USER = "moderator";
  protected static final String MODERATOR_PASSWORD =
      env("SSTATION_DEMO_MODERATOR_PASSWORD", "ci-demo-moderator-pw");

  private static Playwright playwright;
  private static Browser browser;

  protected BrowserContext context;
  protected Page page;

  @BeforeAll
  static void launchBrowser() {
    playwright = Playwright.create();
    browser = playwright.chromium().launch();
  }

  @AfterAll
  static void closeBrowser() {
    if (browser != null) {
      browser.close();
    }
    if (playwright != null) {
      playwright.close();
    }
  }

  @BeforeEach
  void openContext() {
    context = browser.newContext();
    page = context.newPage();
  }

  @AfterEach
  void closeContext() {
    if (context != null) {
      context.close();
    }
  }

  /** Signs in through the real form and waits for the resulting navigation to settle. */
  protected void signIn(String username, String password) {
    page.navigate(BASE_URL + "/login");
    page.fill("input[name='username']", username);
    page.fill("input[name='password']", password);
    page.click("button[type='submit']");
    page.waitForLoadState();
  }

  protected void signInAsAdmin() {
    signIn(ADMIN_USER, ADMIN_PASSWORD);
  }

  protected void signInAsStudent() {
    signIn(STUDENT_USER, STUDENT_PASSWORD);
  }

  protected void signInAsModerator() {
    signIn(MODERATOR_USER, MODERATOR_PASSWORD);
  }

  /** Absolute URL for a path, so tests read as routes rather than string concatenation. */
  protected String url(String path) {
    return BASE_URL + path;
  }

  private static String env(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? fallback : value;
  }
}
