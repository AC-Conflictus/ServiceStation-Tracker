package edu.austincollege.sstation.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * Puts {@link SignInLimiter} in front of every password check (TC-125), local and directory alike.
 *
 * <p>A user name that needs a CAPTCHA has it checked here, before the password: an attempt without
 * a solved CAPTCHA is refused without ever reaching AC's directory, and does not count as a
 * failure, since no password was tried.
 *
 * <p>Only a wrong user name or password counts as a failure. A refusal that happens after the
 * password was accepted (account not registered, disabled, a same-name local account) or because a
 * server was unreachable is not a guess, and must not push a real student toward a block.
 */
class LimitedAuthenticationManager implements AuthenticationManager {

  private final AuthenticationManager delegate;
  private final SignInLimiter limiter;
  private final CaptchaVerifier captcha;

  LimitedAuthenticationManager(
      AuthenticationManager delegate, SignInLimiter limiter, CaptchaVerifier captcha) {
    this.delegate = delegate;
    this.limiter = limiter;
    this.captcha = captcha;
  }

  @Override
  public Authentication authenticate(Authentication attempt) {
    String username = attempt.getName();
    switch (limiter.check(username)) {
      case BLOCKED ->
          throw new SignInLimitException(
              SignInLimitException.Reason.BLOCKED, "Too many failed sign-ins for " + username);
      case CAPTCHA_REQUIRED -> requireCaptcha(attempt);
      case ALLOWED -> {}
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

  private void requireCaptcha(Authentication attempt) {
    String token = null;
    String remoteIp = null;
    if (attempt.getDetails() instanceof SignInDetails details) {
      token = details.captchaToken();
      remoteIp = details.getRemoteAddress();
    }
    if (token == null || token.isBlank()) {
      throw new SignInLimitException(
          SignInLimitException.Reason.CAPTCHA_REQUIRED,
          "CAPTCHA required for " + attempt.getName());
    }
    switch (captcha.verify(token, remoteIp)) {
      case VERIFIED -> {}
      case REJECTED ->
          throw new SignInLimitException(
              SignInLimitException.Reason.CAPTCHA_FAILED, "CAPTCHA not solved");
      case UNAVAILABLE ->
          throw new SignInLimitException(
              SignInLimitException.Reason.CAPTCHA_UNAVAILABLE, "CAPTCHA could not be checked");
    }
  }
}
