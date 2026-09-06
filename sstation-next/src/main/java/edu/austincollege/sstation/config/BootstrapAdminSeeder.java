package edu.austincollege.sstation.config;

import edu.austincollege.sstation.domain.Role;
import edu.austincollege.sstation.domain.User;
import edu.austincollege.sstation.domain.UserRole;
import edu.austincollege.sstation.repository.RoleRepository;
import edu.austincollege.sstation.repository.UserRepository;
import edu.austincollege.sstation.repository.UserRoleRepository;
import edu.austincollege.sstation.service.PasswordPolicy;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first ADMIN account on a bare {@code prod} boot (TC-121).
 *
 * <p>Nothing else does. {@code DevDataSeeder} is {@code dev}-only and {@code DemoAccountSeeder} is
 * {@code demo}-only, so a genuine production start came up with an empty {@code users} table and no
 * supported way to create an account — no CLI, no first-run signup, no documented SQL. AC IT would
 * have been handed a sign-in form they could not get past. The {@code demo} profile hid this,
 * because every environment anyone had actually run seeds accounts.
 *
 * <p>Deliberately scoped {@code prod & !demo}: under {@code prod,demo} the demo seeder owns account
 * creation, and two runners racing for an empty table would make which one wins depend on bean
 * ordering.
 *
 * <p>The account is created with {@code mustChangePassword} set. Its password arrives in an
 * environment variable that stays readable in the deployment's own configuration, so it is a
 * single-use credential for getting in the door once.
 */
@Component
@Profile("prod & !demo")
public class BootstrapAdminSeeder implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(BootstrapAdminSeeder.class);

  private final UserRepository users;
  private final RoleRepository roles;
  private final UserRoleRepository userRoles;
  private final PasswordEncoder passwordEncoder;

  @Value("${SSTATION_BOOTSTRAP_ADMIN_USERNAME:admin}")
  private String username;

  @Value("${SSTATION_BOOTSTRAP_ADMIN_PASSWORD:}")
  private String password;

  public BootstrapAdminSeeder(
      UserRepository users,
      RoleRepository roles,
      UserRoleRepository userRoles,
      PasswordEncoder passwordEncoder) {
    this.users = users;
    this.roles = roles;
    this.userRoles = userRoles;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (users.count() > 0) {
      // Gating on an empty table rather than on "does this username exist" is deliberate: it means
      // the runner can never resurrect an account someone deliberately deleted, and leaving the
      // env vars set in the deployment config is harmless from the second boot onwards.
      log.debug("[bootstrap] accounts already exist — nothing to do");
      return;
    }

    if (password == null || password.isBlank()) {
      // Not fatal. A bare prod boot with no accounts is a legitimate state — an operator may
      // intend to create the first user by SQL. But it must not be silent, because the symptom
      // (a sign-in form that rejects everything) looks nothing like the cause.
      log.warn(
          "[bootstrap] this database has no user accounts and SSTATION_BOOTSTRAP_ADMIN_PASSWORD is"
              + " not set, so nobody can sign in. Set SSTATION_BOOTSTRAP_ADMIN_PASSWORD (and"
              + " optionally SSTATION_BOOTSTRAP_ADMIN_USERNAME, default 'admin') and restart. See"
              + " the 'First login' section of DEPLOY.md.");
      return;
    }

    Optional<String> weakness = PasswordPolicy.validate(password);
    if (weakness.isPresent()) {
      // Fatal, unlike the missing-password case: the operator clearly meant to bootstrap an admin,
      // and quietly creating a weak one on an internet-facing host is the worse outcome.
      throw new IllegalStateException(
          "SSTATION_BOOTSTRAP_ADMIN_PASSWORD is not acceptable: " + weakness.get());
    }

    Role adminRole =
        roles.findByAuthority("ROLE_ADMIN").orElseGet(() -> roles.save(new Role("ROLE_ADMIN")));

    User admin = new User(username, passwordEncoder.encode(password));
    admin.setMustChangePassword(true);
    admin = users.save(admin);
    userRoles.save(new UserRole(admin, adminRole));

    log.warn(
        "[bootstrap] created the first administrator '{}'. This password came from an environment"
            + " variable and is single-use: you will be required to change it at first sign-in."
            + " Remove SSTATION_BOOTSTRAP_ADMIN_PASSWORD from the deployment configuration"
            + " afterwards.",
        username);
  }
}
