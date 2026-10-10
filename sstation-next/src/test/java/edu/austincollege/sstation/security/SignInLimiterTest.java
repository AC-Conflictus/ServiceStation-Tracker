package edu.austincollege.sstation.security;

import static org.assertj.core.api.Assertions.assertThat;

import edu.austincollege.sstation.security.SignInLimiter.Status;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** TC-125: the counting rules, against a clock the test moves by hand. */
class SignInLimiterTest {

  /** Captcha at 3, block at 5, 15-minute window — the shipped defaults. */
  private static final AuthProperties.SignIn DEFAULTS = new AuthProperties.SignIn(null, null, null);

  private final MovableClock clock = new MovableClock();

  private SignInLimiter limiter(boolean captchaEnabled) {
    return new SignInLimiter(DEFAULTS, captchaEnabled, "austincollege.edu", clock);
  }

  private static void fail(SignInLimiter limiter, String username, int times) {
    for (int i = 0; i < times; i++) {
      limiter.recordFailure(username);
    }
  }

  @Test
  void withoutACaptchaTheThirdFailureBlocksForTheWholeWindow() {
    SignInLimiter limiter = limiter(false);

    fail(limiter, "jdoe", 2);
    assertThat(limiter.check("jdoe")).isEqualTo(Status.ALLOWED);

    fail(limiter, "jdoe", 1);
    assertThat(limiter.check("jdoe")).isEqualTo(Status.BLOCKED);
    clock.advance(Duration.ofMinutes(15).minusSeconds(1));
    assertThat(limiter.check("jdoe")).isEqualTo(Status.BLOCKED);
    clock.advance(Duration.ofSeconds(1));
    assertThat(limiter.check("jdoe")).isEqualTo(Status.ALLOWED);
  }

  @Test
  void afterABlockEndsTheNameStartsFresh() {
    SignInLimiter limiter = limiter(false);
    fail(limiter, "jdoe", 3);
    clock.advance(Duration.ofMinutes(15));

    fail(limiter, "jdoe", 2);

    assertThat(limiter.check("jdoe")).isEqualTo(Status.ALLOWED);
  }

  @Test
  void withACaptchaTheThirdFailureAsksForItAndTheFifthBlocks() {
    SignInLimiter limiter = limiter(true);

    fail(limiter, "jdoe", 2);
    assertThat(limiter.check("jdoe")).isEqualTo(Status.ALLOWED);
    fail(limiter, "jdoe", 1);
    assertThat(limiter.check("jdoe")).isEqualTo(Status.CAPTCHA_REQUIRED);
    fail(limiter, "jdoe", 1);
    assertThat(limiter.check("jdoe")).isEqualTo(Status.CAPTCHA_REQUIRED);
    fail(limiter, "jdoe", 1);
    assertThat(limiter.check("jdoe")).isEqualTo(Status.BLOCKED);
  }

  @Test
  void aFailureThatLandsDuringABlockDoesNotLiftIt() {
    // Two wrong passwords submitted together both pass check() before either is recorded.
    SignInLimiter limiter = limiter(false);
    fail(limiter, "jdoe", 3);

    limiter.recordFailure("jdoe");

    assertThat(limiter.check("jdoe")).isEqualTo(Status.BLOCKED);
  }

  @Test
  void failuresOlderThanTheWindowStopCounting() {
    SignInLimiter limiter = limiter(false);
    fail(limiter, "jdoe", 2);
    clock.advance(Duration.ofMinutes(15).plusSeconds(1));

    fail(limiter, "jdoe", 2);

    assertThat(limiter.check("jdoe")).isEqualTo(Status.ALLOWED);
  }

  @Test
  void aSuccessfulSignInClearsTheCount() {
    SignInLimiter limiter = limiter(false);
    fail(limiter, "jdoe", 2);

    limiter.recordSuccess("jdoe");
    fail(limiter, "jdoe", 2);

    assertThat(limiter.check("jdoe")).isEqualTo(Status.ALLOWED);
  }

  @Test
  void everySpellingOfOnePersonSharesOneCounter() {
    SignInLimiter limiter = limiter(false);

    limiter.recordFailure("jdoe");
    limiter.recordFailure("JDoe");
    limiter.recordFailure(" jdoe@AustinCollege.edu ");

    assertThat(limiter.check("jdoe")).isEqualTo(Status.BLOCKED);
    assertThat(limiter.check("jdoe@example.com"))
        .as("only AC's domain is stripped")
        .isEqualTo(Status.ALLOWED);
  }

  @Test
  void oneNamesFailuresNeverBlockAnother() {
    SignInLimiter limiter = limiter(false);

    fail(limiter, "jdoe", 3);

    assertThat(limiter.check("asmith")).isEqualTo(Status.ALLOWED);
  }

  /** A clock that only moves when the test says so. */
  private static final class MovableClock extends Clock {
    private Instant now = Instant.parse("2026-10-09T12:00:00Z");

    void advance(Duration by) {
      now = now.plus(by);
    }

    @Override
    public Instant instant() {
      return now;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }
  }
}
