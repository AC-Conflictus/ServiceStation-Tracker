package edu.austincollege.sstation.service;

import edu.austincollege.sstation.domain.User;
import edu.austincollege.sstation.repository.UserRepository;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lets a signed-in user change their own password (TC-121).
 *
 * <p>The app had no such flow at all. The only way to change a password was the emailed reset link
 * — and {@code SSTATION_MAIL_HOST} defaults to empty, so on a fresh deployment no mail is sent and
 * that path simply does not work. An account created by the bootstrap runner therefore had no way
 * to ever stop using the password it was handed.
 */
@Service
public class PasswordChangeService {

  /** Why a change was refused. {@link #OK} is the only success. */
  public enum Result {
    OK(null),
    WRONG_CURRENT_PASSWORD("That isn't your current password."),
    SAME_AS_CURRENT("Choose a password you haven't already been using."),
    TOO_WEAK(null);

    private final String message;

    Result(String message) {
      this.message = message;
    }

    public String message() {
      return message;
    }
  }

  private final UserRepository users;
  private final PasswordEncoder passwordEncoder;

  public PasswordChangeService(UserRepository users, PasswordEncoder passwordEncoder) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Verifies {@code currentPassword} and, if it holds, stores {@code newPassword} and clears the
   * must-change flag.
   *
   * <p>Rejecting a new password identical to the current one is the point of the whole card, not a
   * nicety: without it the bootstrap admin can satisfy the forced change by re-entering the value
   * from the environment variable, and the credential we were trying to retire stays live.
   */
  @Transactional
  public Result change(String username, String currentPassword, String newPassword) {
    User user = users.findByUsername(username).orElseThrow();

    if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
      return Result.WRONG_CURRENT_PASSWORD;
    }
    Optional<String> weakness = PasswordPolicy.validate(newPassword);
    if (weakness.isPresent()) {
      return Result.TOO_WEAK;
    }
    if (passwordEncoder.matches(newPassword, user.getPassword())) {
      return Result.SAME_AS_CURRENT;
    }

    user.setPassword(passwordEncoder.encode(newPassword));
    user.setMustChangePassword(false);
    users.save(user);
    return Result.OK;
  }

  /** The policy message for a rejected new password, so the caller need not duplicate the rules. */
  public String weaknessMessage(String newPassword) {
    return PasswordPolicy.validate(newPassword).orElse("");
  }
}
