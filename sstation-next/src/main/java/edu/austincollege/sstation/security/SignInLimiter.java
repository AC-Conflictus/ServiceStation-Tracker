package edu.austincollege.sstation.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Counts failed sign-ins per user name and decides when to slow that user name down (TC-125).
 *
 * <p>In directory mode every wrong password is also a wrong password at AC's Active Directory,
 * which locks an account after its own number of failures. Without a limit here, anyone could lock
 * a student out of every AC system just by mistyping their user name on our sign-in page. The limit
 * is checked <em>before</em> the password, so a refused attempt never reaches the directory.
 *
 * <ul>
 *   <li>After {@code captchaAfter} failures within the window, a CAPTCHA is required — or, when no
 *       CAPTCHA is configured, the user name is blocked right there instead.
 *   <li>After {@code blockAfter} failures, the user name is blocked even with a solved CAPTCHA, so
 *       a paid CAPTCHA-solving service still cannot reach AD's lockout threshold.
 *   <li>A block lasts one window. A successful sign-in clears the count.
 * </ul>
 *
 * <p>Counters live in memory: AC IT runs a single container, and this way a flood of attempts costs
 * no database writes. They reset on restart. The cache is size-capped so inventing user names
 * cannot exhaust memory; in exchange, a flood of tens of thousands of distinct names could evict a
 * real counter early — which only ever loosens the limit for that name back to fresh.
 */
public class SignInLimiter {

  private static final Logger log = LoggerFactory.getLogger(SignInLimiter.class);

  /** Comfortably more distinct user names than a campus has people. */
  private static final int MAX_TRACKED_NAMES = 10_000;

  /** What a sign-in attempt for a given user name is allowed to do right now. */
  public enum Status {
    ALLOWED,
    CAPTCHA_REQUIRED,
    BLOCKED
  }

  /** Recent failures, oldest first, and when a block (if any) ends. */
  private record Attempts(List<Instant> failures, Instant blockedUntil) {}

  private final AuthProperties.SignIn limits;
  private final boolean captchaEnabled;
  private final String emailSuffix;
  private final Clock clock;
  private final Cache<String, Attempts> attempts;

  public SignInLimiter(
      AuthProperties.SignIn limits, boolean captchaEnabled, String emailDomain, Clock clock) {
    this.limits = limits;
    this.captchaEnabled = captchaEnabled;
    this.emailSuffix = "@" + emailDomain.toLowerCase(Locale.ROOT);
    this.clock = clock;
    // Expiry is only memory housekeeping; the window itself is enforced against the clock below.
    this.attempts =
        Caffeine.newBuilder()
            .maximumSize(MAX_TRACKED_NAMES)
            .expireAfterWrite(limits.window())
            .build();
  }

  public Status check(String username) {
    Attempts current = attempts.getIfPresent(key(username));
    if (current == null) {
      return Status.ALLOWED;
    }
    Instant now = clock.instant();
    if (current.blockedUntil() != null && now.isBefore(current.blockedUntil())) {
      return Status.BLOCKED;
    }
    if (captchaEnabled && recent(current, now).size() >= limits.captchaAfter()) {
      return Status.CAPTCHA_REQUIRED;
    }
    return Status.ALLOWED;
  }

  public void recordFailure(String username) {
    String key = key(username);
    attempts
        .asMap()
        .compute(
            key,
            (k, current) -> {
              Instant now = clock.instant();
              if (current != null
                  && current.blockedUntil() != null
                  && now.isBefore(current.blockedUntil())) {
                // Two attempts can both pass check() before either records its failure. The
                // second must not replace the block the first one just set with a fresh count.
                return current;
              }
              List<Instant> failures =
                  current == null ? new ArrayList<>() : new ArrayList<>(recent(current, now));
              failures.add(now);
              int blockAt = captchaEnabled ? limits.blockAfter() : limits.captchaAfter();
              if (failures.size() >= blockAt) {
                log.warn(
                    "Sign-in for '{}' blocked for {} minutes after {} failed attempts",
                    k,
                    limits.window().toMinutes(),
                    failures.size());
                // Start the next window clean: once the block ends, the name is back to fresh.
                return new Attempts(List.of(), now.plus(limits.window()));
              }
              return new Attempts(List.copyOf(failures), null);
            });
  }

  public void recordSuccess(String username) {
    attempts.invalidate(key(username));
  }

  /** Failures still inside the window; ignores any block that has already ended. */
  private List<Instant> recent(Attempts current, Instant now) {
    Instant cutoff = now.minus(limits.window());
    return current.failures().stream().filter(t -> t.isAfter(cutoff)).toList();
  }

  /**
   * One counter per person however they type their name — otherwise {@code JDoe}, {@code jdoe} and
   * {@code jdoe@austincollege.edu} would each get a fresh set of guesses at the same AD account.
   * Matches what {@link DirectoryAccountMapper#normalize} hands the directory.
   */
  String key(String typed) {
    String name = typed == null ? "" : typed.trim().toLowerCase(Locale.ROOT);
    return name.endsWith(emailSuffix)
        ? name.substring(0, name.length() - emailSuffix.length())
        : name;
  }

  /** How long a block lasts, for the sign-in page's message. */
  public Duration window() {
    return limits.window();
  }
}
