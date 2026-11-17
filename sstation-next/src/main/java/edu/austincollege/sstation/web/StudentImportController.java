package edu.austincollege.sstation.web;

import edu.austincollege.sstation.service.StudentCsvImportService;
import edu.austincollege.sstation.service.StudentCsvImportService.ImportResult;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
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

  private static final String PENDING = StudentImportController.class.getName() + ".pending";
  private static final Duration PREVIEW_LIFETIME = Duration.ofMinutes(15);

  // Spring Session serializes session attributes into the shared database.
  private record PendingUpload(String token, byte[] bytes, Instant expires)
      implements Serializable {}

  @GetMapping
  public String form(HttpSession session) {
    session.removeAttribute(PENDING);
    return "students/import";
  }

  @PostMapping
  public String upload(@RequestParam("file") MultipartFile file, Model model, HttpSession session) {
    session.removeAttribute(PENDING);
    if (file == null || file.isEmpty()) {
      model.addAttribute("error", "Choose a CSV file to upload.");
      return "students/import";
    }
    try {
      byte[] bytes = file.getBytes();
      var preview = importer.preview(new ByteArrayInputStream(bytes));
      String token = UUID.randomUUID().toString();
      if (preview.validCount() > 0) {
        session.setAttribute(
            PENDING, new PendingUpload(token, bytes, Instant.now().plus(PREVIEW_LIFETIME)));
      }
      model.addAttribute("preview", preview);
      model.addAttribute("token", token);
      return "students/import-preview";
    } catch (IOException unreadable) {
      model.addAttribute("error", unreadable.getMessage());
      return "students/import";
    }
  }

  @PostMapping("/confirm")
  public String confirm(@RequestParam("token") String token, Model model, HttpSession session) {
    PendingUpload pending = (PendingUpload) session.getAttribute(PENDING);
    if (pending == null
        || !pending.token().equals(token)
        || Instant.now().isAfter(pending.expires())) {
      model.addAttribute("error", "This preview is no longer available. Upload the CSV again.");
      return "students/import";
    }
    session.removeAttribute(PENDING);
    try {
      ImportResult result = importer.importFrom(new ByteArrayInputStream(pending.bytes()));
      model.addAttribute("result", result);
      return "students/import-result";
    } catch (IOException unreadable) {
      model.addAttribute("error", "The file could not be imported. Upload it again.");
      return "students/import";
    }
  }

  @PostMapping("/cancel")
  public String cancel(HttpSession session) {
    session.removeAttribute(PENDING);
    return "redirect:/admin/students/import";
  }
}
