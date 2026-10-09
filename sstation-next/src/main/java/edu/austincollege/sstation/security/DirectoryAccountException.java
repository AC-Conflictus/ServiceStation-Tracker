package edu.austincollege.sstation.security;

import org.springframework.security.authentication.AccountStatusException;

/**
 * AC's directory accepted the password, but this app will not sign the person in (TC-124).
 *
 * <p>An {@link AccountStatusException} on purpose: Spring Security's {@code ProviderManager} stops
 * at one of these instead of trying the next provider, so a refused AC sign-in can never fall
 * through to a local-password check.
 */
public class DirectoryAccountException extends AccountStatusException {

  /** Why the sign-in was refused. Each maps to its own message on the sign-in page. */
  public enum Reason {
    /** No student record matches and no directory group grants a role. */
    NOT_REGISTERED,
    /**
     * The user name already belongs to a local account, which a directory sign-in must not take.
     */
    LOCAL_ACCOUNT_CONFLICT
  }

  private final Reason reason;

  public DirectoryAccountException(Reason reason, String message) {
    super(message);
    this.reason = reason;
  }

  public Reason reason() {
    return reason;
  }
}
