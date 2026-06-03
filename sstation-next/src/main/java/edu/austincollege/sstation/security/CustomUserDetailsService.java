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

    List<SimpleGrantedAuthority> authorities =
        userRoles.findAuthoritiesByUser(user).stream().map(SimpleGrantedAuthority::new).toList();

    // The authorities already carry the ROLE_ prefix (e.g. ROLE_ADMIN), so hasRole('ADMIN')
    // resolves correctly without further mangling.
    return org.springframework.security.core.userdetails.User.builder()
        .username(user.getUsername())
        .password(user.getPassword())
        .authorities(authorities)
        .disabled(!user.isEnabled())
        .accountExpired(user.isAccountExpired())
        .accountLocked(user.isAccountLocked())
        .credentialsExpired(user.isPasswordExpired())
        .build();
  }
}
