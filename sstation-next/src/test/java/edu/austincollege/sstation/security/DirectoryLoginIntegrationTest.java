package edu.austincollege.sstation.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import edu.austincollege.sstation.domain.AuthSource;
import edu.austincollege.sstation.domain.Classification;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.domain.User;
import edu.austincollege.sstation.repository.PasswordResetTokenRepository;
import edu.austincollege.sstation.repository.StudentRepository;
import edu.austincollege.sstation.repository.UserRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * TC-124: signing in with AC credentials, against a real (in-memory) LDAP server standing in for
 * AC's directory, through the real filter chain.
 *
 * <p>Runs under {@code dev} on purpose: the seeded local {@code admin} and {@code student} accounts
 * are what prove that local and directory accounts coexist — and that one can never become the
 * other.
 */
class DirectoryLoginIntegrationTest {

  private static final String BASE = "dc=ac,dc=test";
  private static final String ADMIN_GROUP = "cn=sstation admins,ou=groups," + BASE;

  /** Stands in for AC's directory. Schema checking is off so entries can carry memberOf as-is. */
  static final InMemoryDirectoryServer directory = startDirectory();

  private static InMemoryDirectoryServer startDirectory() {
    try {
      InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(BASE);
      config.setSchema(null);
      config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("ldap", 0));
      InMemoryDirectoryServer server = new InMemoryDirectoryServer(config);
      server.add("dn: " + BASE, "objectClass: top", "objectClass: domain", "dc: ac");
      server.add("dn: ou=people," + BASE, "objectClass: organizationalUnit", "ou: people");
      // A student whose directory email differs in case from the registrar's.
      person(server, "ada", "ada-ac-pass", "mail: ADA.Lovelace@AustinCollege.edu");
      // Staff: no student record, but in the admin group — spelled with different case and
      // spacing than the configured DN, as real directories return it.
      person(
          server, "pat", "pat-ac-pass", "memberOf: CN=SStation Admins, OU=Groups, DC=ac,DC=test");
      // A real AC account that Service Station knows nothing about.
      person(server, "newbie", "newbie-ac-pass");
      // No mail attribute: the student record is found as <username>@austincollege.edu.
      person(server, "zed", "zed-ac-pass");
      // A student an admin has promoted on the Moderators page.
      person(server, "mo", "mo-ac-pass", "mail: mo@austincollege.edu");
      // Same user name as the seeded *local* account "student".
      person(server, "student", "student-ac-pass", "mail: someone-else@austincollege.edu");
      server.startListening();
      return server;
    } catch (Exception e) {
      throw new IllegalStateException("Could not start the in-memory directory", e);
    }
  }

  private static void person(
      InMemoryDirectoryServer server, String uid, String password, String... extra)
      throws Exception {
    String[] base = {
      "dn: uid=" + uid + ",ou=people," + BASE,
      "objectClass: inetOrgPerson",
      "uid: " + uid,
      "cn: " + uid,
      "sn: " + uid,
      "userPassword: " + password
    };
    String[] all = new String[base.length + extra.length];
    System.arraycopy(base, 0, all, 0, base.length);
    System.arraycopy(extra, 0, all, base.length, extra.length);
    server.add(all);
  }

  @AfterAll
  static void stopDirectory() {
    directory.shutDown(true);
  }

  @Nested
  @SpringBootTest
  @AutoConfigureMockMvc
  @ActiveProfiles("dev")
  class WithDirectoryReachable {

    @DynamicPropertySource
    static void directoryMode(DynamicPropertyRegistry props) {
      props.add("sstation.auth.mode", () -> "directory");
      props.add("sstation.auth.directory.type", () -> "ldap");
      props.add(
          "sstation.auth.directory.url",
          () -> "ldap://localhost:" + directory.getListenPort() + "/" + BASE);
      props.add("sstation.auth.directory.search-base", () -> "ou=people");
      props.add("sstation.auth.directory.user-search-filter", () -> "(uid={0})");
      props.add("sstation.auth.directory.admin-group", () -> ADMIN_GROUP);
    }

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private StudentRepository students;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PasswordResetTokenRepository resetTokens;

    @BeforeEach
    void studentRecords() {
      ensureStudent("AC70001", "Ada", "ada.lovelace@austincollege.edu", false);
      ensureStudent("AC70002", "Zed", "zed@austincollege.edu", false);
      ensureStudent("AC70003", "Mo", "mo@austincollege.edu", true);
    }

    @Test
    void aStudentsFirstAcSignInCreatesTheirAccountAndLinksTheirRecord() throws Exception {
      MockHttpSession session =
          (MockHttpSession)
              mvc.perform(formLogin("/login").user("ada").password("ada-ac-pass"))
                  .andExpect(authenticated().withUsername("ada").withRoles("STUDENT"))
                  .andExpect(redirectedUrl("/"))
                  .andReturn()
                  .getRequest()
                  .getSession();

      User ada = users.findByUsername("ada").orElseThrow();
      assertThat(ada.getAuthSource()).isEqualTo(AuthSource.DIRECTORY);
      assertThat(users.findStudentByUsername("ada").orElseThrow().getAcid()).isEqualTo("AC70001");

      // The dashboard reads the student through the FK, not by matching email strings.
      mvc.perform(get("/student").session(session))
          .andExpect(status().isOk())
          .andExpect(content().string(org.hamcrest.Matchers.containsString("Ada")));
    }

    @Test
    void signingInAgainReusesTheAccountHoweverTheNameIsTyped() throws Exception {
      mvc.perform(formLogin("/login").user("ada").password("ada-ac-pass"))
          .andExpect(authenticated());
      long accounts = users.count();

      mvc.perform(formLogin("/login").user("  ADA@AustinCollege.edu ").password("ada-ac-pass"))
          .andExpect(authenticated().withUsername("ada"));

      assertThat(users.count()).as("no second account for the same person").isEqualTo(accounts);
    }

    @Test
    void aWrongAcPasswordIsAnOrdinaryFailedSignIn() throws Exception {
      mvc.perform(formLogin("/login").user("ada").password("not-it"))
          .andExpect(unauthenticated())
          .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void theStudentRecordIsFoundByUserNameWhenTheDirectoryHasNoEmail() throws Exception {
      mvc.perform(formLogin("/login").user("zed").password("zed-ac-pass"))
          .andExpect(authenticated().withUsername("zed").withRoles("STUDENT"));
    }

    @Test
    void membersOfTheAdminGroupAreAdminsWithoutAnyStudentRecord() throws Exception {
      mvc.perform(formLogin("/login").user("pat").password("pat-ac-pass"))
          .andExpect(authenticated().withUsername("pat").withRoles("ADMIN"));
    }

    @Test
    void aStudentPromotedOnTheModeratorsPageIsAModerator() throws Exception {
      mvc.perform(formLogin("/login").user("mo").password("mo-ac-pass"))
          .andExpect(authenticated().withUsername("mo").withRoles("STUDENT", "MODERATOR"));
    }

    @Test
    void anAcAccountServiceStationDoesNotKnowIsRefusedAndNotCreated() throws Exception {
      mvc.perform(formLogin("/login").user("newbie").password("newbie-ac-pass"))
          .andExpect(unauthenticated())
          .andExpect(redirectedUrl("/login?notRegistered"));

      assertThat(users.findByUsername("newbie")).isEmpty();
    }

    @Test
    void anAcSignInNeverTakesOverALocalAccountWithTheSameName() throws Exception {
      String before = users.findByUsername("student").orElseThrow().getPassword();

      mvc.perform(formLogin("/login").user("student").password("student-ac-pass"))
          .andExpect(unauthenticated())
          .andExpect(redirectedUrl("/login?conflict"));

      User local = users.findByUsername("student").orElseThrow();
      assertThat(local.getAuthSource()).isEqualTo(AuthSource.LOCAL);
      assertThat(local.getPassword()).isEqualTo(before);
      // ...and the local account still works with its own password.
      mvc.perform(formLogin("/login").user("student").password("student_secret"))
          .andExpect(authenticated().withUsername("student"));
    }

    @Test
    void localAccountsStillSignInForBreakGlassAccess() throws Exception {
      mvc.perform(formLogin("/login").user("admin").password("admin_secret"))
          .andExpect(authenticated().withUsername("admin").withRoles("ADMIN"));
    }

    @Test
    void aDirectoryAccountCanNeverBeOpenedWithALocalPassword() throws Exception {
      mvc.perform(formLogin("/login").user("ada").password("ada-ac-pass"))
          .andExpect(authenticated());
      // Even if someone writes a known hash into the column, the local check must refuse.
      User ada = users.findByUsername("ada").orElseThrow();
      ada.setPassword(passwordEncoder.encode("planted-local-password"));
      users.save(ada);

      mvc.perform(formLogin("/login").user("ada").password("planted-local-password"))
          .andExpect(unauthenticated())
          .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void passwordResetIsANoOpForAnAcAccount() throws Exception {
      mvc.perform(formLogin("/login").user("ada").password("ada-ac-pass"))
          .andExpect(authenticated());
      long before = resetTokens.count();

      mvc.perform(
              post("/forgot-password")
                  .param("email", "ada.lovelace@austincollege.edu")
                  .with(csrf()))
          .andExpect(status().is3xxRedirection());

      assertThat(resetTokens.count()).as("no reset token for an AC account").isEqualTo(before);
    }

    @Test
    void anAcAccountIsToldItsPasswordBelongsToAcIt() throws Exception {
      MockHttpSession session =
          (MockHttpSession)
              mvc.perform(formLogin("/login").user("ada").password("ada-ac-pass"))
                  .andReturn()
                  .getRequest()
                  .getSession();

      mvc.perform(get("/change-password").session(session))
          .andExpect(status().isOk())
          .andExpect(
              content()
                  .string(org.hamcrest.Matchers.containsString("managed by Austin College IT")))
          .andExpect(
              content()
                  .string(
                      org.hamcrest.Matchers.not(
                          org.hamcrest.Matchers.containsString("name=\"newPassword\""))));
    }

    @Test
    void healthStaysUpInDirectoryMode() throws Exception {
      // Boot's own LDAP health check would probe localhost:389 and report DOWN; it is excluded.
      mvc.perform(get("/actuator/health"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("UP"));
    }

    private void ensureStudent(String acid, String first, String email, boolean moderator) {
      if (students.findByAcid(acid).isPresent()) {
        return;
      }
      Student s = new Student();
      s.setAcid(acid);
      s.setFirstname(first);
      s.setLastname("Tester");
      s.setAcEmail(email);
      s.setStatus('A');
      s.setClassification(Classification.JR);
      s.setIsModerator(moderator);
      students.save(s);
    }
  }

  @Nested
  @SpringBootTest
  @AutoConfigureMockMvc
  @ActiveProfiles("dev")
  class WithDirectoryDown {

    @DynamicPropertySource
    static void unreachableDirectory(DynamicPropertyRegistry props) {
      props.add("sstation.auth.mode", () -> "directory");
      props.add("sstation.auth.directory.type", () -> "ldap");
      // Nothing listens on port 1.
      props.add("sstation.auth.directory.url", () -> "ldap://localhost:1/" + BASE);
      props.add("sstation.auth.directory.search-base", () -> "ou=people");
    }

    @Autowired private MockMvc mvc;

    @Test
    void anAcUserIsToldSignInIsUnavailableNotThatTheirPasswordIsWrong() throws Exception {
      mvc.perform(formLogin("/login").user("ada").password("ada-ac-pass"))
          .andExpect(unauthenticated())
          .andExpect(redirectedUrl("/login?unavailable"));
    }

    @Test
    void localAdminsCanStillSignInDuringADirectoryOutage() throws Exception {
      mvc.perform(formLogin("/login").user("admin").password("admin_secret"))
          .andExpect(authenticated().withUsername("admin").withRoles("ADMIN"));
    }
  }
}
