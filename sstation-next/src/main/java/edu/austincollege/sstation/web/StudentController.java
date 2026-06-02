package edu.austincollege.sstation.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Student landing page. Placeholder for now — the real student dashboard (with the logged-in
 * student's hours) arrives in TC-105. Exercises the STUDENT role gate (TC-104).
 */
@Controller
public class StudentController {

  @GetMapping("/student")
  @PreAuthorize("hasRole('STUDENT')")
  public String studentHome() {
    return "student/home";
  }
}
