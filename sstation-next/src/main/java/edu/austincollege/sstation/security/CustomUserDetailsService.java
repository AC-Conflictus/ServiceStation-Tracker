package edu.austincollege.sstation.security;

import edu.austincollege.sstation.domain.User;
import edu.austincollege.sstation.repository.UserRepository;
import edu.austincollege.sstation.repository.UserRoleRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a {@link User} and its granted authorities from the database for Spring Security. Replaces
 * the Grails Spring Security plugin's {@code GormUserDetailsService}.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository users;
  private final UserRoleRepository userRoles;

  public CustomUserDetailsService(UserRepository users, UserRoleRepository userRoles) {
    this.users = users;
    this.userRoles = userRoles;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user =
        users
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("No user: " + username));

    // TC-124: an AC account's password lives in AC's directory. Treating it as unknown here (not
    // as a wrong password) means the local check can never open it — whatever value happens to be
    // in its password column — and the sign-in moves on to the directory.
    if (user.isDirectoryAccount()) {
      throw new UsernameNotFoundException("Directory account, not checked locally: " + username);
    }

    List<SimpleGrantedAuthority> authorities =
        userRoles.findAuthoritiesByUser(user).stream().map(SimpleGrantedAuthority::new).toList();

    // The authorities already carry the ROLE_ prefix (e.g. ROLE_ADMIN), so hasRole('ADMIN')
    // resolves correctly without further mangling.
    //
    // The flag arguments are inverted ("non-expired"), so each is negated exactly once here.
    return new SstationUserDetails(
        user.getUsername(),
        user.getPassword(),
        user.isEnabled(),
        !user.isAccountExpired(),
        !user.isPasswordExpired(),
        !user.isAccountLocked(),
        authorities,
        user.isMustChangePassword());
  }
}
