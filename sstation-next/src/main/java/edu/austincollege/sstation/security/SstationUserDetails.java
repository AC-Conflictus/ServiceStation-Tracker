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
  }

  public boolean isMustChangePassword() {
    return mustChangePassword;
  }
}
