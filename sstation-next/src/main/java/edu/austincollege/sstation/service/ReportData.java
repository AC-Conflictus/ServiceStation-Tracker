package edu.austincollege.sstation.service;

import java.util.List;

/** DTOs backing the six admin/moderator reports (TC-105b). */
public final class ReportData {

  private ReportData() {}

  /**
   * A label and an hours total. Used by the event / community-org / campus-org reports and the
   * top-N breakdowns.
   */
  public record NamedTotal(String name, double total) {}

  /** One year's KPI row (year report). Mirrors {@code StationReportService.hourKPIbyYear}. */
  public record YearKpi(
      int year,
      double total,
      double avgByStudent,
      double avgByGroup,
      double avgByCommOrg,
      double avgByEvent) {}

  /** Summary report for a single year (top-N agencies/orgs/events + the year's total). */
  public record SummaryReport(
      int year,
      List<Integer> yearOptions,
      double totalHours,
      List<NamedTotal> topAgencies,
      List<NamedTotal> topCampusOrgs,
      List<NamedTotal> topEvents) {}

  /** Semester report for a year + semester (Fall/Janterm/Spring/Summer). */
  public record SemesterReport(
      int year,
      String semester,
      List<Integer> yearOptions,
      List<String> semesterOptions,
      double totalHours,
      List<NamedTotal> topAgencies,
      List<NamedTotal> topCampusOrgs,
      List<NamedTotal> topEvents) {}
}
