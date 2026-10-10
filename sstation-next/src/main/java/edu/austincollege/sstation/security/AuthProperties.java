package edu.austincollege.sstation.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How people sign in (TC-124). Bound from {@code sstation.auth.*}, which {@code application.yml}
 * maps to the {@code SSTATION_AUTH_*} and {@code SSTATION_DIRECTORY_*} environment variables.
 *
 * <p>🏫 The login this app ships with is a placeholder. Students and staff are meant to sign in the
 * way they do everywhere else at Austin College — with their AC user name and password — and AC IT
 * owns that. Switching over is configuration, not code: set {@code SSTATION_AUTH_MODE=directory}
 * and point the app at the directory. See "Signing in with AC credentials" in DEPLOY.md.
 *
 * @param mode {@link Mode#LOCAL} (the default) checks passwords stored in this app's database;
 *     {@link Mode#DIRECTORY} checks AC credentials against AC's directory, and still accepts local
 *     accounts so the bootstrap admin and break-glass access keep working.
 * @param passwordHelpUrl 🏫 where "Forgot your password?" sends people in directory mode, since
 *     their password belongs to AC IT, not to this app. Blank shows a plain "contact AC IT" line.
 * @param directory where and how to reach AC's directory. Only read in directory mode.
 * @param signIn how many failed sign-ins a user name gets before it is slowed down (TC-125). Read
 *     in both modes.
 * @param turnstile 🏫 the Cloudflare Turnstile CAPTCHA shown after repeated failures (TC-125).
 *     Without keys, a user name is blocked where it would have been asked for the CAPTCHA.
 */
@ConfigurationProperties(prefix = "sstation.auth")
public record AuthProperties(
    Mode mode,
    String passwordHelpUrl,
    @DefaultValue Directory directory,
    @DefaultValue SignIn signIn,
    @DefaultValue Turnstile turnstile) {

  /**
   * Defaults are applied here rather than with {@code @DefaultValue}, because a variable that is
   * <em>set but empty</em> ({@code SSTATION_AUTH_MODE=} in an env file, or a compose file passing
   * an unset variable through) is not "missing" to Spring, so an annotation default never applies.
   * This repo has been bitten by exactly that once already, with the mail host (TC-115).
   */
  public AuthProperties {
    mode = mode == null ? Mode.LOCAL : mode;
    directory =
        directory == null
            ? new Directory(null, null, null, null, null, null, null, null, null, null)
            : directory;
    signIn = signIn == null ? new SignIn(null, null, null) : signIn;
    turnstile = turnstile == null ? new Turnstile(null, null, null) : turnstile;
  }

  public enum Mode {
    LOCAL,
    DIRECTORY
  }

  public boolean directoryMode() {
    return mode == Mode.DIRECTORY;
  }

  /**
   * 🏫 Every value here comes from AC IT.
   *
   * @param type {@code active-directory} binds as {@code user@domain} and needs no service account
   *     — the usual choice for a Microsoft campus. {@code ldap} searches for the user (optionally
   *     as a service account) and then binds as them; it suits any other LDAP server.
   * @param url e.g. {@code ldaps://dc1.austincollege.edu:636}. Space-separate several for failover.
   * @param domain Active Directory only: the UPN suffix, e.g. {@code austincollege.edu}.
   * @param searchBase where users live, e.g. {@code ou=People,dc=austincollege,dc=edu}. Required
   *     for {@code ldap}; optional for Active Directory, which otherwise derives it from the
   *     domain.
   * @param userSearchFilter {@code ldap} only: how to find a user from what they typed. {@code {0}}
   *     is the user name.
   * @param managerDn {@code ldap} only, optional: a service account to search as, if the directory
   *     does not allow anonymous searches.
   * @param managerPassword the service account's password.
   * @param emailDomain used to find a person's student record when the directory entry has no
   *     {@code mail} attribute: user name {@code jdoe} is looked up as {@code jdoe@<emailDomain>}.
   * @param adminGroup optional: members of this directory group (its full DN, as it appears in a
   *     user's {@code memberOf}) get the ADMIN role while they remain members.
   * @param moderatorGroup optional: the same, for MODERATOR.
   */
  public record Directory(
      String type,
      String url,
      String domain,
      String searchBase,
      String userSearchFilter,
      String managerDn,
      String managerPassword,
      String emailDomain,
      String adminGroup,
      String moderatorGroup) {

    /** Blank means "use the default", for the reason given on {@link AuthProperties}. */
    public Directory {
      type = orDefault(type, "active-directory");
      userSearchFilter = orDefault(userSearchFilter, "(uid={0})");
      emailDomain = orDefault(emailDomain, "austincollege.edu");
    }

    private static String orDefault(String value, String fallback) {
      return value == null || value.isBlank() ? fallback : value.trim();
    }

    public boolean activeDirectory() {
      return "active-directory".equalsIgnoreCase(type);
    }
  }

  /**
   * Failed sign-ins allowed per user name (TC-125). Counting by user name is what keeps repeated
   * guesses away from AC's directory, which locks an account after its own number of failures.
   *
   * <p>🏫 Keep {@code blockAfter} below AC's Active Directory lockout threshold, so this app stops
   * a guesser before AD locks the real student out of every AC system.
   *
   * @param captchaAfter failures before a CAPTCHA is required. Without CAPTCHA keys configured, the
   *     user name is blocked at this point instead.
   * @param blockAfter failures before the user name is blocked even with a solved CAPTCHA.
   * @param window failures older than this stop counting, and a block lasts this long.
   */
  public record SignIn(Integer captchaAfter, Integer blockAfter, Duration window) {

    /** Blank means "use the default", for the reason given on {@link AuthProperties}. */
    public SignIn {
      captchaAfter = captchaAfter == null ? 3 : captchaAfter;
      blockAfter = blockAfter == null ? 5 : blockAfter;
      window = window == null ? Duration.ofMinutes(15) : window;
      if (captchaAfter < 1
          || blockAfter <= captchaAfter
          || window.isNegative()
          || window.isZero()) {
        throw new IllegalStateException(
            "Sign-in limits need SSTATION_SIGNIN_CAPTCHA_AFTER >= 1, SSTATION_SIGNIN_BLOCK_AFTER"
                + " greater than it, and a positive SSTATION_SIGNIN_WINDOW (got "
                + captchaAfter
                + ", "
                + blockAfter
                + ", "
                + window
                + ")");
      }
    }
  }

  /**
   * 🏫 Cloudflare Turnstile keys, created free in a Cloudflare account under Turnstile. The site
   * key is public (it is sent to the browser); the secret key must stay on the server.
   *
   * @param siteKey shown in the sign-in page's CAPTCHA widget.
   * @param secretKey sent, with the solved token, to Cloudflare to confirm the CAPTCHA.
   * @param verifyUrl Cloudflare's verification endpoint. Only tests change it.
   */
  public record Turnstile(String siteKey, String secretKey, String verifyUrl) {

    public Turnstile {
      siteKey = siteKey == null ? "" : siteKey.trim();
      secretKey = secretKey == null ? "" : secretKey.trim();
      verifyUrl =
          verifyUrl == null || verifyUrl.isBlank()
              ? "https://challenges.cloudflare.com/turnstile/v0/siteverify"
              : verifyUrl.trim();
      if (siteKey.isEmpty() != secretKey.isEmpty()) {
        // Half a pair would either show a widget nobody can verify, or verify a widget never shown.
        throw new IllegalStateException(
            "Set both SSTATION_TURNSTILE_SITE_KEY and SSTATION_TURNSTILE_SECRET_KEY, or neither."
                + " See 'Sign-in limits' in DEPLOY.md.");
      }
    }

    public boolean enabled() {
      return !siteKey.isEmpty();
    }

    /** Keeps the secret out of logs and error messages. */
    @Override
    public String toString() {
      return "Turnstile[siteKey=" + siteKey + ", secretKey=" + (enabled() ? "****" : "") + "]";
    }
  }
}
