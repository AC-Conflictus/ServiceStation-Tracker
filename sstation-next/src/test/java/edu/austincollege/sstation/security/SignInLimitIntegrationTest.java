package edu.austincollege.sstation.security;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * TC-125 through the real filter chain. No CAPTCHA is configured here, so the third failure blocks.
 *
 * <p>Its own context, thrown away afterwards: it deliberately blocks a seeded account, and the
 * counters are a singleton that a cached context would share with every other test class.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DirtiesContext
class SignInLimitIntegrationTest {

  @Autowired private MockMvc mvc;

  @Test
  void theThirdWrongPasswordSaysSoAndEvenTheRightOneIsThenRefused() throws Exception {
    for (int i = 0; i < 2; i++) {
      mvc.perform(formLogin("/login").user("moderator").password("nope"))
          .andExpect(redirectedUrl("/login?error"));
    }
    mvc.perform(formLogin("/login").user("moderator").password("nope"))
        .andExpect(redirectedUrl("/login?blocked"));

    mvc.perform(formLogin("/login").user("Moderator").password("moderator_secret"))
        .andExpect(unauthenticated())
        .andExpect(redirectedUrl("/login?blocked"));
  }

  @Test
  void otherAccountsAreUnaffected() throws Exception {
    for (int i = 0; i < 3; i++) {
      mvc.perform(formLogin("/login").user("nobody-at-all").password("nope"));
    }

    mvc.perform(formLogin("/login").user("admin").password("admin_secret"))
        .andExpect(authenticated().withUsername("admin"));
  }

  @Test
  void theSignInPageExplainsTheBlockAndHowLongItLasts() throws Exception {
    mvc.perform(get("/login").param("blocked", ""))
        .andExpect(content().string(containsString("Too many sign-in attempts")))
        .andExpect(content().string(containsString("15</span> minutes")));
  }
}
