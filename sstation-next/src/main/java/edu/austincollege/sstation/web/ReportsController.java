package edu.austincollege.sstation.web;

import edu.austincollege.sstation.service.ReportCsvService;
import edu.austincollege.sstation.service.ReportData.SemesterReport;
import edu.austincollege.sstation.service.ReportData.SummaryReport;
import edu.austincollege.sstation.service.ReportService;
import org.springframework.http.ResponseEntity;
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
  private final ReportCsvService csv;

  public ReportsController(ReportService reports, ReportCsvService csv) {
    this.reports = reports;
    this.csv = csv;
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

  // ----- CSV downloads (TC-023 / TC-108c) -----

  @GetMapping("/year.csv")
  public ResponseEntity<String> yearCsv() {
    return CsvDownloads.attachment("year_kpis.csv", csv.yearKpis(reports.fiveYearKpis()));
  }

  @GetMapping("/event.csv")
  public ResponseEntity<String> eventCsv() {
    return CsvDownloads.attachment(
        "event_hours.csv", csv.namedTotals("Event", "Approved hours", reports.eventTotals()));
  }

  @GetMapping("/community-org.csv")
  public ResponseEntity<String> communityOrgCsv() {
    return CsvDownloads.attachment(
        "community_org_hours.csv",
        csv.namedTotals("Community agency", "Hours", reports.communityAgencyTotals()));
  }

  @GetMapping("/campus-org.csv")
  public ResponseEntity<String> campusOrgCsv() {
    return CsvDownloads.attachment(
        "campus_org_hours.csv", csv.namedTotals("Campus org", "Hours", reports.campusOrgTotals()));
  }

  @GetMapping("/summary.csv")
  public ResponseEntity<String> summaryCsv(@RequestParam(required = false) Integer year) {
    SummaryReport report = reports.summary(year);
    return CsvDownloads.attachment("summary_" + report.year() + ".csv", csv.summary(report));
  }

  @GetMapping("/semester.csv")
  public ResponseEntity<String> semesterCsv(
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) String semester) {
    SemesterReport report = reports.semester(year, semester);
    return CsvDownloads.attachment(
        "semester_" + report.year() + "_" + report.semester().toLowerCase() + ".csv",
        csv.semester(report));
  }
}
