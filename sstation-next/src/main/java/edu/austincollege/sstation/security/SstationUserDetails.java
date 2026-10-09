package edu.austincollege.sstation.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * The authenticated principal, carrying the one piece of account state the request path needs
 * beyond Spring Security's own flags (TC-121).
 *
 * <p>Holding {@code mustChangePassword} on the principal is what keeps {@link
 * MustChangePasswordFilter} free of a database round trip on every single request. It does mean the
 * value is a snapshot taken at login — which is exactly why a successful change ends the session
 * and sends the user back through the sign-in form rather than trying to mutate the principal in
 * place.
 */
public class SstationUserDetails extends User {

  private final boolean mustChangePassword;

  /** TC-124: signed in with AC credentials, so the password is not ours to change or reset. */
  private final boolean directoryAccount;

  public SstationUserDetails(
      String username,
      String password,
      boolean enabled,
      boolean accountNonExpired,
      boolean credentialsNonExpired,
      boolean accountNonLocked,
      Collection<? extends GrantedAuthority> authorities,
      boolean mustChangePassword) {
    super(
        username,
        password,
        enabled,
        accountNonExpired,
        credentialsNonExpired,
        accountNonLocked,
        authorities);
    this.mustChangePassword = mustChangePassword;
    this.directoryAccount = false;
  }

  /**
   * A principal for someone who signed in with AC credentials (TC-124). The password field is left
   * empty: it was checked by the directory and is not held here.
   */
  public static SstationUserDetails forDirectoryAccount(
      String username, Collection<? extends GrantedAuthority> authorities) {
    return new SstationUserDetails(username, authorities);
  }

  private SstationUserDetails(String username, Collection<? extends GrantedAuthority> authorities) {
    super(username, "", true, true, true, true, authorities);
    this.mustChangePassword = false;
    this.directoryAccount = true;
  }

  public boolean isMustChangePassword() {
    return mustChangePassword;
  }

  public boolean isDirectoryAccount() {
    return directoryAccount;
  }
}
