package edu.austincollege.sstation.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.austincollege.sstation.security.CaptchaVerifier.Result;
import edu.austincollege.sstation.security.SignInLimitException.Reason;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * TC-125 with a CAPTCHA configured: once a user name needs one, nothing reaches the password check
 * (and so AC's directory) without a CAPTCHA that Cloudflare confirms.
 */
class CaptchaGateTest {

  private final AuthenticationManager passwords = mock(AuthenticationManager.class);
  private final SignInLimiter limiter =
      new SignInLimiter(
          new AuthProperties.SignIn(null, null, null),
          true,
          "austincollege.edu",
          Clock.systemUTC());
  private Result cloudflareSays = Result.VERIFIED;
  private final LimitedAuthenticationManager manager =
      new LimitedAuthenticationManager(passwords, limiter, (token, ip) -> cloudflareSays);

  private static Authentication attempt(String captchaToken) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    if (captchaToken != null) {
      request.setParameter(SignInDetails.TURNSTILE_FIELD, captchaToken);
    }
    UsernamePasswordAuthenticationToken attempt =
        UsernamePasswordAuthenticationToken.unauthenticated("jdoe", "guess");
    attempt.setDetails(new SignInDetails(request));
    return attempt;
  }

  private void needsCaptcha() {
    for (int i = 0; i < 3; i++) {
      limiter.recordFailure("jdoe");
    }
  }

  private void refusedFor(Authentication attempt, Reason reason) {
    assertThatThrownBy(() -> manager.authenticate(attempt))
        .isInstanceOfSatisfying(
            SignInLimitException.class, e -> assertThat(e.reason()).isEqualTo(reason));
    verify(passwords, never()).authenticate(any());
  }

  @Test
  void noCaptchaIsAskedForBeforeTheThirdFailure() {
    Authentication ok = UsernamePasswordAuthenticationToken.authenticated("jdoe", null, List.of());
    when(passwords.authenticate(any())).thenReturn(ok);
    cloudflareSays = Result.UNAVAILABLE;

    assertThat(manager.authenticate(attempt(null))).isSameAs(ok);
  }

  @Test
  void withoutATokenThePasswordIsNotChecked() {
    needsCaptcha();

    refusedFor(attempt(null), Reason.CAPTCHA_REQUIRED);
    refusedFor(attempt(" "), Reason.CAPTCHA_REQUIRED);
  }

  @Test
  void aTokenCloudflareRejectsIsRefused() {
    needsCaptcha();
    cloudflareSays = Result.REJECTED;

    refusedFor(attempt("forged"), Reason.CAPTCHA_FAILED);
  }

  @Test
  void ifCloudflareCannotBeReachedTheAttemptIsRefusedNotWaved() {
    needsCaptcha();
    cloudflareSays = Result.UNAVAILABLE;

    refusedFor(attempt("anything"), Reason.CAPTCHA_UNAVAILABLE);
  }

  @Test
  void refusedCaptchasDoNotCountTowardTheBlock() {
    // No password was tried, so there is nothing to count; a user with a flaky CAPTCHA is not
    // pushed toward a block by it.
    needsCaptcha();
    cloudflareSays = Result.REJECTED;
    for (int i = 0; i < 5; i++) {
      assertThatThrownBy(() -> manager.authenticate(attempt("bad")))
          .isInstanceOf(SignInLimitException.class);
    }

    assertThat(limiter.check("jdoe")).isEqualTo(SignInLimiter.Status.CAPTCHA_REQUIRED);
  }

  @Test
  void aConfirmedCaptchaLetsThePasswordBeCheckedUntilTheFifthFailureBlocks() {
    needsCaptcha();
    when(passwords.authenticate(any())).thenThrow(new BadCredentialsException("wrong"));

    for (int i = 0; i < 2; i++) {
      assertThatThrownBy(() -> manager.authenticate(attempt("solved")))
          .isInstanceOf(BadCredentialsException.class);
    }

    // A paid CAPTCHA solver gets no further than this.
    assertThat(limiter.check("jdoe")).isEqualTo(SignInLimiter.Status.BLOCKED);
    assertThatThrownBy(() -> manager.authenticate(attempt("solved")))
        .isInstanceOfSatisfying(
            SignInLimitException.class, e -> assertThat(e.reason()).isEqualTo(Reason.BLOCKED));
  }
}
