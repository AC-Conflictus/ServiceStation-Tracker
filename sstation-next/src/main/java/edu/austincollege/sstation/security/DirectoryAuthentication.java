package edu.austincollege.sstation.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.NullLdapAuthoritiesPopulator;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;

/**
 * Builds the provider that checks AC credentials against AC's directory (TC-124).
 *
 * <p>Both shapes hand the authenticated directory entry to {@link DirectoryAccountMapper}, which is
 * where "who is this person in Service Station" is decided. Neither asks the directory for roles —
 * the Active Directory provider's own group-to-authority mapping is ignored on purpose, because a
 * person's AD groups are not Service Station roles unless AC IT names one in the config.
 */
final class DirectoryAuthentication {

  /**
   * Attributes the mapper reads. {@code memberOf} is operational on some servers, so ask for it.
   */
  private static final String[] USER_ATTRIBUTES = {"*", "memberOf"};

  private DirectoryAuthentication() {}

  /**
   * Fails at startup, with the variable to set, rather than at the first sign-in attempt — a
   * misconfigured directory would otherwise look exactly like "everyone's password is wrong".
   */
  static void validate(AuthProperties.Directory d) {
    List<String> missing = new ArrayList<>();
    if (blank(d.url())) {
      missing.add("SSTATION_DIRECTORY_URL");
    }
    if (d.activeDirectory()) {
      if (blank(d.domain())) {
        missing.add("SSTATION_DIRECTORY_DOMAIN");
      }
    } else if ("ldap".equalsIgnoreCase(d.type())) {
      if (blank(d.searchBase())) {
        missing.add("SSTATION_DIRECTORY_SEARCH_BASE");
      }
    } else {
      throw new IllegalStateException(
          "SSTATION_DIRECTORY_TYPE must be 'active-directory' or 'ldap', not '" + d.type() + "'");
    }
    if (!missing.isEmpty()) {
      throw new IllegalStateException(
          "SSTATION_AUTH_MODE=directory needs "
              + String.join(", ", missing)
              + ". See 'Signing in with AC credentials' in DEPLOY.md.");
    }
  }

  static AuthenticationProvider provider(
      AuthProperties.Directory d, DirectoryAccountMapper mapper) {
    validate(d);
    AuthenticationProvider directory =
        d.activeDirectory() ? activeDirectory(d, mapper) : ldap(d, mapper);
    return new NormalizingProvider(directory, mapper);
  }

  /**
   * Hands the directory the user name as {@link DirectoryAccountMapper#normalize} would store it,
   * so {@code " JDoe@AustinCollege.edu "} finds the same directory entry — and the same account —
   * as {@code jdoe}. Without this an LDAP search filter like {@code (uid={0})} sees the raw text.
   */
  private record NormalizingProvider(AuthenticationProvider delegate, DirectoryAccountMapper mapper)
      implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication typed) {
      UsernamePasswordAuthenticationToken normalized =
          UsernamePasswordAuthenticationToken.unauthenticated(
              mapper.normalize(typed.getName()), typed.getCredentials());
      normalized.setDetails(typed.getDetails());
      return delegate.authenticate(normalized);
    }

    @Override
    public boolean supports(Class<?> authentication) {
      return delegate.supports(authentication);
    }
  }

  /** Binds as {@code user@domain}; no service account needed. */
  private static AuthenticationProvider activeDirectory(
      AuthProperties.Directory d, DirectoryAccountMapper mapper) {
    ActiveDirectoryLdapAuthenticationProvider provider =
        new ActiveDirectoryLdapAuthenticationProvider(
            d.domain(), d.url(), blank(d.searchBase()) ? null : d.searchBase());
    // Lets AD's own reasons (account disabled, expired, locked) come back as the matching
    // Spring Security exceptions instead of a generic bad-credentials failure.
    provider.setConvertSubErrorCodesToExceptions(true);
    provider.setUserDetailsContextMapper(mapper);
    return provider;
  }

  /** Finds the user's entry (as the service account, if one is set), then binds as them. */
  private static AuthenticationProvider ldap(
      AuthProperties.Directory d, DirectoryAccountMapper mapper) {
    DefaultSpringSecurityContextSource contextSource =
        new DefaultSpringSecurityContextSource(d.url());
    if (!blank(d.managerDn())) {
      contextSource.setUserDn(d.managerDn());
      contextSource.setPassword(d.managerPassword() == null ? "" : d.managerPassword());
    }
    contextSource.afterPropertiesSet();

    FilterBasedLdapUserSearch search =
        new FilterBasedLdapUserSearch(d.searchBase(), d.userSearchFilter(), contextSource);
    search.setReturningAttributes(USER_ATTRIBUTES);

    BindAuthenticator authenticator = new BindAuthenticator(contextSource);
    authenticator.setUserSearch(search);
    authenticator.setUserAttributes(USER_ATTRIBUTES);

    LdapAuthenticationProvider provider =
        new LdapAuthenticationProvider(authenticator, new NullLdapAuthoritiesPopulator());
    provider.setUserDetailsContextMapper(mapper);
    return provider;
  }

  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }
}
