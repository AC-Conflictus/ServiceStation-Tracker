package edu.austincollege.sstation.security;

import org.springframework.security.core.AuthenticationException;

/**
 * A sign-in attempt refused by {@link SignInLimiter} before its password was checked (TC-125).
 * Because the password is never checked, this says nothing about whether it was right.
 */
public class SignInLimitException extends AuthenticationException {

  /** Why the attempt was refused. Each maps to its own message on the sign-in page. */
  public enum Reason {
    /** Too many recent failures for this user name. */
    BLOCKED,
    /** This user name needs a CAPTCHA and the form did not carry one. */
    CAPTCHA_REQUIRED,
    /** The CAPTCHA was missing, expired or wrong. */
    CAPTCHA_FAILED,
    /** The CAPTCHA could not be checked because the CAPTCHA service was unreachable. */
    CAPTCHA_UNAVAILABLE
  }

  private final Reason reason;

  public SignInLimitException(Reason reason, String message) {
    super(message);
    this.reason = reason;
  }

  public Reason reason() {
    return reason;
  }
}
