package edu.austincollege.sstation.security;

import edu.austincollege.sstation.domain.AuthSource;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.domain.User;
import edu.austincollege.sstation.repository.StudentRepository;
import edu.austincollege.sstation.repository.UserRepository;
import edu.austincollege.sstation.repository.UserRoleRepository;
import edu.austincollege.sstation.security.DirectoryAccountException.Reason;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs once AC's directory has accepted someone's user name and password (TC-124), and decides who
 * they are <em>in this app</em>.
 *
 * <p>The directory answers "is this really jdoe?". It does not know who volunteers, who runs the
 * office, or which student record holds jdoe's hours — that stays here, so AC IT never has to model
 * Service Station roles in their directory unless they want to.
 *
 * <ul>
 *   <li><b>First sign-in creates the account.</b> Nobody has to pre-create 1,300 student logins:
 *       the person's student record is found by email (the directory's {@code mail} attribute, else
 *       {@code username@emailDomain}) and linked. That link is the real FK from TC-009, not the
 *       Grails app's email-string match at every request.
 *   <li><b>Roles</b> are the union of: STUDENT if linked to a student record; MODERATOR if an admin
 *       has promoted that student on the Moderators page; whatever is granted in this app's own
 *       {@code user_roles}; and ADMIN / MODERATOR from the optional directory groups, re-read at
 *       every sign-in so removing someone from the group takes effect next time they sign in.
 *   <li><b>Nobody gets in with no role.</b> A valid AC account that matches no student and no group
 *       is refused with a message to contact the office — and no account row is created.
 *   <li><b>A directory sign-in never takes over a local account.</b> Local accounts are the
 *       bootstrap admin and break-glass access; if an AC user name collides with one, the sign-in
 *       is refused rather than merged.
 * </ul>
 */
public class DirectoryAccountMapper implements UserDetailsContextMapper {

  private static final Logger log = LoggerFactory.getLogger(DirectoryAccountMapper.class);

  private final UserRepository users;
  private final UserRoleRepository userRoles;
  private final StudentRepository students;
  private final PasswordEncoder passwordEncoder;
  private final AuthProperties.Directory config;

  public DirectoryAccountMapper(
      UserRepository users,
      UserRoleRepository userRoles,
      StudentRepository students,
      PasswordEncoder passwordEncoder,
      AuthProperties.Directory config) {
    this.users = users;
    this.userRoles = userRoles;
    this.students = students;
    this.passwordEncoder = passwordEncoder;
    this.config = config;
  }

  @Override
  @Transactional
  public UserDetails mapUserFromContext(
      DirContextOperations entry,
      String typedUsername,
      Collection<? extends GrantedAuthority> directoryAuthorities) {
    String username = normalize(typedUsername);
    try {
      return map(entry, username);
    } catch (DataAccessException db) {
      // Surface as "sign-in is unavailable", not as a 500 from inside the security filter.
      throw new InternalAuthenticationServiceException("Could not load account " + username, db);
    }
  }

  private UserDetails map(DirContextOperations entry, String username) {
    Set<String> groupRoles = rolesFromGroups(entry);
    Optional<User> existing = users.findByUsername(username);

    if (existing.isPresent() && !existing.get().isDirectoryAccount()) {
      log.warn("AC sign-in for '{}' refused: that user name belongs to a local account", username);
      throw new DirectoryAccountException(
          Reason.LOCAL_ACCOUNT_CONFLICT, "User name belongs to a local account: " + username);
    }

    User user;
    if (existing.isPresent()) {
      user = existing.get();
      if (!user.isEnabled()) {
        throw new DisabledException("Account disabled in Service Station: " + username);
      }
      if (user.isAccountLocked()) {
        throw new LockedException("Account locked in Service Station: " + username);
      }
      if (user.getStudent() == null) {
        // The student may have been imported since this person last signed in.
        findStudent(entry, username).ifPresent(user::setStudent);
      }
    } else {
      Optional<Student> student = findStudent(entry, username);
      if (student.isEmpty() && groupRoles.isEmpty()) {
        log.info("AC sign-in for '{}' refused: no student record and no directory group", username);
        throw new DirectoryAccountException(
            Reason.NOT_REGISTERED, "No Service Station record for " + username);
      }
      // The column is NOT NULL, and a local password must never work for this account. A random
      // value nobody knows, hashed, satisfies both — and the local sign-in path refuses directory
      // accounts outright anyway (CustomUserDetailsService).
      user = new User(username, passwordEncoder.encode(UUID.randomUUID().toString()));
      user.setAuthSource(AuthSource.DIRECTORY);
      student.ifPresent(user::setStudent);
      users.save(user);
      log.info(
          "Created Service Station account for AC user '{}'{}",
          username,
          student.map(s -> " linked to student " + s.getAcid()).orElse(""));
    }

    Set<String> authorities = new LinkedHashSet<>(userRoles.findAuthoritiesByUser(user));
    authorities.addAll(groupRoles);
    Student student = user.getStudent();
    if (student != null) {
      authorities.add("ROLE_STUDENT");
      if (Boolean.TRUE.equals(student.getIsModerator())) {
        authorities.add("ROLE_MODERATOR");
      }
    }
    if (authorities.isEmpty()) {
      // An existing account that has lost everything — e.g. a graduate whose student record was
      // deleted and who is in no group. Same answer as a newcomer with nothing to match.
      throw new DirectoryAccountException(
          Reason.NOT_REGISTERED, "No Service Station role for " + username);
    }

    return SstationUserDetails.forDirectoryAccount(
        username, authorities.stream().map(SimpleGrantedAuthority::new).toList());
  }

  private Optional<Student> findStudent(DirContextOperations entry, String username) {
    String mail = entry.getStringAttribute("mail");
    String email =
        (mail != null && !mail.isBlank()) ? mail.trim() : username + "@" + config.emailDomain();
    return students.findByAcEmailIgnoreCase(email);
  }

  private Set<String> rolesFromGroups(DirContextOperations entry) {
    Set<String> roles = new LinkedHashSet<>();
    String[] memberOf = entry.getStringAttributes("memberOf");
    if (memberOf == null) {
      return roles;
    }
    for (String group : memberOf) {
      if (sameGroup(group, config.adminGroup())) {
        roles.add("ROLE_ADMIN");
      }
      if (sameGroup(group, config.moderatorGroup())) {
        roles.add("ROLE_MODERATOR");
      }
    }
    return roles;
  }

  /**
   * Directory DNs are case-insensitive and directories differ on spacing after commas, so compare
   * them normalised rather than as raw strings — "CN=Admins, OU=Groups" is the same group as
   * "cn=admins,ou=groups".
   */
  private static boolean sameGroup(String memberOf, String configured) {
    if (configured == null || configured.isBlank()) {
      return false;
    }
    return canonical(memberOf).equals(canonical(configured));
  }

  private static String canonical(String dn) {
    return dn.replaceAll("\\s*,\\s*", ",").trim().toLowerCase(Locale.ROOT);
  }

  /**
   * One account per person however they type their name: directories match user names
   * case-insensitively, and people type {@code jdoe@austincollege.edu} as often as {@code jdoe}.
   */
  String normalize(String typed) {
    String username = typed.trim().toLowerCase(Locale.ROOT);
    String suffix = "@" + config.emailDomain().toLowerCase(Locale.ROOT);
    if (username.endsWith(suffix)) {
      username = username.substring(0, username.length() - suffix.length());
    }
    return username;
  }

  /** Writing back to the directory is AC IT's business, never this app's. */
  @Override
  public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
    throw new UnsupportedOperationException("Service Station never writes to AC's directory");
  }
}
