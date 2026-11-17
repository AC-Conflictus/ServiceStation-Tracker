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
import java.util.HashSet;
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

  /** Mapped values shown before any student is saved. */
  public record PreviewRow(int line, Student student, String action, String warning, String error) {
    public boolean valid() {
      return error.isEmpty();
    }
  }

  public record ImportPreview(List<PreviewRow> rows, long added, long updated, long skipped) {
    public long validCount() {
      return added + updated;
    }
  }

  /** Parse and validate without mutating managed students or saving anything. */
  @Transactional(readOnly = true)
  public ImportPreview preview(InputStream csv) throws IOException {
    List<PreviewRow> rows = parse(csv);
    Set<String> seen = new HashSet<>();
    List<PreviewRow> preview = new ArrayList<>();
    long added = 0;
    long updated = 0;
    for (PreviewRow row : rows) {
      if (!row.valid()) {
        preview.add(row);
        continue;
      }
      String acid = row.student().getAcid();
      boolean duplicate = !seen.add(acid);
      boolean update = duplicate || students.findByAcid(acid).isPresent();
      if (update) updated++;
      else added++;
      String warning = row.warning();
      if (duplicate)
        warning += " Duplicate student ID in this file; later valid rows overwrite earlier values.";
      preview.add(
          new PreviewRow(row.line(), row.student(), update ? "Update" : "Add", warning.trim(), ""));
    }
    return new ImportPreview(preview, added, updated, rows.size() - added - updated);
  }

  /** Revalidate the reviewed file and apply valid rows in order, in one transaction. */
  @Transactional
  public ImportResult importFrom(InputStream csv) throws IOException {
    int added = 0;
    int updated = 0;
    List<SkippedRow> skipped = new ArrayList<>();
    for (PreviewRow row : parse(csv)) {
      Student candidate = row.student();
      if (!row.valid()) {
        skipped.add(new SkippedRow(row.line(), candidate.getAcid(), row.error()));
        continue;
      }
      Optional<Student> existing = students.findByAcid(candidate.getAcid());
      if (existing.isPresent()) {
        Student target = existing.get();
        target.setFirstname(candidate.getFirstname());
        target.setLastname(candidate.getLastname());
        target.setStatus(candidate.getStatus());
        target.setAcBox(candidate.getAcBox());
        target.setClassification(candidate.getClassification());
        target.setAcEmail(candidate.getAcEmail());
        students.save(target);
        updated++;
      } else {
        students.save(candidate);
        added++;
      }
    }
    return new ImportResult(added, updated, skipped);
  }

  private List<PreviewRow> parse(InputStream csv) throws IOException {
    List<PreviewRow> parsed = new ArrayList<>();
    try (Reader reader = new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8));
        CSVReader rows = new CSVReader(reader)) {
      String[] row;
      int line = 0;
      while ((row = rows.readNext()) != null) {
        line++;
        if (line == 1 || isBlank(row)) continue;
        Student candidate = new Student();
        candidate.setAcid(row.length > 0 ? trim(row[ACID]) : "");
        String error = "";
        String warning = "";
        if (row.length < REQUIRED_COLUMNS) {
          error = "expected at least " + REQUIRED_COLUMNS + " columns, found " + row.length;
        } else {
          applyRow(candidate, trim(row[ACID]), row);
          error =
              candidate.getAcid().isEmpty()
                  ? "no student id in the first column"
                  : firstViolation(candidate).orElse("");
          if (candidate.getClassification() == Classification.OTHER
              && !trim(row[CLASSIFICATION]).equalsIgnoreCase("OTHER")) {
            warning = "Blank or unrecognised classification mapped to OTHER.";
          }
          if (trim(row[STATUS]).length() > 1) warning += " Status uses only the first character.";
        }
        parsed.add(
            new PreviewRow(line, candidate, error.isEmpty() ? "" : "Skip", warning.trim(), error));
      }
    } catch (CsvValidationException malformed) {
      throw new IOException(
          "The file could not be read as CSV: " + malformed.getMessage(), malformed);
    }
    return parsed;
  }

  private void applyRow(Student student, String acid, String[] row) {
    student.setAcid(acid);
    student.setFirstname(trim(row[FIRSTNAME]));
    student.setLastname(trim(row[LASTNAME]));
    student.setStatus(firstCharOrNull(row[STATUS]));
    student.setAcBox(trim(row[AC_BOX]));
    student.setClassification(parseClassification(row[CLASSIFICATION]));
    student.setAcEmail(trim(row[AC_EMAIL]));
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
