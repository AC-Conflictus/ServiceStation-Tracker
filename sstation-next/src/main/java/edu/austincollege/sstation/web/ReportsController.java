package edu.austincollege.sstation.web;

import edu.austincollege.sstation.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The six read-only reports (TC-105b). Gated to admins and moderators, matching the Grails {@code
 * ReportsController}'s {@code @Secured(['ROLE_ADMIN','ROLE_MODERATOR'])} (TC-104 / TC-019).
 */
@Controller
@RequestMapping("/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class ReportsController {

  private final ReportService reports;

  public ReportsController(ReportService reports) {
    this.reports = reports;
  }

  @GetMapping
  public String index(Model model) {
    model.addAttribute("yearOptions", reports.yearOptions());
    model.addAttribute("semesterOptions", reports.semesterOptions());
    return "reports/index";
  }

  @GetMapping("/year")
  public String yearReport(Model model) {
    model.addAttribute("kpis", reports.fiveYearKpis());
    return "reports/year";
  }

  @GetMapping("/event")
  public String eventReport(Model model) {
    model.addAttribute("totals", reports.eventTotals());
    return "reports/event";
  }

  @GetMapping("/community-org")
  public String communityOrgReport(Model model) {
    model.addAttribute("totals", reports.communityAgencyTotals());
    return "reports/community-org";
  }

  @GetMapping("/campus-org")
  public String campusOrgReport(Model model) {
    model.addAttribute("totals", reports.campusOrgTotals());
    return "reports/campus-org";
  }

  @GetMapping("/summary")
  public String summaryReport(@RequestParam(required = false) Integer year, Model model) {
    model.addAttribute("report", reports.summary(year));
    return "reports/summary";
  }

  @GetMapping("/semester")
  public String semesterReport(
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) String semester,
      Model model) {
    model.addAttribute("report", reports.semester(year, semester));
    return "reports/semester";
  }
}
