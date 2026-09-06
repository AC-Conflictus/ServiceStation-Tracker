package edu.austincollege.sstation.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.austincollege.sstation.domain.Role;
import edu.austincollege.sstation.domain.User;
import edu.austincollege.sstation.domain.UserRole;
import edu.austincollege.sstation.repository.RoleRepository;
import edu.austincollege.sstation.repository.UserRepository;
import edu.austincollege.sstation.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** TC-121: a flagged account is confined to /change-password until it sets its own password. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class MustChangePasswordFilterTest {

  private static final String USERNAME = "bootstrap-admin";
  private static final String BOOTSTRAP_PASSWORD = "from_the_environment";

  @Autowired private MockMvc mvc;
  @Autowired private UserRepository users;
  @Autowired private RoleRepository roles;
  @Autowired private UserRoleRepository userRoles;
  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  void createFlaggedAccount() {
    // user_roles has no cascade on its FK to users, so the join rows go first.
    users
        .findByUsername(USERNAME)
        .ifPresent(
            existing -> {
              userRoles.deleteAll(
                  userRoles.findAll().stream()
                      .filter(ur -> ur.getUser().getId().equals(existing.getId()))
                      .toList());
              users.delete(existing);
            });

    User user = new User(USERNAME, passwordEncoder.encode(BOOTSTRAP_PASSWORD));
    user.setMustChangePassword(true);
    user = users.save(user);

    Role admin =
        roles.findByAuthority("ROLE_ADMIN").orElseGet(() -> roles.save(new Role("ROLE_ADMIN")));
    userRoles.save(new UserRole(user, admin));
  }

  @Test
  void flaggedAccountIsRedirectedAwayFromEveryOtherPage() throws Exception {
    MockHttpSession session = signIn(BOOTSTRAP_PASSWORD);

    mvc.perform(get("/").session(session)).andExpect(redirectedUrl("/change-password"));
    mvc.perform(get("/admin").session(session)).andExpect(redirectedUrl("/change-password"));
    mvc.perform(get("/reports").session(session)).andExpect(redirectedUrl("/change-password"));
  }

  @Test
  void theChangePasswordPageItselfStaysReachable() throws Exception {
    // Redirecting this one would be an infinite loop rather than a security control.
    mvc.perform(get("/change-password").session(signIn(BOOTSTRAP_PASSWORD)))
        .andExpect(status().isOk());
  }

  @Test
  void signOutStaysAvailableSoTheAccountIsNotTrapped() throws Exception {
    mvc.perform(post("/logout").with(csrf()).session(signIn(BOOTSTRAP_PASSWORD)))
        .andExpect(redirectedUrl("/login?logout"));
  }

  @Test
  void changingThePasswordClearsTheFlagAndEndsTheSession() throws Exception {
    MockHttpSession session = signIn(BOOTSTRAP_PASSWORD);

    mvc.perform(
            post("/change-password")
                .with(csrf())
                .session(session)
                .param("currentPassword", BOOTSTRAP_PASSWORD)
                .param("newPassword", "a_password_of_my_own"))
        .andExpect(redirectedUrl("/login?changed"));

    assertThat(users.findByUsername(USERNAME).orElseThrow().isMustChangePassword()).isFalse();

    // Signing in again with the new password now reaches the app instead of bouncing. Checked on
    // /admin rather than /, because / legitimately redirects an admin to the dashboard.
    mvc.perform(get("/admin").session(signIn("a_password_of_my_own"))).andExpect(status().isOk());
  }

  @Test
  void aFailedChangeLeavesTheAccountFlagged() throws Exception {
    MockHttpSession session = signIn(BOOTSTRAP_PASSWORD);

    mvc.perform(
            post("/change-password")
                .with(csrf())
                .session(session)
                .param("currentPassword", "wrong")
                .param("newPassword", "a_password_of_my_own"))
        .andExpect(redirectedUrl("/change-password"));

    assertThat(users.findByUsername(USERNAME).orElseThrow().isMustChangePassword()).isTrue();
  }

  private MockHttpSession signIn(String password) throws Exception {
    MvcResult result =
        mvc.perform(formLogin("/login").user(USERNAME).password(password))
            .andExpect(authenticated())
            .andReturn();
    return (MockHttpSession) result.getRequest().getSession(false);
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor csrf() {
    return org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.csrf();
  }
}
