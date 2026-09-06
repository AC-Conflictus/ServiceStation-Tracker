package edu.austincollege.sstation.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.austincollege.sstation.domain.User;
import edu.austincollege.sstation.repository.UserRepository;
import edu.austincollege.sstation.service.PasswordChangeService.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Coverage for {@link PasswordChangeService} + the V5 must_change_password column (TC-121). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PasswordChangeServiceTest {

  private static final String CURRENT = "bootstrap_password";

  @Autowired private UserRepository users;

  private final PasswordEncoder encoder = new BCryptPasswordEncoder();
  private PasswordChangeService service;

  @BeforeEach
  void setUp() {
    service = new PasswordChangeService(users, encoder);

    User admin = new User("admin", encoder.encode(CURRENT));
    admin.setMustChangePassword(true);
    users.save(admin);
  }

  @Test
  void changingPasswordStoresTheNewHashAndClearsTheFlag() {
    Result result = service.change("admin", CURRENT, "a_password_of_my_own");

    assertThat(result).isEqualTo(Result.OK);
    User reloaded = users.findByUsername("admin").orElseThrow();
    assertThat(encoder.matches("a_password_of_my_own", reloaded.getPassword())).isTrue();
    assertThat(reloaded.isMustChangePassword()).isFalse();
  }

  @Test
  void wrongCurrentPasswordChangesNothing() {
    Result result = service.change("admin", "not_the_password", "a_password_of_my_own");

    assertThat(result).isEqualTo(Result.WRONG_CURRENT_PASSWORD);
    User reloaded = users.findByUsername("admin").orElseThrow();
    assertThat(encoder.matches(CURRENT, reloaded.getPassword())).isTrue();
    assertThat(reloaded.isMustChangePassword()).isTrue();
  }

  @Test
  void reusingTheCurrentPasswordIsRefused() {
    // The whole point of the forced change is retiring the value that came from an environment
    // variable. Letting it be re-entered would satisfy the prompt and keep the credential live.
    Result result = service.change("admin", CURRENT, CURRENT);

    assertThat(result).isEqualTo(Result.SAME_AS_CURRENT);
    assertThat(users.findByUsername("admin").orElseThrow().isMustChangePassword()).isTrue();
  }

  @Test
  void aPasswordShorterThanThePolicyIsRefused() {
    Result result = service.change("admin", CURRENT, "short");

    assertThat(result).isEqualTo(Result.TOO_WEAK);
    assertThat(service.weaknessMessage("short"))
        .contains(String.valueOf(PasswordPolicy.MIN_LENGTH));
    assertThat(users.findByUsername("admin").orElseThrow().isMustChangePassword()).isTrue();
  }
}
