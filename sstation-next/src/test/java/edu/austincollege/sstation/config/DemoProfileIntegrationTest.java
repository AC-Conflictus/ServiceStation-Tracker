package edu.austincollege.sstation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;

import edu.austincollege.sstation.repository.ServiceHourRepository;
import edu.austincollege.sstation.repository.UserRepository;
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

/** TC-114: prod + demo against real Postgres seeds accounts and hours; auth uses demo passwords. */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"prod", "demo"})
class DemoProfileIntegrationTest {

  private static final String DEMO_ADMIN = "demo-admin-pass";
  private static final String DEMO_STUDENT = "demo-student-pass";
  private static final String DEMO_MODERATOR = "demo-moderator-pass";

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
    registry.add("SSTATION_DEMO_ADMIN_PASSWORD", () -> DEMO_ADMIN);
    registry.add("SSTATION_DEMO_STUDENT_PASSWORD", () -> DEMO_STUDENT);
    registry.add("SSTATION_DEMO_MODERATOR_PASSWORD", () -> DEMO_MODERATOR);
    registry.add("SSTATION_MAIL_HOST", () -> "");
  }

  @Autowired private UserRepository users;
  @Autowired private ServiceHourRepository serviceHours;
  @Autowired private MockMvc mvc;

  @Test
  void seedsUsersAndHours() {
    assertThat(users.findByUsername("admin")).isPresent();
    assertThat(users.findByUsername("student")).isPresent();
    assertThat(users.findByUsername("moderator")).isPresent();
    assertThat(serviceHours.count()).isGreaterThan(0);
  }

  @Test
  void demoAdminCanLogIn() throws Exception {
    mvc.perform(formLogin("/login").user("admin").password(DEMO_ADMIN))
        .andExpect(authenticated().withUsername("admin").withRoles("ADMIN"));
  }
}
