package edu.austincollege.sstation.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * How people sign in (TC-124). Bound from {@code sstation.auth.*}, which {@code application.yml}
 * maps to the {@code SSTATION_AUTH_*} environment variables.
 *
 * <p>🏫 The login this app ships with is a placeholder. Students and staff are meant to sign in the
 * way they do everywhere else at Austin College — with their AC user name and password — and AC IT
 * owns that. Switching over is configuration, not code: set {@code SSTATION_AUTH_MODE=directory}
 * and point the app at the directory. See "Signing in with AC credentials" in DEPLOY.md.
 *
 * @param mode {@link Mode#LOCAL} (the default) checks passwords stored in this app's database;
 *     {@link Mode#DIRECTORY} checks AC credentials against AC's directory.
 * @param passwordHelpUrl 🏫 where "Forgot your password?" sends people in directory mode, since
 *     their password belongs to AC IT, not to this app. Blank shows a plain "contact AC IT" line.
 */
@ConfigurationProperties(prefix = "sstation.auth")
public record AuthProperties(@DefaultValue("local") Mode mode, String passwordHelpUrl) {

  public enum Mode {
    LOCAL,
    DIRECTORY
  }

  public boolean directory() {
    return mode == Mode.DIRECTORY;
  }
}
