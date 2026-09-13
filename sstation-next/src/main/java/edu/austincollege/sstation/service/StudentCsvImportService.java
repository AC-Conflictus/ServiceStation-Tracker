package edu.austincollege.sstation.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import edu.austincollege.sstation.domain.Classification;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.repository.StudentRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bulk student import from a registrar CSV export (TC-123).
 *
 * <p>Ports the Grails {@code StudentService.importStudents}, which was the one user-visible feature
 * the rewrite had missed — found by the TC-111 parity checklist. Matching its <em>column
 * layout</em> exactly matters more than matching its code: the file AC hands the office is a
 * registrar export with a fixed shape, and a column-order mismatch silently imports garbage rather
 * than failing.
 *
 * <p>Layout, zero-indexed, taken from the Grails implementation:
 *
 * <pre>
 *   0 acid (the key)   1 (ignored)   2 firstname   3 lastname   4 status
 *   5 acBox            6 classification          7 (ignored)   8 acEmail
 * </pre>
 *
 * <p>Columns 1 and 7 are skipped by the original and are skipped here too. The first row is treated
 * as a header and ignored, also matching the original.
 *
 * <p><b>One deliberate improvement.</b> Grails silently dropped any row that failed validation —
 * upload 500 students with 12 bad rows and you were told "488 added" with no hint that anything was
 * wrong. Skipped rows are now collected with their line number and reason and shown to the user.
 * That is strictly more information; nothing that imported before stops importing.
 */
@Service
public class StudentCsvImportService {

  /** Column positions, named so the mapping below reads as the spec above. */
  private static final int ACID = 0;

  private static final int FIRSTNAME = 2;
  private static final int LASTNAME = 3;
  private static final int STATUS = 4;
  private static final int AC_BOX = 5;
  private static final int CLASSIFICATION = 6;
  private static final int AC_EMAIL = 8;

  /** Highest index we read, so a short row can be rejected with a useful message. */
  private static final int REQUIRED_COLUMNS = AC_EMAIL + 1;

  private final StudentRepository students;
  private final Validator validator;

  public StudentCsvImportService(StudentRepository students, Validator validator) {
    this.students = students;
    this.validator = validator;
  }

  /** A row that could not be imported, and why. */
  public record SkippedRow(int line, String acid, String reason) {}

  /** Outcome of an import. {@code skipped} is empty on a completely clean file. */
  public record ImportResult(int added, int updated, List<SkippedRow> skipped) {
    public int total() {
      return added + updated;
    }

    public boolean hasSkips() {
      return !skipped.isEmpty();
    }
  }

  /**
   * Imports students, adding or updating by {@code acid}.
   *
   * <p>Transactional as a whole: a file that blows up halfway through leaves no half-applied import
   * behind. Individual <em>invalid rows</em> are not failures — they are skipped and reported,
   * which is what makes a 500-row registrar export usable when three rows are messy.
   */
  @Transactional
  public ImportResult importFrom(InputStream csv) throws IOException {
    int added = 0;
    int updated = 0;
    List<SkippedRow> skipped = new ArrayList<>();

    try (Reader reader = new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8));
        CSVReader rows = new CSVReader(reader)) {

      String[] row;
      int line = 0;
      while ((row = rows.readNext()) != null) {
        line++;
        if (line == 1) {
          continue; // header row, as in the Grails original
        }
        if (isBlank(row)) {
          continue; // trailing newline at the end of the file
        }
        if (row.length < REQUIRED_COLUMNS) {
          skipped.add(
              new SkippedRow(
                  line,
                  row.length > ACID ? row[ACID] : "",
                  "expected at least " + REQUIRED_COLUMNS + " columns, found " + row.length));
          continue;
        }

        String acid = trim(row[ACID]);
        if (acid.isEmpty()) {
          skipped.add(new SkippedRow(line, "", "no student id in the first column"));
          continue;
        }

        Optional<Student> existing = students.findByAcid(acid);
        Student student = existing.orElseGet(Student::new);
        student.setAcid(acid);
        student.setFirstname(trim(row[FIRSTNAME]));
        student.setLastname(trim(row[LASTNAME]));
        student.setStatus(firstCharOrNull(row[STATUS]));
        student.setAcBox(trim(row[AC_BOX]));
        student.setClassification(parseClassification(row[CLASSIFICATION]));
        student.setAcEmail(trim(row[AC_EMAIL]));

        Optional<String> problem = firstViolation(student);
        if (problem.isPresent()) {
          skipped.add(new SkippedRow(line, acid, problem.get()));
          continue;
        }

        students.save(student);
        if (existing.isPresent()) {
          updated++;
        } else {
          added++;
        }
      }
    } catch (CsvValidationException malformed) {
      throw new IOException(
          "The file could not be read as CSV: " + malformed.getMessage(), malformed);
    }

    return new ImportResult(added, updated, skipped);
  }

  /**
   * Unrecognised classifications become {@link Classification#OTHER} rather than failing the row —
   * the Grails original did the same, and a registrar export using a code we don't model is not a
   * reason to drop an otherwise valid student.
   */
  private Classification parseClassification(String raw) {
    String value = trim(raw);
    if (value.isEmpty()) {
      return Classification.OTHER;
    }
    try {
      return Classification.valueOf(value.toUpperCase());
    } catch (IllegalArgumentException unknown) {
      return Classification.OTHER;
    }
  }

  /** The first bean-validation message for this student, if it is not importable. */
  private Optional<String> firstViolation(Student student) {
    Set<ConstraintViolation<Student>> violations = validator.validate(student);
    return violations.stream().findFirst().map(v -> v.getPropertyPath() + " " + v.getMessage());
  }

  private static Character firstCharOrNull(String raw) {
    String value = trim(raw);
    return value.isEmpty() ? null : value.charAt(0);
  }

  private static String trim(String raw) {
    return raw == null ? "" : raw.trim();
  }

  private static boolean isBlank(String[] row) {
    for (String cell : row) {
      if (cell != null && !cell.isBlank()) {
        return false;
      }
    }
    return true;
  }
}
