package edu.austincollege.sstation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import edu.austincollege.sstation.domain.PasswordResetToken;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.domain.User;
import edu.austincollege.sstation.repository.PasswordResetTokenRepository;
import edu.austincollege.sstation.repository.StudentRepository;
import edu.austincollege.sstation.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Coverage for {@link PasswordResetService} + the V4 schema (TC-108g). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PasswordResetServiceTest {

  @Autowired private UserRepository users;
  @Autowired private PasswordResetTokenRepository tokens;
  @Autowired private StudentRepository students;

  private final PasswordEncoder encoder = new BCryptPasswordEncoder();
  private NotificationService notifications;
  private PasswordResetService service;
  private User user;

  @BeforeEach
  void setUp() {
    notifications = mock(NotificationService.class);
    service = new PasswordResetService(users, tokens, encoder, notifications);

    Student sam = students.save(student());
    user = new User("student", encoder.encode("old_secret"));
    user.setStudent(sam);
    user = users.save(user);
  }

  @Test
  void requestResetIssuesTokenAndEmailsLinkForKnownEmail() {
    service.requestReset("student@austincollege.edu", "https://app/reset-password");

    assertThat(tokens.count()).isEqualTo(1);
    // The raw token is emailed (never stored), so verify the link was sent to the right address.
    verify(notifications)
        .sendPasswordReset(
            eq("student@austincollege.edu"), contains("https://app/reset-password?token="));
  }

  @Test
  void requestResetIsSilentNoOpForUnknownEmail() {
    service.requestReset("nobody@austincollege.edu", "https://app/reset-password");

    assertThat(tokens.count()).isZero();
    verify(notifications, never())
        .sendPasswordReset(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void resetWithValidTokenChangesPasswordAndConsumesToken() {
    String raw = captureRawToken();

    boolean ok = service.reset(raw, "new_secret");

    assertThat(ok).isTrue();
    User reloaded = users.findById(user.getId()).orElseThrow();
    assertThat(encoder.matches("new_secret", reloaded.getPassword())).isTrue();
    assertThat(tokens.findAll().get(0).isUsed()).isTrue();
  }

  @Test
  void resetIsRejectedForReusedToken() {
    String raw = captureRawToken();
    assertThat(service.reset(raw, "first_new_password")).isTrue();
    assertThat(service.reset(raw, "second_new_password")).isFalse(); // single-use
  }

  @Test
  void resetIsRejectedForExpiredToken() {
    // Insert an already-expired token directly (hash of "expired-raw").
    tokens.save(
        new PasswordResetToken(user, sha256("expired-raw"), LocalDateTime.now().minusMinutes(1)));
    assertThat(service.reset("expired-raw", "whatever")).isFalse();
  }

  @Test
  void resetIsRejectedForUnknownToken() {
    assertThat(service.reset("not-a-real-token", "whatever")).isFalse();
  }

  /** Drives requestReset, then recovers the raw token from the captured email link. */
  private String captureRawToken() {
    var captor = org.mockito.ArgumentCaptor.forClass(String.class);
    service.requestReset("student@austincollege.edu", "https://app/reset-password");
    verify(notifications).sendPasswordReset(eq("student@austincollege.edu"), captor.capture());
    String url = captor.getValue();
    return url.substring(url.indexOf("token=") + "token=".length());
  }

  private static String sha256(String value) {
    try {
      byte[] d =
          java.security.MessageDigest.getInstance("SHA-256")
              .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(d);
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Student student() {
    Student s = new Student();
    s.setFirstname("Sam");
    s.setLastname("Student");
    s.setAcid("AC50000");
    s.setAcEmail("student@austincollege.edu");
    s.setStatus('A');
    s.setIsModerator(false);
    return s;
  }

  @Test
  void resetRejectsAPasswordShorterThanThePolicyAllows() {
    String raw = captureRawToken();

    // TC-121: the form's minlength attribute is not a control — anything that isn't a cooperating
    // browser skips it. A short password must be refused here, and the token must survive so the
    // user can try again.
    assertThat(service.reset(raw, "short")).isFalse();
    assertThat(service.reset(raw, "a_long_enough_password")).isTrue();
  }
}
