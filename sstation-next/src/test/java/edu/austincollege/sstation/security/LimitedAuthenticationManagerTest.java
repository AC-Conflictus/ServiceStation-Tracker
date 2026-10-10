package edu.austincollege.sstation.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.austincollege.sstation.security.CaptchaVerifier.Result;
import edu.austincollege.sstation.security.SignInLimiter.Status;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * TC-125: the limit sits in front of the password check, and only wrong passwords count toward it.
 */
class LimitedAuthenticationManagerTest {

  private final AuthenticationManager passwords = mock(AuthenticationManager.class);
  private final SignInLimiter limiter =
      new SignInLimiter(
          new AuthProperties.SignIn(null, null, null),
          false,
          "austincollege.edu",
          Clock.systemUTC());
  private final LimitedAuthenticationManager manager =
      new LimitedAuthenticationManager(passwords, limiter, (token, ip) -> Result.UNAVAILABLE);

  private static Authentication attempt() {
    return UsernamePasswordAuthenticationToken.unauthenticated("jdoe", "guess");
  }

  private void attemptFailsWith(RuntimeException failure) {
    when(passwords.authenticate(any())).thenThrow(failure);
    for (int i = 0; i < 3; i++) {
      assertThatThrownBy(() -> manager.authenticate(attempt())).isSameAs(failure);
    }
  }

  @Test
  void aBlockedNameNeverReachesThePasswordCheck() {
    // This is the whole point in directory mode: a refused attempt must not count at AD.
    for (int i = 0; i < 3; i++) {
      limiter.recordFailure("jdoe");
    }

    assertThatThrownBy(() -> manager.authenticate(attempt()))
        .isInstanceOf(SignInLimitException.class);
    verify(passwords, never()).authenticate(any());
  }

  @Test
  void wrongPasswordsCount() {
    attemptFailsWith(new BadCredentialsException("wrong"));

    assertThat(limiter.check("jdoe")).isEqualTo(Status.BLOCKED);
  }

  @Test
  void refusalsAfterTheRightPasswordDoNotCount() {
    attemptFailsWith(
        new DirectoryAccountException(
            DirectoryAccountException.Reason.NOT_REGISTERED, "not registered"));

    assertThat(limiter.check("jdoe")).isEqualTo(Status.ALLOWED);
  }

  @Test
  void anUnreachableDirectoryDoesNotCount() {
    // An outage is not a guess; counting it would block every student who tried during it.
    attemptFailsWith(new InternalAuthenticationServiceException("directory down"));

    assertThat(limiter.check("jdoe")).isEqualTo(Status.ALLOWED);
  }

  @Test
  void anAccountAlreadyLockedDoesNotCount() {
    attemptFailsWith(new LockedException("locked"));

    assertThat(limiter.check("jdoe")).isEqualTo(Status.ALLOWED);
  }

  @Test
  void aSuccessfulSignInClearsEarlierFailures() {
    limiter.recordFailure("jdoe");
    limiter.recordFailure("jdoe");
    Authentication ok =
        UsernamePasswordAuthenticationToken.authenticated("jdoe", null, java.util.List.of());
    when(passwords.authenticate(any())).thenReturn(ok);

    assertThat(manager.authenticate(attempt())).isSameAs(ok);
    limiter.recordFailure("jdoe");

    assertThat(limiter.check("jdoe")).isEqualTo(Status.ALLOWED);
  }
}
