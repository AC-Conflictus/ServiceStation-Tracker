package edu.austincollege.sstation.web;

import edu.austincollege.sstation.service.StatsService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
  public String adminHome(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      Model model) {
    model.addAttribute("dashboard", statsService.adminDashboard(from, to));
    model.addAttribute("from", from);
    model.addAttribute("to", to);
    model.addAttribute("ranged", from != null || to != null);
    return "admin/home";
  }
}
