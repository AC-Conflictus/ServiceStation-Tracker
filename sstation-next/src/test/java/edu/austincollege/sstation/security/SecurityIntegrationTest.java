package edu.austincollege.sstation.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end auth coverage for TC-104. Runs under the {@code dev} profile so the seeded
 * admin/student/moderator accounts exist, exercising the real BCrypt verification path.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SecurityIntegrationTest {

  @Autowired private MockMvc mvc;

  @Test
  void anonymousIsRedirectedToLogin() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));
  }

  @Test
  void loginPageIsPublic() throws Exception {
    mvc.perform(get("/login")).andExpect(status().isOk());
  }

  @Test
  void seededAdminCanLogInWithBcryptVerifiedPassword() throws Exception {
    mvc.perform(formLogin("/login").user("admin").password("admin_secret"))
        .andExpect(authenticated().withUsername("admin").withRoles("ADMIN"))
        .andExpect(redirectedUrl("/"));
  }

  @Test
  void wrongPasswordIsRejected() throws Exception {
    mvc.perform(formLogin("/login").user("admin").password("nope"))
        .andExpect(unauthenticated())
        .andExpect(redirectedUrl("/login?error"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminRoleReachesAdminHome() throws Exception {
    mvc.perform(get("/admin")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void studentRoleIsForbiddenFromAdminHome() throws Exception {
    // Proves the @PreAuthorize("hasRole('ADMIN')") gate (TC-019).
    mvc.perform(get("/admin")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void postWithoutCsrfTokenIsForbidden() throws Exception {
    // CSRF protection is on (TC-018): a POST with no token is rejected.
    mvc.perform(post("/logout")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void postWithCsrfTokenSucceeds() throws Exception {
    mvc.perform(post("/logout").with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?logout"));
  }
}
