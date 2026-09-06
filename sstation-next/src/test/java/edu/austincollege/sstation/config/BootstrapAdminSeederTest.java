package edu.austincollege.sstation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.austincollege.sstation.domain.Role;
import edu.austincollege.sstation.domain.User;
import edu.austincollege.sstation.repository.RoleRepository;
import edu.austincollege.sstation.repository.UserRepository;
import edu.austincollege.sstation.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/** TC-121: the day-one admin bootstrap, and the three cases where it declines to act. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BootstrapAdminSeederTest {

  @Autowired private UserRepository users;
  @Autowired private RoleRepository roles;
  @Autowired private UserRoleRepository userRoles;

  private final PasswordEncoder encoder = new BCryptPasswordEncoder();
  private BootstrapAdminSeeder seeder;

  @BeforeEach
  void setUp() {
    seeder = new BootstrapAdminSeeder(users, roles, userRoles, encoder);
  }

  @Test
  void createsTheFirstAdminOnAnEmptyDatabase() {
    configure("admin", "a_strong_bootstrap_password");

    seeder.run();

    User admin = users.findByUsername("admin").orElseThrow();
    assertThat(encoder.matches("a_strong_bootstrap_password", admin.getPassword())).isTrue();
    assertThat(userRoles.findAuthoritiesByUser(admin)).containsExactly("ROLE_ADMIN");
    // The credential came from an environment variable, so it is single-use by design.
    assertThat(admin.isMustChangePassword()).isTrue();
  }

  @Test
  void honoursACustomUsername() {
    configure("ac-it-admin", "a_strong_bootstrap_password");

    seeder.run();

    assertThat(users.findByUsername("ac-it-admin")).isPresent();
    assertThat(users.findByUsername("admin")).isEmpty();
  }

  @Test
  void doesNothingWhenAnyAccountAlreadyExists() {
    users.save(new User("someone", encoder.encode("their_own_password")));
    configure("admin", "a_strong_bootstrap_password");

    seeder.run();

    // Gated on an empty table, not on the username — so it can never resurrect a deleted admin,
    // and leaving the env vars set from the first boot stays harmless.
    assertThat(users.findByUsername("admin")).isEmpty();
    assertThat(users.count()).isEqualTo(1);
  }

  @Test
  void doesNothingButWarnsWhenNoPasswordIsSupplied() {
    configure("admin", "");

    seeder.run();

    // A bare prod boot with no accounts is a legitimate state, so this is not fatal.
    assertThat(users.count()).isZero();
  }

  @Test
  void refusesToStartWithAWeakBootstrapPassword() {
    configure("admin", "short");

    assertThatThrownBy(() -> seeder.run())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SSTATION_BOOTSTRAP_ADMIN_PASSWORD");

    assertThat(users.count()).isZero();
  }

  @Test
  void reusesAnExistingAdminRoleRatherThanDuplicatingIt() {
    Role existing = roles.save(new Role("ROLE_ADMIN"));
    configure("admin", "a_strong_bootstrap_password");

    seeder.run();

    assertThat(roles.findAll()).extracting(Role::getAuthority).containsExactly("ROLE_ADMIN");
    assertThat(roles.findByAuthority("ROLE_ADMIN").orElseThrow().getId())
        .isEqualTo(existing.getId());
  }

  /** The seeder reads both values from {@code @Value}-injected fields. */
  private void configure(String username, String password) {
    ReflectionTestUtils.setField(seeder, "username", username);
    ReflectionTestUtils.setField(seeder, "password", password);
  }
}
