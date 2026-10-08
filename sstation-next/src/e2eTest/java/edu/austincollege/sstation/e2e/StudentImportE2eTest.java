package edu.austincollege.sstation.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.options.FilePayload;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TC-123: the student CSV import, driven through a real file picker.
 *
 * <p>Closes the loop on TC-111 — the parity checklist is what found this feature missing, so the
 * suite that produced the checklist is where the fix gets its browser coverage. Multipart uploads
 * are also the one thing MockMvc's {@code multipart()} builder simulates rather than performs: this
 * goes over a real socket with a real encoding and a real CSRF token.
 */
@DisplayName("E2E: student CSV import")
class StudentImportE2eTest extends E2eTest {

  private static final String HEADER =
      "acid,ignored,firstname,lastname,status,acbox,classification,ignored2,email\n";

  /** Unique ids per run so re-running against a live stack updates rather than colliding. */
  private String uniqueAcid() {
    return "AC" + (System.currentTimeMillis() % 100000);
  }

  private void upload(String csv) {
    page.setInputFiles(
        "input[type='file']",
        new FilePayload("students.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)));
    submit();
  }

  @Test
  @DisplayName("an admin can import students and the new rows appear in the list")
  void adminCanImportStudents() {
    String acid = uniqueAcid();

    signInAsAdmin();
    page.navigate(url("/admin/students/import"));
    upload(HEADER + acid + ",x,Imported,Viabrowser,A,900,JR,y,imported@austincollege.edu\n");

    assertThat(page.textContent("body")).contains("Import complete");
    assertThat(page.textContent("body")).contains("Every row in the file imported cleanly");

    // The real proof: the student is actually on the roster afterwards.
    page.navigate(url("/admin/students"));
    assertThat(page.textContent("body")).contains("Viabrowser");
  }

  @Test
  @DisplayName("bad rows are listed with a reason instead of vanishing")
  void badRowsAreReported() {
    String good = uniqueAcid();

    signInAsAdmin();
    page.navigate(url("/admin/students/import"));
    upload(
        HEADER
            + good
            + ",x,Good,Row,A,1,FR,y,goodrow@austincollege.edu\n"
            + "AC99999,x,Bad,Email,A,2,FR,y,definitely-not-an-email\n");

    String body = page.textContent("body");
    assertThat(body).contains("Import complete");
    // The whole point of the improvement over Grails, which dropped these in silence.
    assertThat(body).contains("These rows were not imported");
    assertThat(body).contains("AC99999");
    // The good row still landed.
    assertThat(body).doesNotContain("Every row in the file imported cleanly");
  }

  @Test
  @DisplayName("re-importing the same file updates rather than duplicating")
  void reImportingUpdates() {
    String acid = uniqueAcid();
    String csv = HEADER + acid + ",x,Repeat,Import,A,5,SO,y,repeat@austincollege.edu\n";

    signInAsAdmin();
    page.navigate(url("/admin/students/import"));
    upload(csv);
    assertThat(page.textContent("body")).contains("Import complete");

    page.navigate(url("/admin/students/import"));
    upload(csv);

    // Read the counts off the cards, not the page text. "Updated" is a permanent label on the
    // result page, so asserting the page merely contains the word proves nothing at all.
    assertThat(page.locator(".card:has-text('Updated') .fs-4").textContent().trim())
        .as("the second import must update the existing student")
        .isEqualTo("1");
    assertThat(page.locator(".card:has-text('Added') .fs-4").textContent().trim())
        .as("and must not add a duplicate")
        .isEqualTo("0");
  }

  @Test
  @DisplayName("a student cannot reach the import page")
  void studentsAreDenied() {
    // Grails allowed this; deliberately not ported. Asserted in a browser as well as in the
    // controller test, because this is the kind of rule that gets loosened by accident.
    signInAsStudent();
    page.navigate(url("/admin/students/import"));

    assertThat(page.textContent("body")).contains("You don't have access to that page");
  }

  @Test
  @DisplayName("the import link is reachable from the students list")
  void importIsDiscoverableFromTheStudentsList() {
    // A feature nobody can find is not ported. The office has to be able to get here.
    signInAsAdmin();
    page.navigate(url("/admin/students"));

    page.locator("a:has-text('Import CSV')").click();
    page.waitForLoadState();

    assertThat(page.url()).endsWith("/admin/students/import");
  }
}
