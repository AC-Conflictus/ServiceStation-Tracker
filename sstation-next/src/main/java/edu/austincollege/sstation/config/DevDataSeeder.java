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
 * Seeds the dev database with the three accounts the Grails app shipped (admin / student /
 * moderator) plus the demo student profile. Mirrors the gating of the Grails {@code BootStrap}:
 *
 * <ul>
 *   <li>Roles are created idempotently (here, only in the dev profile — prod provisions accounts
 *       out of band rather than seeding a permanent admin backdoor, which was the Grails TC-007
 *       bug).
 *   <li>Test users + demo data are dev-only — this bean is {@code @Profile("dev")}.
 *   <li>Passwords come from env vars (TC-017); the defaults below exist purely for local dev
 *       convenience and never ship to prod, which doesn't run this seeder at all.
 * </ul>
 *
 * Unlike Grails, the {@code student} account is linked to its {@link Student} by a real FK
 * (TC-009), not an email-string match.
 */
@Component
@Profile("dev")
@Order(1)
public class DevDataSeeder implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

  private final UserRepository users;
  private final RoleRepository roles;
  private final UserRoleRepository userRoles;
  private final StudentRepository students;
  private final PasswordEncoder passwordEncoder;

  @Value("${SSTATION_DEV_ADMIN_PASSWORD:admin_secret}")
  private String adminPassword;

  @Value("${SSTATION_DEV_STUDENT_PASSWORD:student_secret}")
  private String studentPassword;

  @Value("${SSTATION_DEV_MODERATOR_PASSWORD:moderator_secret}")
  private String moderatorPassword;

  public DevDataSeeder(
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

  @Override
  @Transactional
  public void run(String... args) {
    log.info("[dev] seeding roles, users and demo student");

    Role adminRole = ensureRole("ROLE_ADMIN");
    Role studentRole = ensureRole("ROLE_STUDENT");
    Role moderatorRole = ensureRole("ROLE_MODERATOR");

    ensureUser("admin", adminPassword, adminRole, null);

    Student demo = ensureDemoStudent();
    ensureUser("student", studentPassword, studentRole, demo);

    ensureUser("moderator", moderatorPassword, moderatorRole, null);
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
