package edu.austincollege.sstation.service;

import edu.austincollege.sstation.domain.CampusOrg;
import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.Status;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.repository.CampusOrgRepository;
import edu.austincollege.sstation.repository.ServiceHourRepository;
import edu.austincollege.sstation.service.StudentData.Bucket;
import edu.austincollege.sstation.service.StudentData.Dashboard;
import edu.austincollege.sstation.service.StudentData.HourRow;
import edu.austincollege.sstation.service.StudentData.Report;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-student statistics — the Spring port of {@code HourService.studentStat} and {@code
 * ReportService.semesterReport/orgReport}. Drives the student dashboard and the per-student report
 * (TC-105c).
 *
 * <p>Null-safe throughout (TC-006): {@code event}/{@code campusOrg}/{@code commAg} are nullable, so
 * names fall back to the {@code other*} free-text fields or a dash. Current year via {@link
 * LocalDate#now()} (TC-001).
 */
@Service
@Transactional(readOnly = true)
public class StudentStatsService {

  private static final String[] SEMESTER_CODES = {"JA", "SP", "SU", "FA"};

  private final ServiceHourRepository serviceHours;
  private final CampusOrgRepository campusOrgs;

  public StudentStatsService(ServiceHourRepository serviceHours, CampusOrgRepository campusOrgs) {
    this.serviceHours = serviceHours;
    this.campusOrgs = campusOrgs;
  }

  public Dashboard dashboard(Student student) {
    List<ServiceHour> hours =
        serviceHours.findByStudent(student).stream()
            .sorted(Comparator.comparing(ServiceHour::getStartTime).reversed())
            .toList();

    double grand = sum(hours);
    double approved = sumWhere(hours, Status.APPROVED);
    double pending = sumWhere(hours, Status.PENDING);
    double rejected = sumWhere(hours, Status.REJECTED);

    List<HourRow> rows = new ArrayList<>();
    for (ServiceHour h : hours) {
      rows.add(
          new HourRow(
              h.getStartTime(),
              h.getDuration(),
              h.getStatus(),
              h.getEvent() != null ? h.getEvent().getName() : "—",
              campusOrgName(h),
              communityAgencyName(h)));
    }
    return new Dashboard(student.getFullName(), grand, approved, pending, rejected, rows);
  }

  public Report report(Student student) {
    List<ServiceHour> approved =
        serviceHours.findByStudent(student).stream()
            .filter(h -> h.getStatus() == Status.APPROVED)
            .toList();

    return new Report(student.getFullName(), bySemester(approved), byCampusOrg(approved));
  }

  // ----- semester bucketing (current year and the previous four) -----

  private List<Bucket> bySemester(List<ServiceHour> approved) {
    int currentYear = LocalDate.now().getYear();
    List<Bucket> buckets = new ArrayList<>();
    for (int year = currentYear; year > currentYear - 5; year--) {
      for (String code : SEMESTER_CODES) {
        final int y = year;
        List<ServiceHour> matched =
            approved.stream()
                .filter(h -> h.getStartTime().getYear() == y)
                .filter(h -> code.equals(semesterCode(h)))
                .toList();
        if (!matched.isEmpty()) {
          buckets.add(new Bucket(year + " " + code, round2(sum(matched)), matched.size()));
        }
      }
    }
    return buckets;
  }

  private List<Bucket> byCampusOrg(List<ServiceHour> approved) {
    List<Bucket> buckets = new ArrayList<>();
    for (CampusOrg org : campusOrgs.findAll()) {
      List<ServiceHour> matched =
          approved.stream()
              .filter(h -> h.getCampusOrg() != null && h.getCampusOrg().getId().equals(org.getId()))
              .toList();
      if (!matched.isEmpty()) {
        buckets.add(new Bucket(org.getName(), round2(sum(matched)), matched.size()));
      }
    }
    return buckets;
  }

  /**
   * Maps a service hour to a semester code, matching the Grails month ranges (now 1-based): Janterm
   * = January; Spring = Feb–May; Summer = Jun–Aug; Fall = Sep–Dec.
   */
  private static String semesterCode(ServiceHour hour) {
    int month = hour.getStartTime().getMonthValue();
    if (month == 1) {
      return "JA";
    }
    if (month >= 2 && month <= 5) {
      return "SP";
    }
    if (month >= 6 && month <= 8) {
      return "SU";
    }
    return "FA";
  }

  private static String campusOrgName(ServiceHour h) {
    if (h.getCampusOrg() != null) {
      return h.getCampusOrg().getName();
    }
    return h.getOtherCamOrg() != null ? h.getOtherCamOrg() : "—";
  }

  private static String communityAgencyName(ServiceHour h) {
    if (h.getCommAg() != null) {
      return h.getCommAg().getName();
    }
    return h.getOtherCommAg() != null ? h.getOtherCommAg() : "—";
  }

  private static double sum(List<ServiceHour> hours) {
    return hours.stream().mapToDouble(ServiceHour::getDuration).sum();
  }

  private static double sumWhere(List<ServiceHour> hours, Status status) {
    return hours.stream()
        .filter(h -> h.getStatus() == status)
        .mapToDouble(ServiceHour::getDuration)
        .sum();
  }

  private static double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
