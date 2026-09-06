package edu.austincollege.sstation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.austincollege.sstation.repository.RoleRepository;
import edu.austincollege.sstation.repository.ServiceHourRepository;
import edu.austincollege.sstation.repository.StudentRepository;
import edu.austincollege.sstation.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * TC-118: bare {@code prod} against a virgin PostgreSQL — the literal AC IT day-one path.
 *
 * <p>{@link DemoProfileIntegrationTest} covers {@code prod,demo}, where the seeders create data. It
 * cannot cover this, because what is being asserted here is the state of a database nothing has
 * written to yet. Hence a second container rather than a shared one: the moment these two tests
 * share a Postgres instance, "virgin" stops being true.
 *
 * <p>What a green run proves: Flyway V1–V4 apply cleanly to an empty schema, Hibernate's {@code
 * ddl-auto=validate} agrees with the result (the context would refuse to start otherwise), and a
 * bare {@code prod} boot seeds nothing at all — so AC IT's first login is against an empty user
 * table, which is the documented and intended behaviour.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
class ProdProfileMigrationIntegrationTest {

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
  }

  @Autowired private Flyway flyway;
  @Autowired private UserRepository users;
  @Autowired private RoleRepository roles;
  @Autowired private StudentRepository students;
  @Autowired private ServiceHourRepository serviceHours;
  @Autowired private MockMvc mvc;

  @Test
  void flywayMigratesAVirginDatabaseCleanly() {
    MigrationInfo[] applied = flyway.info().applied();

    assertThat(applied).isNotEmpty();
    assertThat(applied)
        .allSatisfy(info -> assertThat(info.getState().isFailed()).as("%s failed", info).isFalse());
    // Nothing left over: every migration on the classpath ran against this empty database.
    assertThat(flyway.info().pending()).isEmpty();
  }

  @Test
  void bareProdSeedsNothing() {
    // DevDataSeeder is @Profile("dev") and DemoAccountSeeder/DemoDataSeeder are @Profile("demo"),
    // so none of them are active here. No migration inserts rows either.
    //
    // BootstrapAdminSeeder *is* active under bare prod (TC-121), but declines without
    // SSTATION_BOOTSTRAP_ADMIN_PASSWORD, which this test deliberately does not set — so this also
    // pins the "warn, don't create" branch. BootstrapAdminIntegrationTest covers the other side.
    // If this ever fails, a seeder has started creating accounts it shouldn't.
    assertThat(users.count()).isZero();
    assertThat(roles.count()).isZero();
    assertThat(students.count()).isZero();
    assertThat(serviceHours.count()).isZero();
  }

  @Test
  void loginPageServesButNoAccountCanAuthenticate() throws Exception {
    mvc.perform(get("/login")).andExpect(status().isOk());
    // The dev-profile credentials must not work on a bare prod boot.
    mvc.perform(formLogin("/login").user("admin").password("admin_secret"))
        .andExpect(unauthenticated());
  }
}
