package edu.austincollege.sstation.service;

import edu.austincollege.sstation.domain.PasswordResetToken;
import edu.austincollege.sstation.domain.User;
import edu.austincollege.sstation.repository.PasswordResetTokenRepository;
import edu.austincollege.sstation.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-service password reset (TC-028 / TC-108g). A high-entropy random token is emailed to the
 * user; only its SHA-256 hash is persisted. Tokens are single-use and valid for one hour.
 *
 * <p>{@link #requestReset} never reveals whether an account exists (no user enumeration) — the
 * controller always shows the same generic message.
 */
@Service
public class PasswordResetService {

  private static final Duration TTL = Duration.ofHours(1);

  private final UserRepository users;
  private final PasswordResetTokenRepository tokens;
  private final PasswordEncoder passwordEncoder;
  private final NotificationService notifications;
  private final SecureRandom random = new SecureRandom();

  public PasswordResetService(
      UserRepository users,
      PasswordResetTokenRepository tokens,
      PasswordEncoder passwordEncoder,
      NotificationService notifications) {
    this.users = users;
    this.tokens = tokens;
    this.passwordEncoder = passwordEncoder;
    this.notifications = notifications;
  }

  /**
   * Issues a reset token for the account owning {@code email} and emails the link ({@code
   * resetUrlBase + "?token=" + rawToken}). A no-op if no such account exists.
   */
  @Transactional
  public void requestReset(String email, String resetUrlBase) {
    users
        .findByStudentAcEmail(email)
        .ifPresent(
            user -> {
              String rawToken = newToken();
              tokens.save(
                  new PasswordResetToken(user, sha256(rawToken), LocalDateTime.now().plus(TTL)));
              notifications.sendPasswordReset(email, resetUrlBase + "?token=" + rawToken);
            });
  }

  /**
   * Validates the token (exists, unused, unexpired) and sets the new password. Returns false for
   * any invalid/expired/used token. The token is consumed on success.
   */
  @Transactional
  public boolean reset(String rawToken, String newPassword) {
    if (rawToken == null || rawToken.isBlank() || newPassword == null || newPassword.isBlank()) {
      return false;
    }
    return tokens
        .findByTokenHash(sha256(rawToken))
        .filter(t -> !t.isUsed() && t.getExpiresAt().isAfter(LocalDateTime.now()))
        .map(
            t -> {
              User user = t.getUser();
              user.setPassword(passwordEncoder.encode(newPassword));
              users.save(user);
              t.setUsed(true);
              tokens.save(t);
              return true;
            })
        .orElse(false);
  }

  private String newToken() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e); // never happens on a standard JRE
    }
  }
}
