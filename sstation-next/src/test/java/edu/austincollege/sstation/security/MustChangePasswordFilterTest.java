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
    jakarta.servlet.http.Cookie session = signIn(BOOTSTRAP_PASSWORD);

    mvc.perform(get("/").cookie(session)).andExpect(redirectedUrl("/change-password"));
    mvc.perform(get("/admin").cookie(session)).andExpect(redirectedUrl("/change-password"));
    mvc.perform(get("/reports").cookie(session)).andExpect(redirectedUrl("/change-password"));
  }

  @Test
  void theChangePasswordPageItselfStaysReachable() throws Exception {
    // Redirecting this one would be an infinite loop rather than a security control.
    mvc.perform(get("/change-password").cookie(signIn(BOOTSTRAP_PASSWORD)))
        .andExpect(status().isOk());
  }

  @Test
  void signOutStaysAvailableSoTheAccountIsNotTrapped() throws Exception {
    mvc.perform(post("/logout").with(csrf()).cookie(signIn(BOOTSTRAP_PASSWORD)))
        .andExpect(redirectedUrl("/login?logout"));
  }

  @Test
  void changingThePasswordClearsTheFlagAndEndsTheSession() throws Exception {
    jakarta.servlet.http.Cookie session = signIn(BOOTSTRAP_PASSWORD);

    mvc.perform(
            post("/change-password")
                .with(csrf())
                .cookie(session)
                .param("currentPassword", BOOTSTRAP_PASSWORD)
                .param("newPassword", "a_password_of_my_own"))
        .andExpect(redirectedUrl("/login?changed"));

    assertThat(users.findByUsername(USERNAME).orElseThrow().isMustChangePassword()).isFalse();

    // Signing in again with the new password now reaches the app instead of bouncing. Checked on
    // /admin rather than /, because / legitimately redirects an admin to the dashboard.
    mvc.perform(get("/admin").cookie(signIn("a_password_of_my_own"))).andExpect(status().isOk());
  }

  @Test
  void aFailedChangeLeavesTheAccountFlagged() throws Exception {
    jakarta.servlet.http.Cookie session = signIn(BOOTSTRAP_PASSWORD);

    mvc.perform(
            post("/change-password")
                .with(csrf())
                .cookie(session)
                .param("currentPassword", "wrong")
                .param("newPassword", "a_password_of_my_own"))
        .andExpect(redirectedUrl("/change-password"));

    assertThat(users.findByUsername(USERNAME).orElseThrow().isMustChangePassword()).isTrue();
  }

  /**
   * Signs in and returns the {@code SESSION} cookie a real browser would hold afterwards.
   *
   * <p>Sessions live in the database now (TC-122a), so there is no container session object to hand
   * between requests — {@code request.getSession(false)} on the raw mock comes back null because
   * the session filter owns session access. The browser flow is the correct one anyway: take the
   * {@code SESSION} cookie (spring-session's default name) the sign-in response sets and send it
   * back. It carries the *new* id Spring Session assigned at login (session-fixation protection),
   * which is exactly what makes the next request land in the signed-in session.
   *
   * <p>Note {@code .cookie(…)}, not {@code .header("Cookie", …)}: the session id resolver reads
   * {@code request.getCookies()}, which the mock only populates through the cookie builder — a
   * literal Cookie header is ignored. (Verified the hard way in this very test.)
   */
  private jakarta.servlet.http.Cookie signIn(String password) throws Exception {
    MvcResult result =
        mvc.perform(formLogin("/login").user(USERNAME).password(password))
            .andExpect(authenticated())
            .andReturn();
    return result.getResponse().getHeaders("Set-Cookie").stream()
        .filter(c -> c.startsWith("SESSION="))
        .map(
            c ->
                new jakarta.servlet.http.Cookie(
                    "SESSION", c.split(";", 2)[0].substring("SESSION=".length())))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no SESSION cookie on the sign-in response"));
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor csrf() {
    return org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.csrf();
  }
}
