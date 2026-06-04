package edu.austincollege.sstation.service;

import edu.austincollege.sstation.domain.Classification;
import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.Status;
import edu.austincollege.sstation.repository.ServiceHourRepository;
import edu.austincollege.sstation.repository.StudentRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Statistics for the admin dashboard — the Spring port of the Grails {@code HourService}.
 *
 * <p>Three Grails bugs are fixed at the source here:
 *
 * <ul>
 *   <li><b>TC-001</b>: the "current year" comes from {@link LocalDate#now()} on every call, never a
 *       hardcoded constant.
 *   <li><b>TC-006</b>: every nullable FK / field is null-guarded.
 *   <li>The classification/status pies divide by the sum of <em>all</em> logged hours, so the
 *       fractions are coherent. (The Grails version divided by the approved-only total, which made
 *       the pending/rejected slices meaningless. Same chart <em>shape</em>, corrected denominator —
 *       flagged for the dev.)
 * </ul>
 */
@Service
public class StatsService {

  private final ServiceHourRepository serviceHours;
  private final StudentRepository students;

  public StatsService(ServiceHourRepository serviceHours, StudentRepository students) {
    this.serviceHours = serviceHours;
    this.students = students;
  }

  /** All-time dashboard (no date filter) — the default landing view. */
  @Transactional(readOnly = true)
  public AdminDashboardData adminDashboard() {
    return adminDashboard(null, null);
  }

  /**
   * Dashboard scoped to an optional inclusive date range (TC-025 / TC-108e). A null bound is open,
   * so {@code (null, null)} reproduces the all-time view exactly — every KPI, the year/month
   * trends, and both pies are computed over the in-range hours.
   */
  @Transactional(readOnly = true)
  public AdminDashboardData adminDashboard(LocalDate from, LocalDate to) {
    int currentYear = LocalDate.now().getYear();
    boolean ranged = from != null || to != null;

    List<ServiceHour> approved = inRange(serviceHours.findByStatus(Status.APPROVED), from, to);
    List<ServiceHour> all = inRange(serviceHours.findAll(), from, to);

    double approvedTotal = sumDuration(approved);
    double allTotal = sumDuration(all);

    return new AdminDashboardData(
        overallStat(approved, approvedTotal, all, ranged, currentYear),
        yearTrend(approved, approvedTotal, from, to, currentYear),
        monthlyTrend(approved, ranged, currentYear),
        classificationSlices(all, allTotal),
        statusSlices(all, allTotal));
  }

  private AdminDashboardData.OverallStat overallStat(
      List<ServiceHour> approved,
      double approvedTotal,
      List<ServiceHour> all,
      boolean ranged,
      int currentYear) {
    long totalStudents = students.count();
    double averagePerStudent = totalStudents == 0 ? 0 : approvedTotal / totalStudents;
    // With a range active this headline reflects the selected period; otherwise the current year.
    double periodOrYear =
        ranged
            ? approvedTotal
            : approved.stream()
                .filter(h -> yearOf(h) == currentYear)
                .mapToDouble(ServiceHour::getDuration)
                .sum();
    long pending = all.stream().filter(h -> h.getStatus() == Status.PENDING).count();
    return new AdminDashboardData.OverallStat(
        totalStudents, approvedTotal, averagePerStudent, periodOrYear, pending);
  }

  /**
   * Approved hours per year. With no range this is the current year and the previous four
   * (preserving the original 5-year chart); with a range it spans the range's years.
   */
  private AdminDashboardData.FiveYearTrend yearTrend(
      List<ServiceHour> approved,
      double approvedTotal,
      LocalDate from,
      LocalDate to,
      int currentYear) {
    int endYear = to != null ? to.getYear() : currentYear;
    int startYear = from != null ? from.getYear() : endYear - 4;
    if (startYear > endYear) {
      startYear = endYear; // guard inverted ranges
    }
    List<Integer> years = new ArrayList<>();
    List<Double> totals = new ArrayList<>();
    for (int y = startYear; y <= endYear; y++) {
      final int year = y;
      years.add(year);
      totals.add(
          approved.stream()
              .filter(h -> yearOf(h) == year)
              .mapToDouble(ServiceHour::getDuration)
              .sum());
    }
    double average = years.isEmpty() ? 0 : approvedTotal / years.size();
    return new AdminDashboardData.FiveYearTrend(years, totals, average);
  }

  /**
   * Approved hours by calendar month. With no range this is the current year (preserving the
   * original chart); with a range it sums the in-range hours by month.
   */
  private AdminDashboardData.MonthlyTrend monthlyTrend(
      List<ServiceHour> approved, boolean ranged, int currentYear) {
    List<ServiceHour> scope =
        ranged ? approved : approved.stream().filter(h -> yearOf(h) == currentYear).toList();
    List<Double> monthly = new ArrayList<>();
    for (int m = 1; m <= 12; m++) {
      final int month = m;
      monthly.add(
          scope.stream()
              .filter(h -> h.getStartTime().getMonthValue() == month)
              .mapToDouble(ServiceHour::getDuration)
              .sum());
    }
    double average = scope.isEmpty() ? 0 : sumDuration(scope) / 12;
    return new AdminDashboardData.MonthlyTrend(monthly, average);
  }

  /**
   * Filters to hours whose date falls within the inclusive [from, to] range (null = open bound).
   */
  private static List<ServiceHour> inRange(List<ServiceHour> hours, LocalDate from, LocalDate to) {
    if (from == null && to == null) {
      return hours;
    }
    return hours.stream()
        .filter(
            h -> {
              LocalDate d = h.getStartTime().toLocalDate();
              return (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
            })
        .toList();
  }

  private List<AdminDashboardData.Slice> classificationSlices(
      List<ServiceHour> all, double allTotal) {
    List<AdminDashboardData.Slice> slices = new ArrayList<>();
    for (Classification c : Classification.values()) {
      double sum =
          all.stream()
              .filter(h -> h.getStudent() != null && h.getStudent().getClassification() == c)
              .mapToDouble(ServiceHour::getDuration)
              .sum();
      slices.add(new AdminDashboardData.Slice(c.getDisplayName(), fraction(sum, allTotal)));
    }
    return slices;
  }

  private List<AdminDashboardData.Slice> statusSlices(List<ServiceHour> all, double allTotal) {
    List<AdminDashboardData.Slice> slices = new ArrayList<>();
    for (Status s : Status.values()) {
      double sum =
          all.stream().filter(h -> h.getStatus() == s).mapToDouble(ServiceHour::getDuration).sum();
      slices.add(new AdminDashboardData.Slice(s.name(), fraction(sum, allTotal)));
    }
    return slices;
  }

  private static double sumDuration(List<ServiceHour> hours) {
    return hours.stream().mapToDouble(ServiceHour::getDuration).sum();
  }

  private static int yearOf(ServiceHour hour) {
    return hour.getStartTime().getYear();
  }

  private static double fraction(double part, double whole) {
    return whole == 0 ? 0 : part / whole;
  }
}
