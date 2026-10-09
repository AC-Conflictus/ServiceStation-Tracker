package edu.austincollege.sstation.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

/**
 * TC-124: a half-configured directory has to fail at startup, naming the variable to set. Found at
 * the first sign-in instead, it would look exactly like "everybody's password is wrong".
 */
class DirectoryAuthenticationTest {

  private static AuthProperties.Directory directory(
      String type, String url, String domain, String searchBase) {
    return new AuthProperties.Directory(
        type, url, domain, searchBase, "(uid={0})", null, null, "austincollege.edu", null, null);
  }

  @Test
  void activeDirectoryNeedsAUrlAndADomain() {
    assertThatIllegalStateException()
        .isThrownBy(
            () -> DirectoryAuthentication.validate(directory("active-directory", "", "", "")))
        .withMessageContaining("SSTATION_DIRECTORY_URL")
        .withMessageContaining("SSTATION_DIRECTORY_DOMAIN")
        .withMessageContaining("DEPLOY.md");
  }

  @Test
  void genericLdapNeedsASearchBase() {
    assertThatIllegalStateException()
        .isThrownBy(
            () -> DirectoryAuthentication.validate(directory("ldap", "ldap://x", null, null)))
        .withMessageContaining("SSTATION_DIRECTORY_SEARCH_BASE")
        .withMessageNotContaining("SSTATION_DIRECTORY_URL");
  }

  @Test
  void anUnknownTypeIsNamedRatherThanSilentlyTreatedAsLdap() {
    assertThatIllegalStateException()
        .isThrownBy(() -> DirectoryAuthentication.validate(directory("okta", "ldap://x", "d", "b")))
        .withMessageContaining("'okta'");
  }

  @Test
  void aCompleteActiveDirectoryConfigBuildsWithoutContactingTheServer() {
    // The provider connects lazily, at the first sign-in — so the app still starts while AC's
    // directory is briefly unreachable, and local accounts keep working (see the integration test).
    AuthProperties.Directory ad =
        directory("active-directory", "ldaps://dc.example.edu:636", "example.edu", null);

    AuthenticationProvider provider =
        DirectoryAuthentication.provider(ad, mock(DirectoryAccountMapper.class));

    assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isTrue();
  }
}
