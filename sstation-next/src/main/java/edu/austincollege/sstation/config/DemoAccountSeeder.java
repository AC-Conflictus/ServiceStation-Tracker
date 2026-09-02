package edu.austincollege.sstation.config;

import edu.austincollege.sstation.domain.Classification;
import edu.austincollege.sstation.domain.Role;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.domain.User;
import edu.austincollege.sstation.domain.UserRole;
import edu.austincollege.sstation.repository.RoleRepository;
import edu.austincollege.sstation.repository.StudentRepository;
import edu.austincollege.sstation.repository.UserRepository;
import edu.austincollege.sstation.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds showcase accounts when the {@code demo} profile is active (TC-114). Pair with {@code prod}
 * and Postgres for Docker/AWS — never use on Austin College production. Passwords must come from
 * {@code SSTATION_DEMO_*_PASSWORD} env vars with no dev defaults ({@link #requirePassword}).
 *
 * <p>Runs before {@link DemoDataSeeder} ({@code @Order(1)}) so Sam Student exists for hour seeding.
 */
@Component
@Profile("demo")
@Order(1)
public class DemoAccountSeeder implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DemoAccountSeeder.class);

  private final UserRepository users;
  private final RoleRepository roles;
  private final UserRoleRepository userRoles;
  private final StudentRepository students;
  private final PasswordEncoder passwordEncoder;

  @Value("${SSTATION_DEMO_ADMIN_PASSWORD:}")
  private String adminPassword;

  @Value("${SSTATION_DEMO_STUDENT_PASSWORD:}")
  private String studentPassword;

  @Value("${SSTATION_DEMO_MODERATOR_PASSWORD:}")
  private String moderatorPassword;

  public DemoAccountSeeder(
      UserRepository users,
      RoleRepository roles,
      UserRoleRepository userRoles,
      StudentRepository students,
      PasswordEncoder passwordEncoder) {
    this.users = users;
    this.roles = roles;
    this.userRoles = userRoles;
    this.students = students;
    this.passwordEncoder = passwordEncoder;
  }

  /** Fails fast when a demo password env var is missing or blank (TC-114 / TC-017). */
  static String requirePassword(String envVarName, String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(
          envVarName + " must be set to a non-blank value when the demo profile is active");
    }
    return value;
  }

  @Override
  @Transactional
  public void run(String... args) {
    log.warn(
        "[demo] showcase profile active — rotate SSTATION_DEMO_*_PASSWORD credentials and never"
            + " enable demo on Austin College production");

    String admin = requirePassword("SSTATION_DEMO_ADMIN_PASSWORD", adminPassword);
    String student = requirePassword("SSTATION_DEMO_STUDENT_PASSWORD", studentPassword);
    String moderator = requirePassword("SSTATION_DEMO_MODERATOR_PASSWORD", moderatorPassword);

    log.info("[demo] seeding roles, users and Sam Student");

    Role adminRole = ensureRole("ROLE_ADMIN");
    Role studentRole = ensureRole("ROLE_STUDENT");
    Role moderatorRole = ensureRole("ROLE_MODERATOR");

    ensureUser("admin", admin, adminRole, null);

    Student sam = ensureDemoStudent();
    ensureUser("student", student, studentRole, sam);

    ensureUser("moderator", moderator, moderatorRole, null);
  }

  private Role ensureRole(String authority) {
    return roles.findByAuthority(authority).orElseGet(() -> roles.save(new Role(authority)));
  }

  private void ensureUser(String username, String rawPassword, Role role, Student student) {
    if (users.existsByUsername(username)) {
      return;
    }
    User user = new User(username, passwordEncoder.encode(rawPassword));
    user.setStudent(student);
    users.save(user);
    if (!userRoles.existsByUserAndRole(user, role)) {
      userRoles.save(new UserRole(user, role));
    }
  }

  private Student ensureDemoStudent() {
    return students
        .findByAcid("AC50000")
        .orElseGet(
            () -> {
              Student s = new Student();
              s.setFirstname("Sam");
              s.setLastname("Student");
              s.setAcid("AC50000");
              s.setAcEmail("student@austincollege.edu");
              s.setAcBox("30000");
              s.setAcYear(2024);
              s.setStatus('A');
              s.setClassification(Classification.JR);
              s.setIsModerator(false);
              s.setPhone("9038132000");
              return students.save(s);
            });
  }
}
