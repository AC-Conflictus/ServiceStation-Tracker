package edu.austincollege.sstation.web;

import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.repository.UserRepository;
import edu.austincollege.sstation.service.StudentStatsService;
import java.util.Optional;
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

  public StudentController(UserRepository users, StudentStatsService studentStats) {
    this.users = users;
    this.studentStats = studentStats;
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

  private Optional<Student> currentStudent(Authentication authentication) {
    // Fetch the linked student directly so we never dereference the LAZY User.student proxy
    // outside its persistence session. Empty when the account has no student profile.
    return users.findStudentByUsername(authentication.getName());
  }
}
