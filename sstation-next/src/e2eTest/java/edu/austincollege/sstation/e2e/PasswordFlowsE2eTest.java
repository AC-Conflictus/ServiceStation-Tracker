package edu.austincollege.sstation.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonParser;
import com.microsoft.playwright.options.RequestOptions;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Real browser forms plus SMTP captured by the test-only Mailpit service. */
class PasswordFlowsE2eTest extends E2eTest {
  private static final String MAILBOX =
      System.getenv().getOrDefault("E2E_MAILPIT_URL", "http://127.0.0.1:8025");

  @Test
  void emailedResetIsSingleUseAndChangedPasswordCanSignIn() {
    String email = "student@austincollege.edu";
    Set<String> before = messageIds(email);
    page.navigate(url("/forgot-password"));
    page.fill("[name=email]", email);
    submit();
    assertThat(page.textContent("main")).contains("If an account with that email exists");
    Set<String> arrived = messageIds(email);
    arrived.removeAll(before);
    assertThat(arrived).as("a new reset email must reach the isolated mailbox").hasSize(1);
    var response = context.request().get(MAILBOX + "/api/v1/message/" + arrived.iterator().next());
    assertThat(response.ok()).isTrue();
    String html =
        JsonParser.parseString(response.text()).getAsJsonObject().get("HTML").getAsString();
    var match = Pattern.compile("href=\"([^\"]*/reset-password\\?token=[^\"]+)\"").matcher(html);
    assertThat(match.find()).as("reset email contains an actual link").isTrue();
    String link = match.group(1).replace("&amp;", "&");
    assertThat(link).startsWith(BASE_URL + "/reset-password?token=");
    String temporary = "browser-reset-password-123";
    String changed = "browser-changed-password-456";
    String current = STUDENT_PASSWORD;
    try {
      page.navigate(link);
      page.fill("[name=password]", temporary);
      submit();
      assertThat(page.url()).endsWith("/login?reset");
      current = temporary;
      signIn(STUDENT_USER, STUDENT_PASSWORD);
      assertThat(page.url()).doesNotEndWith("/student");
      assertThat(page.textContent("main")).contains("Sign in failed");
      signIn(STUDENT_USER, current);
      assertThat(page.url()).endsWith("/student");
      page.navigate(link);
      page.fill("[name=password]", "reuse-must-not-work-789");
      submit();
      assertThat(page.textContent("main")).contains("invalid or has expired");
      page.navigate(url("/change-password"));
      page.fill("[name=currentPassword]", "wrong-current-password");
      page.fill("[name=newPassword]", changed);
      submit();
      assertThat(page.textContent("main")).contains("That isn't your current password");
      changePassword(current, changed);
      current = changed;
      assertThat(page.url()).endsWith("/login?changed");
      signIn(STUDENT_USER, current);
      assertThat(page.url()).endsWith("/student");
    } finally {
      // The shared demo login must keep working for later tests and repeat runs.
      if (!current.equals(STUDENT_PASSWORD)) {
        signIn(STUDENT_USER, current);
        changePassword(current, STUDENT_PASSWORD);
        assertThat(page.url()).endsWith("/login?changed");
      }
    }
    signInAsStudent();
    assertThat(page.url()).endsWith("/student");
  }

  private void changePassword(String current, String replacement) {
    page.navigate(url("/change-password"));
    page.fill("[name=currentPassword]", current);
    page.fill("[name=newPassword]", replacement);
    submit();
  }

  private Set<String> messageIds(String email) {
    var response =
        context
            .request()
            .get(
                MAILBOX + "/api/v1/search",
                RequestOptions.create().setQueryParam("query", "to:" + email));
    assertThat(response.ok()).as("Mailpit API reachable; see docker-compose.ci.yml").isTrue();
    Set<String> ids = new HashSet<>();
    for (var message :
        JsonParser.parseString(response.text()).getAsJsonObject().getAsJsonArray("messages")) {
      ids.add(message.getAsJsonObject().get("ID").getAsString());
    }
    return ids;
  }
}
