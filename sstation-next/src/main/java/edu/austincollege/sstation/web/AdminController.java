package edu.austincollege.sstation.web;

import edu.austincollege.sstation.service.StatsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Admin dashboard — the KPI + chart landing page (TC-105). Port of the Grails {@code
 * HomeController.adminHome} / {@code HourService}-backed view. Gated to {@code ROLE_ADMIN} via
 * {@code @PreAuthorize} (TC-104 / TC-019).
 */
@Controller
public class AdminController {

  private final StatsService statsService;

  public AdminController(StatsService statsService) {
    this.statsService = statsService;
  }

  @GetMapping("/admin")
  @PreAuthorize("hasRole('ADMIN')")
  public String adminHome(Model model) {
    model.addAttribute("dashboard", statsService.adminDashboard());
    return "admin/home";
  }
}
