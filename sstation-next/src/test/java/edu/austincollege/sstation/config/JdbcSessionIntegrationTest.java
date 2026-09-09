package edu.austincollege.sstation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * TC-122a: a signed-in session lands in PostgreSQL, not in instance memory.
 *
 * <p>The reason the card exists: Vercel's Fluid compute may serve any request from any instance, so
 * an in-memory session signs the user out at random. The local stand-in for "another instance picks
 * up the request" is exactly what a second instance would do — load the session by id from the
 * shared database. This test does that literally: sign in through the real filter chain, then
 * reload the row through {@link SessionRepository#findById} and assert the deserialized security
 * context still says the admin is authenticated.
 *
 * <p>Runs bare {@code prod} with the TC-121 bootstrap credentials, like {@link
 * BootstrapAdminIntegrationTest}, and against its own container for the same reason: the bootstrap
 * seeder is gated on an empty users table, and a shared container would already have accounts.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class JdbcSessionIntegrationTest {

  /** Spring Security's session attribute under {@code HttpSessionSecurityContextRepository}. */
  private static final String SECURITY_CONTEXT = "SPRING_SECURITY_CONTEXT";

  private static final String BOOTSTRAP_PASSWORD = "sessions_bootstrap_password";

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("sstation")
          .withUsername("sstation")
          .withPassword("sstation_test");

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("SSTATION_DB_URL", POSTGRES::getJdbcUrl);
    registry.add("SSTATION_DB_USER", POSTGRES::getUsername);
    registry.add("SSTATION_DB_PASSWORD", POSTGRES::getPassword);
    registry.add("SSTATION_MAIL_HOST", () -> "");
    registry.add("SSTATION_BOOTSTRAP_ADMIN_USERNAME", () -> "ac-it-admin");
    registry.add("SSTATION_BOOTSTRAP_ADMIN_PASSWORD", () -> BOOTSTRAP_PASSWORD);
  }

  @Autowired private JdbcTemplate jdbc;
  @Autowired private SessionRepository<? extends Session> sessions;
  @Autowired private MockMvc mvc;

  @Test
  void signInWritesTheSessionToPostgres() throws Exception {
    long before = sessionRows();

    mvc.perform(formLogin("/login").user("ac-it-admin").password(BOOTSTRAP_PASSWORD))
        .andExpect(authenticated().withUsername("ac-it-admin").withRoles("ADMIN"));

    // The row exists in Postgres — session state has left the JVM. Principal name and expiry
    // are populated by the repository on save; expiry is a BIGINT epoch-millis.
    assertThat(sessionRows()).isGreaterThan(before);
    Long expiry =
        jdbc.queryForObject(
            "SELECT MAX(EXPIRY_TIME) FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ?",
            Long.class,
            "ac-it-admin");
    assertThat(expiry).isNotNull().isGreaterThan(System.currentTimeMillis());

    // And the security context is in the attributes table, not just the row shell.
    Long contextRows =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM SPRING_SESSION_ATTRIBUTES WHERE ATTRIBUTE_NAME = ?",
            Long.class,
            SECURITY_CONTEXT);
    assertThat(contextRows).isPositive();
  }

  @Test
  void aColdLoadOfThatSessionIsStillSignedIn() throws Exception {
    mvc.perform(formLogin("/login").user("ac-it-admin").password(BOOTSTRAP_PASSWORD))
        .andExpect(authenticated());

    // The current session id lives in the SESSION_ID column (it changes on login — session
    // fixation protection). Reading it from the DB and loading through the repository is the
    // same path a different Vercel instance would take.
    String sessionId =
        jdbc.queryForObject(
            "SELECT SESSION_ID FROM SPRING_SESSION WHERE PRINCIPAL_NAME = ?"
                + " ORDER BY EXPIRY_TIME DESC LIMIT 1",
            String.class,
            "ac-it-admin");
    assertThat(sessionId).isNotBlank();

    Session reloaded = sessions.findById(sessionId);
    assertThat(reloaded).isNotNull();
    assertThat(reloaded.getAttributeNames()).contains(SECURITY_CONTEXT);
    assertThat(reloaded.<SecurityContext>getAttribute(SECURITY_CONTEXT))
        .isNotNull()
        .extracting(SecurityContext::getAuthentication)
        .isNotNull()
        .extracting(Authentication::getName)
        .isEqualTo("ac-it-admin");
  }

  private long sessionRows() {
    Long count = jdbc.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Long.class);
    return count == null ? 0 : count;
  }
}
