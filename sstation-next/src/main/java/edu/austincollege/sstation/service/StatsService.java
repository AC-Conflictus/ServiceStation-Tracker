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

  @Transactional(readOnly = true)
  public AdminDashboardData adminDashboard() {
    int currentYear = LocalDate.now().getYear();

    List<ServiceHour> approved = serviceHours.findByStatus(Status.APPROVED);
    List<ServiceHour> all = serviceHours.findAll();

    double approvedTotal = sumDuration(approved);
    double allTotal = sumDuration(all);

    return new AdminDashboardData(
        overallStat(approved, approvedTotal, currentYear),
        fiveYearTrend(approved, approvedTotal, currentYear),
        monthlyTrend(approved, currentYear),
        classificationSlices(all, allTotal),
        statusSlices(all, allTotal));
  }

  private AdminDashboardData.OverallStat overallStat(
      List<ServiceHour> approved, double approvedTotal, int currentYear) {
    long totalStudents = students.count();
    double totalThisYear =
        approved.stream()
            .filter(h -> yearOf(h) == currentYear)
            .mapToDouble(ServiceHour::getDuration)
            .sum();
    double averagePerStudent = totalStudents == 0 ? 0 : approvedTotal / totalStudents;
    long pending = serviceHours.countByStatus(Status.PENDING);
    return new AdminDashboardData.OverallStat(
        totalStudents, approvedTotal, averagePerStudent, totalThisYear, pending);
  }

  private AdminDashboardData.FiveYearTrend fiveYearTrend(
      List<ServiceHour> approved, double approvedTotal, int currentYear) {
    List<Integer> years = new ArrayList<>();
    List<Double> totals = new ArrayList<>();
    for (int y = currentYear - 4; y <= currentYear; y++) {
      final int year = y;
      years.add(year);
      totals.add(
          approved.stream()
              .filter(h -> yearOf(h) == year)
              .mapToDouble(ServiceHour::getDuration)
              .sum());
    }
    return new AdminDashboardData.FiveYearTrend(years, totals, approvedTotal / 5);
  }

  private AdminDashboardData.MonthlyTrend monthlyTrend(
      List<ServiceHour> approved, int currentYear) {
    List<ServiceHour> thisYear = approved.stream().filter(h -> yearOf(h) == currentYear).toList();
    List<Double> monthly = new ArrayList<>();
    for (int m = 1; m <= 12; m++) {
      final int month = m;
      monthly.add(
          thisYear.stream()
              .filter(h -> h.getStartTime().getMonthValue() == month)
              .mapToDouble(ServiceHour::getDuration)
              .sum());
    }
    double average = thisYear.isEmpty() ? 0 : sumDuration(thisYear) / 12;
    return new AdminDashboardData.MonthlyTrend(monthly, average);
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
