package edu.austincollege.sstation.service;

import java.util.List;

/**
 * Aggregated KPIs and chart series for the admin dashboard (TC-105). Mirrors the data shapes the
 * Grails {@code HourService.hourCall()} produced. Deliberately chart-library-agnostic — plain
 * label/value lists — which is what made the TC-119 swap from Highcharts to Chart.js a
 * template-only change.
 */
public record AdminDashboardData(
    OverallStat overall,
    FiveYearTrend fiveYear,
    MonthlyTrend monthly,
    List<Slice> byClassification,
    List<Slice> byStatus) {

  /** Headline KPI cards. Hours are approved-only, matching the Grails dashboard. */
  public record OverallStat(
      long totalStudents,
      double totalHours,
      double averagePerStudent,
      double totalThisYear,
      long pendingTotal) {}

  /** Approved hours per year for the last five years (ascending), plus the per-year average. */
  public record FiveYearTrend(List<Integer> years, List<Double> totals, double average) {}

  /** Approved hours for each of the 12 months of the current year, plus the monthly average. */
  public record MonthlyTrend(List<Double> monthly, double average) {}

  /** One pie slice: a label and its fraction (0..1) of all logged hours. */
  public record Slice(String label, double fraction) {}
}
