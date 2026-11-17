package edu.austincollege.sstation.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Proves the site migration upgrades a populated pre-site database without losing events. */
@Testcontainers(disabledWithoutDocker = true)
class EventSiteMigrationIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

  @Test
  void upgradesV8AndCascadesOnlySiteSetupWhenEventIsDeleted() throws Exception {
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .target("8")
        .load()
        .migrate();
    try (var connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var sql = connection.createStatement()) {
      sql.executeUpdate(
          "INSERT INTO events(name,description,contact,contact_phone,contact_email) VALUES ('Existing event','Existing description','Office','9030000000','office@example.invalid')");
      Flyway.configure()
          .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
          .load()
          .migrate();
      try (var rows =
          sql.executeQuery("SELECT id,starts_at,ends_at FROM events WHERE name='Existing event'")) {
        assertThat(rows.next()).isTrue();
        long id = rows.getLong("id");
        assertThat(rows.getObject("starts_at")).isNull();
        assertThat(rows.getObject("ends_at")).isNull();
        sql.executeUpdate(
            "INSERT INTO event_sites(event_id,name,location) VALUES ("
                + id
                + ",'Example site','Example address')");
        sql.executeUpdate("DELETE FROM events WHERE id=" + id);
      }
      try (var rows = sql.executeQuery("SELECT COUNT(*) FROM event_sites")) {
        assertThat(rows.next()).isTrue();
        assertThat(rows.getLong(1)).isZero();
      }
    }
  }
}
