package edu.austincollege.sstation.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * Puts {@link SignInLimiter} in front of every password check (TC-125), local and directory alike.
 *
 * <p>Only a wrong user name or password counts as a failure. A refusal that happens after the
 * password was accepted (account not registered, disabled, a same-name local account) or because a
 * server was unreachable is not a guess, and must not push a real student toward a block.
 */
class LimitedAuthenticationManager implements AuthenticationManager {

  private final AuthenticationManager delegate;
  private final SignInLimiter limiter;

  LimitedAuthenticationManager(AuthenticationManager delegate, SignInLimiter limiter) {
    this.delegate = delegate;
    this.limiter = limiter;
  }

  @Override
  public Authentication authenticate(Authentication attempt) {
    String username = attempt.getName();
    if (limiter.check(username) == SignInLimiter.Status.BLOCKED) {
      throw new SignInLimitException(
          SignInLimitException.Reason.BLOCKED, "Too many failed sign-ins for " + username);
    }
    try {
      Authentication result = delegate.authenticate(attempt);
      limiter.recordSuccess(username);
      return result;
    } catch (BadCredentialsException wrong) {
      limiter.recordFailure(username);
      throw wrong;
    }
  }
}
