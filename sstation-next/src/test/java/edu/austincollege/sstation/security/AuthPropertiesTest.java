package edu.austincollege.sstation.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * TC-124: a variable that is set but empty must mean "use the default", not "use nothing". Spring
 * treats {@code SSTATION_DIRECTORY_TYPE=} as present, so an annotation default never applies — and
 * an env file or compose file that passes unset variables through produces exactly that.
 */
class AuthPropertiesTest {

  private static AuthProperties bind(Map<String, String> values) {
    return new Binder(new MapConfigurationPropertySource(values))
        .bindOrCreate("sstation.auth", AuthProperties.class);
  }

  @Test
  void nothingSetMeansLocalSignInWithTheDocumentedDirectoryDefaults() {
    AuthProperties auth = bind(Map.of());

    assertThat(auth.directoryMode()).isFalse();
    assertThat(auth.directory().activeDirectory()).isTrue();
    assertThat(auth.directory().userSearchFilter()).isEqualTo("(uid={0})");
    assertThat(auth.directory().emailDomain()).isEqualTo("austincollege.edu");
  }

  @Test
  void setButEmptyValuesFallBackToTheSameDefaults() {
    AuthProperties auth =
        bind(
            Map.of(
                "sstation.auth.mode", "",
                "sstation.auth.directory.type", "",
                "sstation.auth.directory.user-search-filter", " ",
                "sstation.auth.directory.email-domain", ""));

    assertThat(auth.directoryMode()).isFalse();
    assertThat(auth.directory().activeDirectory()).isTrue();
    assertThat(auth.directory().userSearchFilter()).isEqualTo("(uid={0})");
    assertThat(auth.directory().emailDomain()).isEqualTo("austincollege.edu");
  }

  @Test
  void theModeIsCaseInsensitiveLikeEveryOtherSwitch() {
    assertThat(bind(Map.of("sstation.auth.mode", "directory")).directoryMode()).isTrue();
    assertThat(bind(Map.of("sstation.auth.mode", "DIRECTORY")).directoryMode()).isTrue();
  }
}
