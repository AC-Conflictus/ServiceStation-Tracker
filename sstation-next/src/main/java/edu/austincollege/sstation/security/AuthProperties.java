package edu.austincollege.sstation.security;

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
 */
@ConfigurationProperties(prefix = "sstation.auth")
public record AuthProperties(
    @DefaultValue("local") Mode mode, String passwordHelpUrl, @DefaultValue Directory directory) {

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
      @DefaultValue("active-directory") String type,
      String url,
      String domain,
      String searchBase,
      @DefaultValue("(uid={0})") String userSearchFilter,
      String managerDn,
      String managerPassword,
      @DefaultValue("austincollege.edu") String emailDomain,
      String adminGroup,
      String moderatorGroup) {

    public boolean activeDirectory() {
      return "active-directory".equalsIgnoreCase(type);
    }
  }
}
