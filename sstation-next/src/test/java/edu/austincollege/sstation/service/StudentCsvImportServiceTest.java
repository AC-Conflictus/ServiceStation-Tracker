package edu.austincollege.sstation.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.austincollege.sstation.domain.Classification;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.repository.StudentRepository;
import edu.austincollege.sstation.service.StudentCsvImportService.ImportResult;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * TC-123: the student CSV import the TC-111 parity checklist found missing.
 *
 * <p>The column layout is the part worth pinning hardest. The file AC hands the office is a
 * registrar export with a fixed shape, and columns 1 and 7 are deliberately unread — get the
 * offsets wrong and the import silently fills names with box numbers rather than failing.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StudentCsvImportServiceTest {

  /** Header + one row, in the Grails column order. Columns 1 and 7 are junk on purpose. */
  private static final String HEADER =
      "acid,ignored,firstname,lastname,status,acbox,classification,ignored2,email\n";

  @Autowired private StudentRepository students;

  private StudentCsvImportService service;

  @BeforeEach
  void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    service = new StudentCsvImportService(students, validator);
  }

  private ImportResult importCsv(String csv) throws IOException {
    return service.importFrom(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void addsANewStudentAndMapsEveryColumnToTheRightField() throws IOException {
    ImportResult result =
        importCsv(
            HEADER + "AC91001,JUNK,Ada,Lovelace,A,31337,JR,MORE JUNK,ada@austincollege.edu\n");

    assertThat(result.added()).isEqualTo(1);
    assertThat(result.updated()).isZero();
    assertThat(result.skipped()).isEmpty();

    Student saved = students.findByAcid("AC91001").orElseThrow();
    assertThat(saved.getFirstname()).isEqualTo("Ada");
    assertThat(saved.getLastname()).isEqualTo("Lovelace");
    assertThat(saved.getStatus()).isEqualTo('A');
    assertThat(saved.getAcBox()).isEqualTo("31337");
    assertThat(saved.getClassification()).isEqualTo(Classification.JR);
    assertThat(saved.getAcEmail()).isEqualTo("ada@austincollege.edu");
  }

  @Test
  void skipsTheHeaderRow() throws IOException {
    ImportResult result = importCsv(HEADER);

    assertThat(result.total()).isZero();
    assertThat(students.findByAcid("acid")).isEmpty();
  }

  @Test
  void matchesOnStudentIdSoAReImportUpdatesRatherThanDuplicating() throws IOException {
    importCsv(HEADER + "AC91002,x,Grace,Hopper,A,100,SO,y,grace@austincollege.edu\n");

    // Same id, changed details — this is the registrar re-export case.
    ImportResult second =
        importCsv(HEADER + "AC91002,x,Grace,Hopper-Murray,I,200,SR,y,ghopper@austincollege.edu\n");

    assertThat(second.added()).isZero();
    assertThat(second.updated()).isEqualTo(1);
    assertThat(students.findAll().stream().filter(s -> "AC91002".equals(s.getAcid())).count())
        .as("re-importing must not create a second row")
        .isEqualTo(1);

    Student saved = students.findByAcid("AC91002").orElseThrow();
    assertThat(saved.getLastname()).isEqualTo("Hopper-Murray");
    assertThat(saved.getClassification()).isEqualTo(Classification.SR);
    assertThat(saved.getStatus()).isEqualTo('I');
  }

  @Test
  void anUnknownClassificationBecomesOtherRatherThanFailingTheRow() throws IOException {
    // Matches the Grails original: a registrar code we don't model is not a reason to drop an
    // otherwise valid student.
    ImportResult result =
        importCsv(HEADER + "AC91003,x,Alan,Turing,A,42,POSTBAC,y,alan@austincollege.edu\n");

    assertThat(result.added()).isEqualTo(1);
    assertThat(students.findByAcid("AC91003").orElseThrow().getClassification())
        .isEqualTo(Classification.OTHER);
  }

  @Test
  void reportsInvalidRowsInsteadOfDroppingThemSilently() throws IOException {
    // The improvement over Grails, which counted only successes and said nothing about the rest.
    String csv =
        HEADER
            + "AC91004,x,Valid,Student,A,1,FR,y,valid@austincollege.edu\n"
            + "AC91005,x,,NoFirstName,A,2,FR,y,nofirst@austincollege.edu\n"
            + "AC91006,x,Bad,Email,A,3,FR,y,not-an-email\n"
            + ",x,No,Id,A,4,FR,y,noid@austincollege.edu\n";

    ImportResult result = importCsv(csv);

    assertThat(result.added()).as("the good row still imports").isEqualTo(1);
    assertThat(result.skipped()).hasSize(3);
    assertThat(result.skipped()).extracting("acid").contains("AC91005", "AC91006", "");
    assertThat(result.skipped()).allSatisfy(row -> assertThat(row.reason()).isNotBlank());
    // Line numbers are 1-based including the header, so the reader can find the row in a
    // spreadsheet.
    assertThat(result.skipped()).extracting("line").containsExactly(3, 4, 5);

    assertThat(students.findByAcid("AC91005")).isEmpty();
    assertThat(students.findByAcid("AC91006")).isEmpty();
  }

  @Test
  void shortRowsAreReportedRatherThanCrashing() throws IOException {
    ImportResult result = importCsv(HEADER + "AC91007,x,Too,Few,A\n");

    assertThat(result.total()).isZero();
    assertThat(result.skipped()).hasSize(1);
    assertThat(result.skipped().get(0).reason()).contains("columns");
  }

  @Test
  void trailingBlankLinesAreIgnored() throws IOException {
    ImportResult result =
        importCsv(HEADER + "AC91008,x,Trailing,Newline,A,5,FR,y,tn@austincollege.edu\n\n");

    assertThat(result.added()).isEqualTo(1);
    assertThat(result.skipped()).as("a trailing newline is not an error").isEmpty();
  }

  @Test
  void quotedFieldsWithCommasSurvive() throws IOException {
    // OpenCSV handles RFC-4180 quoting; a name like "Smith, Jr." must not shift every column.
    ImportResult result =
        importCsv(HEADER + "AC91009,x,John,\"Smith, Jr.\",A,6,SR,y,js@austincollege.edu\n");

    assertThat(result.added()).isEqualTo(1);
    Student saved = students.findByAcid("AC91009").orElseThrow();
    assertThat(saved.getLastname()).isEqualTo("Smith, Jr.");
    assertThat(saved.getAcEmail()).isEqualTo("js@austincollege.edu");
  }

  @Test
  void anEmptyFileIsAnEmptyImportNotAnError() throws IOException {
    ImportResult result = importCsv("");

    assertThat(result.total()).isZero();
    assertThat(result.skipped()).isEmpty();
  }
}
