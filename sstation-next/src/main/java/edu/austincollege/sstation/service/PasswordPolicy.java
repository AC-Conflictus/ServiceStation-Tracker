package edu.austincollege.sstation.service;

/**
 * The one place a new password is judged (TC-121).
 *
 * <p>Before this existed the only length rule anywhere was the {@code minlength="6"} attribute on
 * the reset form — HTML validation, which any client that isn't a cooperating browser simply
 * ignores. Both password-setting paths now check server-side.
 */
public final class PasswordPolicy {

  /** Minimum length. NIST SP 800-63B's floor for a user-chosen secret. */
  public static final int MIN_LENGTH = 8;

  private PasswordPolicy() {}

  /** Returns a human-readable problem with {@code candidate}, or empty if it is acceptable. */
  public static java.util.Optional<String> validate(String candidate) {
    if (candidate == null || candidate.isBlank()) {
      return java.util.Optional.of("Enter a new password.");
    }
    if (candidate.length() < MIN_LENGTH) {
      return java.util.Optional.of("Password must be at least " + MIN_LENGTH + " characters long.");
    }
    return java.util.Optional.empty();
  }
}
