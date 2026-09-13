package edu.austincollege.sstation.web;

import edu.austincollege.sstation.service.StudentCsvImportService;
import edu.austincollege.sstation.service.StudentCsvImportService.ImportResult;
import java.io.IOException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * Bulk student import from a registrar CSV (TC-123) — the feature the TC-111 parity checklist found
 * missing from the rewrite.
 *
 * <p><b>Not</b> a straight port of the Grails permissions. The original lived on {@code
 * AcStudentController}, whose class-level {@code @Secured} listed {@code ROLE_STUDENT} — so any
 * signed-in student could overwrite the entire student roster. That is an accident of a grab-bag
 * controller (it also hosts the student's own dashboard), not a decision worth carrying over. This
 * matches the rewrite's existing answer to "who manages students": {@code StudentAdminController}'s
 * ADMIN + MODERATOR, both of whom can already create, edit and delete students one at a time.
 */
@Controller
@RequestMapping("/admin/students/import")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class StudentImportController {

  private final StudentCsvImportService importer;

  public StudentImportController(StudentCsvImportService importer) {
    this.importer = importer;
  }

  @GetMapping
  public String form() {
    return "students/import";
  }

  @PostMapping
  public String upload(@RequestParam("file") MultipartFile file, Model model) {
    if (file == null || file.isEmpty()) {
      model.addAttribute("error", "Choose a CSV file to upload.");
      return "students/import";
    }

    try {
      ImportResult result = importer.importFrom(file.getInputStream());
      model.addAttribute("result", result);
      return "students/import-result";
    } catch (IOException unreadable) {
      // The import is @Transactional, so a file that fails partway leaves nothing behind.
      model.addAttribute("error", unreadable.getMessage());
      return "students/import";
    }
  }
}
