package edu.austincollege.sstation.web;

import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.repository.UserRepository;
import edu.austincollege.sstation.service.ReportCsvService;
import edu.austincollege.sstation.service.StudentReportPdfService;
import edu.austincollege.sstation.service.StudentStatsService;
import java.util.Optional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Student-facing dashboard and per-student report (TC-105c). The current student is resolved via
 * the real {@code User -> Student} FK (TC-009) — no email-string match like the Grails app.
 */
@Controller
public class StudentController {

  private final UserRepository users;
  private final StudentStatsService studentStats;
  private final ReportCsvService csv;
  private final StudentReportPdfService pdf;

  public StudentController(
      UserRepository users,
      StudentStatsService studentStats,
      ReportCsvService csv,
      StudentReportPdfService pdf) {
    this.users = users;
    this.studentStats = studentStats;
    this.csv = csv;
    this.pdf = pdf;
  }

  @GetMapping("/student")
  @PreAuthorize("hasRole('STUDENT')")
  public String studentHome(Authentication authentication, Model model) {
    Optional<Student> student = currentStudent(authentication);
    student.ifPresent(s -> model.addAttribute("dashboard", studentStats.dashboard(s)));
    model.addAttribute("hasProfile", student.isPresent());
    return "student/home";
  }

  @GetMapping("/student/report")
  @PreAuthorize("hasRole('STUDENT')")
  public String studentReport(Authentication authentication, Model model) {
    Optional<Student> student = currentStudent(authentication);
    student.ifPresent(s -> model.addAttribute("report", studentStats.report(s)));
    model.addAttribute("hasProfile", student.isPresent());
    return "student/report";
  }

  /** CSV download of the per-student report (TC-023 / TC-108c). 404 when no student profile. */
  @GetMapping("/student/report.csv")
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<String> studentReportCsv(Authentication authentication) {
    return currentStudent(authentication)
        .map(
            s ->
                CsvDownloads.attachment(
                    "student_" + s.getAcid() + "_hours.csv",
                    csv.studentReport(studentStats.report(s))))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /** PDF download of the per-student report (TC-024 / TC-108d). 404 when no student profile. */
  @GetMapping("/student/report.pdf")
  @PreAuthorize("hasRole('STUDENT')")
  public ResponseEntity<byte[]> studentReportPdf(Authentication authentication) {
    return currentStudent(authentication)
        .map(
            s ->
                ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(
                        org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"student_" + s.getAcid() + "_hours.pdf\"")
                    .body(pdf.render(studentStats.pdfReport(s))))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private Optional<Student> currentStudent(Authentication authentication) {
    // Fetch the linked student directly so we never dereference the LAZY User.student proxy
    // outside its persistence session. Empty when the account has no student profile.
    return users.findStudentByUsername(authentication.getName());
  }
}
