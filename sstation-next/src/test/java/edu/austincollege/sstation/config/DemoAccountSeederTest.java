package edu.austincollege.sstation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DemoAccountSeederTest {

  @Test
  void requirePasswordAcceptsNonBlank() {
    assertThat(DemoAccountSeeder.requirePassword("SSTATION_DEMO_ADMIN_PASSWORD", "secret"))
        .isEqualTo("secret");
  }

  @Test
  void requirePasswordRejectsBlank() {
    assertThatThrownBy(() -> DemoAccountSeeder.requirePassword("SSTATION_DEMO_ADMIN_PASSWORD", " "))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SSTATION_DEMO_ADMIN_PASSWORD");
  }
}
