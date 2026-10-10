package edu.austincollege.sstation.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
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

  @Test
  void signInLimitsDefaultToCaptchaAtThreeBlockAtFiveForFifteenMinutes() {
    // TC-125. Empty values come from compose passing unset variables through.
    for (AuthProperties auth :
        new AuthProperties[] {
          bind(Map.of()),
          bind(
              Map.of(
                  "sstation.auth.sign-in.captcha-after", "",
                  "sstation.auth.sign-in.block-after", "",
                  "sstation.auth.sign-in.window", ""))
        }) {
      assertThat(auth.signIn().captchaAfter()).isEqualTo(3);
      assertThat(auth.signIn().blockAfter()).isEqualTo(5);
      assertThat(auth.signIn().window()).isEqualTo(Duration.ofMinutes(15));
    }
  }

  @Test
  void signInLimitsThatCouldNeverBlockFailAtStartup() {
    // Blocking at or before the CAPTCHA would make the CAPTCHA unreachable; a zero window would
    // make every block end the moment it starts.
    assertThatThrownBy(
            () ->
                bind(
                    Map.of(
                        "sstation.auth.sign-in.captcha-after", "5",
                        "sstation.auth.sign-in.block-after", "5")))
        .hasRootCauseInstanceOf(IllegalStateException.class)
        .rootCause()
        .hasMessageContaining("SSTATION_SIGNIN_BLOCK_AFTER");
    assertThatThrownBy(() -> bind(Map.of("sstation.auth.sign-in.window", "0s")))
        .rootCause()
        .hasMessageContaining("SSTATION_SIGNIN_WINDOW");
  }

  @Test
  void turnstileIsOffWithoutKeysAndRefusesHalfAPair() {
    assertThat(bind(Map.of()).turnstile().enabled()).isFalse();
    assertThat(
            bind(Map.of(
                    "sstation.auth.turnstile.site-key", "",
                    "sstation.auth.turnstile.secret-key", ""))
                .turnstile()
                .enabled())
        .isFalse();
    assertThatThrownBy(() -> bind(Map.of("sstation.auth.turnstile.site-key", "0x4AAA")))
        .rootCause()
        .hasMessageContaining("SSTATION_TURNSTILE_SECRET_KEY");
  }

  @Test
  void theTurnstileSecretNeverAppearsInItsToString() {
    AuthProperties.Turnstile turnstile =
        new AuthProperties.Turnstile("site-key", "very-secret", null);

    assertThat(turnstile.toString()).contains("site-key").doesNotContain("very-secret");
  }
}
