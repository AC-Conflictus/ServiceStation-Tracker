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
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.filter.ForwardedHeaderFilter;
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
 * <p>What a green run proves: Flyway V1–V6 apply cleanly to an empty schema, Hibernate's {@code
 * ddl-auto=validate} agrees with the result (the context would refuse to start otherwise), a bare
 * {@code prod} boot seeds nothing at all — so AC IT's first login is against an empty user table,
 * which is the documented and intended behaviour — and the proxy-header handling AC IT's deployment
 * depends on is actually switched on (TC-110b).
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

  @Autowired private ApplicationContext context;
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
  void prodRegistersTheForwardedHeaderFilter() {
    // TC-110b, pinned here rather than only in PasswordResetControllerTest. That test supplies
    // `server.forward-headers-strategy` itself via @SpringBootTest(properties = …), so it proves
    // the filter behaves — not that the prod profile turns it on. Deleting the property from
    // application.yml left the entire suite green, which is the failure this asserts against.
    //
    // Checks the effect rather than the property string: under `framework` Boot registers
    // Spring's ForwardedHeaderFilter, and that bean disappearing is what would silently put
    // http:// links back into password-reset email behind AC IT's TLS-terminating proxy.
    // Matched by wrapped filter type rather than bean name: Boot registers it as a
    // FilterRegistrationBean (named `forwardedHeaderFilter` today), and pinning the name would
    // make this a false alarm the day Boot renames it.
    assertThat(context.getBeansOfType(FilterRegistrationBean.class).values())
        .as("prod must register ForwardedHeaderFilter — see server.forward-headers-strategy")
        .anyMatch(registration -> registration.getFilter() instanceof ForwardedHeaderFilter);
  }

  @Test
  void loginPageServesButNoAccountCanAuthenticate() throws Exception {
    mvc.perform(get("/login")).andExpect(status().isOk());
    // The dev-profile credentials must not work on a bare prod boot.
    mvc.perform(formLogin("/login").user("admin").password("admin_secret"))
        .andExpect(unauthenticated());
  }
}
