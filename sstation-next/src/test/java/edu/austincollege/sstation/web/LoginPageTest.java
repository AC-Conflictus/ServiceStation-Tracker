package edu.austincollege.sstation.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * TC-124: the sign-in page follows AC Self-Service's flow, and its "forgot password" path depends
 * on who owns the password.
 *
 * <p>The directory-mode cases matter most. Pointing an AC user at our own reset flow would be worse
 * than useless: their account has no local password to reset, so they would get a reset email (or
 * silently not), and still be unable to sign in.
 */
class LoginPageTest {

  @Nested
  @SpringBootTest
  @AutoConfigureMockMvc
  class LocalMode {

    @Autowired private MockMvc mvc;

    @Test
    void usesSelfServiceLabelsAndOffersOurOwnReset() throws Exception {
      String page = render(mvc, "/login");

      assertThat(page).contains(">User name</label>", ">Password</label>");
      // Labels are bound to their inputs, so clicking one focuses the field and screen readers
      // announce it.
      assertThat(page).contains("for=\"username\"", "id=\"username\"", "for=\"password\"");
      assertThat(page).contains("Sign in with your Service Station account.");
      assertThat(page).contains("href=\"/forgot-password\"");
      assertThat(page).doesNotContain("Forgot your AC password");
    }

    @Test
    void aFailedSignInDoesNotSayWhichFieldWasWrong() throws Exception {
      String page = render(mvc, "/login?error");

      assertThat(page).contains("Sign in failed. Please check your user name and password.");
    }
  }

  @Nested
  @SpringBootTest
  @AutoConfigureMockMvc
  @TestPropertySource(
      properties = {
        "sstation.auth.mode=directory",
        "sstation.auth.password-help-url=https://help.example.edu/password"
      })
  class DirectoryModeWithHelpUrl {

    @Autowired private MockMvc mvc;

    @Test
    void asksForAcCredentialsAndSendsForgottenPasswordsToAcIt() throws Exception {
      String page = render(mvc, "/login");

      assertThat(page).contains("Use your Austin College user name and password.");
      assertThat(page).contains("href=\"https://help.example.edu/password\"");
      assertThat(page).contains("Forgot your AC password?");
      assertThat(page).doesNotContain("href=\"/forgot-password\"");
    }
  }

  @Nested
  @SpringBootTest
  @AutoConfigureMockMvc
  @TestPropertySource(properties = "sstation.auth.mode=directory")
  class DirectoryModeWithoutHelpUrl {

    @Autowired private MockMvc mvc;

    @Test
    void stillNeverOffersOurOwnResetFlow() throws Exception {
      String page = render(mvc, "/login");

      assertThat(page).contains("Forgot your AC password? Contact Austin College IT.");
      assertThat(page).doesNotContain("href=\"/forgot-password\"");
    }
  }

  private static String render(MockMvc mvc, String url) throws Exception {
    return mvc.perform(get(url))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }
}
