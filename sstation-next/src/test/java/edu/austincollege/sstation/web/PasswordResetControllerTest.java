package edu.austincollege.sstation.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.austincollege.sstation.domain.PasswordResetToken;
import edu.austincollege.sstation.domain.User;
import edu.austincollege.sstation.repository.PasswordResetTokenRepository;
import edu.austincollege.sstation.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer coverage for the password-reset flow (TC-108g). Dev profile = seeded `student` user.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PasswordResetControllerTest {

  @Autowired private MockMvc mvc;
  @Autowired private UserRepository users;
  @Autowired private PasswordResetTokenRepository tokens;
  @Autowired private PasswordEncoder encoder;

  @Test
  void resetPagesArePublic() throws Exception {
    mvc.perform(get("/forgot-password")).andExpect(status().isOk());
    mvc.perform(get("/reset-password").param("token", "x")).andExpect(status().isOk());
  }

  @Test
  void forgotPasswordIssuesATokenForAKnownEmail() throws Exception {
    long before = tokens.count();
    mvc.perform(post("/forgot-password").with(csrf()).param("email", "student@austincollege.edu"))
        .andExpect(redirectedUrl("/forgot-password"));
    assertThat(tokens.count()).isEqualTo(before + 1);
  }

  @Test
  void validTokenResetsPasswordThenRedirectsToLogin() throws Exception {
    User student = users.findByUsername("student").orElseThrow();
    String raw = "integration-raw-token";
    tokens.save(new PasswordResetToken(student, sha256(raw), LocalDateTime.now().plusHours(1)));

    mvc.perform(
            post("/reset-password")
                .with(csrf())
                .param("token", raw)
                .param("password", "brand_new_secret"))
        .andExpect(redirectedUrl("/login?reset"));

    User reloaded = users.findById(student.getId()).orElseThrow();
    assertThat(encoder.matches("brand_new_secret", reloaded.getPassword())).isTrue();
  }

  @Test
  void invalidTokenRedirectsBackToResetForm() throws Exception {
    mvc.perform(
            post("/reset-password")
                .with(csrf())
                .param("token", "bogus")
                .param("password", "x123456"))
        .andExpect(redirectedUrlPattern("/reset-password*"));
  }

  private static String sha256(String value) {
    try {
      byte[] d =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(d);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
