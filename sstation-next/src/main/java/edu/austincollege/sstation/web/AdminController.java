package edu.austincollege.sstation.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Admin landing page. A placeholder for now — the real KPI dashboard arrives in TC-105. Kept here
 * so the {@code @PreAuthorize} role gate (TC-104) is exercised by a real endpoint.
 */
@Controller
public class AdminController {

  @GetMapping("/admin")
  @PreAuthorize("hasRole('ADMIN')")
  public String adminHome() {
    return "admin/home";
  }
}
