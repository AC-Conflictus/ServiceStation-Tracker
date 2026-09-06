package edu.austincollege.sstation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import edu.austincollege.sstation.domain.User;
import edu.austincollege.sstation.repository.UserRepository;
import edu.austincollege.sstation.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * TC-121: the literal AC IT day-one path, end to end on real PostgreSQL.
 *
 * <p>Bare {@code prod} against a virgin database with the bootstrap env vars set — boot, sign in
 * with the credential from the environment, and be sent straight to {@code /change-password}. This
 * is the one flow that was impossible before this card: {@link ProdProfileMigrationIntegrationTest}
 * proves the same boot without the env vars leaves an empty user table, which is where AC IT would
 * have been stranded.
 *
 * <p>Its own container, like the other two Postgres-backed tests — a shared one would already have
 * accounts, and the seeder is gated on the table being empty.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class BootstrapAdminIntegrationTest {

  private static final String BOOTSTRAP_PASSWORD = "day_one_bootstrap_password";

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

  @Autowired private UserRepository users;
  @Autowired private UserRoleRepository userRoles;
  @Autowired private BootstrapAdminSeeder seeder;
  @Autowired private MockMvc mvc;

  @Test
  void bootCreatesExactlyOneAdministrator() {
    assertThat(users.count()).isEqualTo(1);

    User admin = users.findByUsername("ac-it-admin").orElseThrow();
    assertThat(userRoles.findAuthoritiesByUser(admin)).containsExactly("ROLE_ADMIN");
    // Also the only place must_change_password is exercised on Postgres rather than H2: the
    // seeder wrote it and this reads it back, and ddl-auto=validate would have refused to start
    // the context above if V5 and the entity disagreed.
    assertThat(admin.isMustChangePassword()).isTrue();
  }

  @Test
  void thatAdministratorCanActuallySignIn() throws Exception {
    // The whole point of the card. Before it, this login had no account to match.
    mvc.perform(formLogin("/login").user("ac-it-admin").password(BOOTSTRAP_PASSWORD))
        .andExpect(authenticated().withUsername("ac-it-admin").withRoles("ADMIN"));
  }

  @Test
  void andIsSentStraightToChangeItsPassword() throws Exception {
    MvcResult login =
        mvc.perform(formLogin("/login").user("ac-it-admin").password(BOOTSTRAP_PASSWORD))
            .andExpect(authenticated())
            .andReturn();
    MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

    mvc.perform(get("/admin").session(session)).andExpect(redirectedUrl("/change-password"));
  }

  @Test
  void restartingDoesNotCreateASecondAdministrator() {
    // Simulates the next container start with the env vars still in place.
    seeder.run();

    assertThat(users.count()).isEqualTo(1);
  }
}
