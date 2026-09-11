package edu.austincollege.sstation.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TC-111: sign-in, sign-out and role routing for all three roles.
 *
 * <p>This is the flow every other flow depends on, and the one the Grails app's own Selenium
 * scripts covered, so it goes first.
 */
@DisplayName("E2E: authentication and role routing")
class AuthAndRoutingE2eTest extends E2eTest {

  @Test
  @DisplayName("an anonymous visitor is sent to the sign-in page")
  void anonymousVisitorIsSentToSignIn() {
    page.navigate(url("/admin"));

    assertThat(page.url()).contains("/login");
    assertThat(page.locator("input[name='username']").isVisible()).isTrue();
  }

  @Test
  @DisplayName("admin signs in and lands on the KPI dashboard")
  void adminLandsOnDashboard() {
    signInAsAdmin();

    // HomeController redirects ROLE_ADMIN from / to /admin.
    assertThat(page.url()).endsWith("/admin");
    assertThat(page.textContent("body")).contains("Signed in as");
  }

  @Test
  @DisplayName("student signs in and lands on their own dashboard")
  void studentLandsOnStudentDashboard() {
    signInAsStudent();

    assertThat(page.url()).endsWith("/student");
  }

  @Test
  @DisplayName("moderator signs in and lands on the shared landing page, not a dashboard")
  void moderatorLandsOnLandingPage() {
    signInAsModerator();

    // Moderators match neither redirect branch in HomeController, so they stay on /.
    assertThat(page.url()).doesNotContain("/login");
    assertThat(page.url()).doesNotEndWith("/admin");
    assertThat(page.url()).doesNotEndWith("/student");
  }

  @Test
  @DisplayName("a wrong password is rejected")
  void wrongPasswordIsRejected() {
    signIn(ADMIN_USER, "definitely-not-the-password");

    assertThat(page.url()).contains("/login");
    assertThat(page.url()).contains("error");
  }

  @Test
  @DisplayName("the dev-profile password never works on a demo host")
  void devPasswordIsRejected() {
    // TC-114's guarantee, asserted in a browser as well as in CI's smoke script: admin_secret is
    // a dev-only fallback and must never authenticate anywhere the demo seeder is active.
    signIn(ADMIN_USER, "admin_secret");

    assertThat(page.url()).contains("/login");
    assertThat(page.url()).contains("error");
  }

  @Test
  @DisplayName("a student cannot reach an admin page")
  void studentCannotReachAdminPages() {
    signInAsStudent();

    page.navigate(url("/admin"));

    // TC-120 renders a real 403 page inside the app shell rather than a Whitelabel error.
    assertThat(page.textContent("body")).contains("You don't have access to that page");
  }

  @Test
  @DisplayName("signing out ends the session")
  void signOutEndsTheSession() {
    signInAsAdmin();

    page.click("form[action$='/logout'] button[type='submit']");
    page.waitForLoadState();

    assertThat(page.url()).contains("/login");

    // The session is really gone, not just the page: a protected URL bounces again.
    page.navigate(url("/admin"));
    assertThat(page.url()).contains("/login");
  }
}
