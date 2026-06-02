package edu.austincollege.sstation.service;

import edu.austincollege.sstation.domain.CampusOrg;
import edu.austincollege.sstation.domain.CommunityAgency;
import edu.austincollege.sstation.domain.Event;
import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.Status;
import edu.austincollege.sstation.repository.CampusOrgRepository;
import edu.austincollege.sstation.repository.CommunityAgencyRepository;
import edu.austincollege.sstation.repository.EventRepository;
import edu.austincollege.sstation.repository.ServiceHourRepository;
import edu.austincollege.sstation.repository.StudentRepository;
import edu.austincollege.sstation.service.ReportData.NamedTotal;
import edu.austincollege.sstation.service.ReportData.SemesterReport;
import edu.austincollege.sstation.service.ReportData.SummaryReport;
import edu.austincollege.sstation.service.ReportData.YearKpi;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The six admin/moderator reports — Spring port of {@code ReportsController} + {@code
 * StationReportService} + the three stateful {@code *ReportService} helpers.
 *
 * <p>Bugs fixed at the source:
 *
 * <ul>
 *   <li><b>TC-001/TC-002</b>: every "current year" comes from {@link LocalDate#now()}.
 *   <li><b>TC-003</b>: top-N is bounded by {@code min(5, list sizes)} — never an out-of-bounds get.
 *   <li><b>TC-006/TC-010</b>: every nullable FK ({@code event}/{@code commAg}/{@code campusOrg}) is
 *       null-guarded — the unguarded {@code s.commAg.name} in the old {@code *ReportService}
 *       helpers and {@code commOrgReport} could NPE on real data.
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

  private static final int TOP_N = 5;
  private static final List<String> SEMESTERS = List.of("Fall", "Janterm", "Spring", "Summer");

  private final ServiceHourRepository serviceHours;
  private final StudentRepository students;
  private final EventRepository events;
  private final CampusOrgRepository campusOrgs;
  private final CommunityAgencyRepository agencies;

  public ReportService(
      ServiceHourRepository serviceHours,
      StudentRepository students,
      EventRepository events,
      CampusOrgRepository campusOrgs,
      CommunityAgencyRepository agencies) {
    this.serviceHours = serviceHours;
    this.students = students;
    this.events = events;
    this.campusOrgs = campusOrgs;
    this.agencies = agencies;
  }

  /** Year options for the selectors: the current year and the previous four (descending). */
  public List<Integer> yearOptions() {
    int currentYear = LocalDate.now().getYear();
    List<Integer> years = new ArrayList<>();
    for (int y = currentYear; y > currentYear - 5; y--) {
      years.add(y);
    }
    return years;
  }

  public List<String> semesterOptions() {
    return SEMESTERS;
  }

  // ----- Year report -----

  public List<YearKpi> fiveYearKpis() {
    List<YearKpi> kpis = new ArrayList<>();
    for (int year : yearOptions()) {
      kpis.add(yearKpi(year));
    }
    return kpis;
  }

  private YearKpi yearKpi(int year) {
    double total =
        approvedHours().stream()
            .filter(h -> h.getStartTime().getYear() == year)
            .mapToDouble(ServiceHour::getDuration)
            .sum();
    return new YearKpi(
        year,
        round2(total),
        average(total, students.count()),
        average(total, campusOrgs.count()),
        average(total, agencies.count()),
        average(total, events.count()));
  }

  // ----- Event report (approved hours per event) -----

  public List<NamedTotal> eventTotals() {
    List<ServiceHour> approved = approvedHours();
    List<NamedTotal> out = new ArrayList<>();
    for (Event event : events.findAll()) {
      double total =
          approved.stream()
              .filter(h -> h.getEvent() != null && h.getEvent().getId().equals(event.getId()))
              .mapToDouble(ServiceHour::getDuration)
              .sum();
      out.add(new NamedTotal(event.getName(), round2(total)));
    }
    return out;
  }

  // ----- Community-org & campus-org reports (all hours per org) -----

  public List<NamedTotal> communityAgencyTotals() {
    List<ServiceHour> all = serviceHours.findAll();
    List<NamedTotal> out = new ArrayList<>();
    for (CommunityAgency ag : agencies.findAll()) {
      double total =
          all.stream()
              .filter(h -> h.getCommAg() != null && h.getCommAg().getId().equals(ag.getId()))
              .mapToDouble(ServiceHour::getDuration)
              .sum();
      out.add(new NamedTotal(ag.getName(), round2(total)));
    }
    return out;
  }

  public List<NamedTotal> campusOrgTotals() {
    List<ServiceHour> all = serviceHours.findAll();
    List<NamedTotal> out = new ArrayList<>();
    for (CampusOrg org : campusOrgs.findAll()) {
      double total =
          all.stream()
              .filter(h -> h.getCampusOrg() != null && h.getCampusOrg().getId().equals(org.getId()))
              .mapToDouble(ServiceHour::getDuration)
              .sum();
      out.add(new NamedTotal(org.getName(), round2(total)));
    }
    return out;
  }

  // ----- Summary report -----

  public SummaryReport summary(Integer requestedYear) {
    int year = requestedYear != null ? requestedYear : LocalDate.now().getYear();
    List<ServiceHour> all = serviceHours.findAll();
    List<ServiceHour> inYear =
        all.stream().filter(h -> h.getStartTime().getYear() == year).toList();

    double totalHours = inYear.stream().mapToDouble(ServiceHour::getDuration).sum();

    return new SummaryReport(
        year,
        yearOptions(),
        round2(totalHours),
        topByAgency(inYear),
        topByCampusOrg(inYear),
        topByEvent(inYear));
  }

  // ----- Semester report -----

  public SemesterReport semester(Integer requestedYear, String requestedSemester) {
    int year = requestedYear != null ? requestedYear : LocalDate.now().getYear();
    // Default to Fall when absent/unknown. Null-check first: SEMESTERS is a List.of(...), which is
    // null-hostile — contains(null) would throw NPE.
    String semester =
        (requestedSemester != null && SEMESTERS.contains(requestedSemester))
            ? requestedSemester
            : "Fall";

    List<ServiceHour> inSemester =
        serviceHours.findAll().stream()
            .filter(h -> h.getStartTime().getYear() == year)
            .filter(h -> inSemester(h, semester))
            .toList();

    double totalHours = inSemester.stream().mapToDouble(ServiceHour::getDuration).sum();

    return new SemesterReport(
        year,
        semester,
        yearOptions(),
        SEMESTERS,
        round2(totalHours),
        topByAgency(inSemester),
        topByCampusOrg(inSemester),
        topByEvent(inSemester));
  }

  // ----- shared helpers -----

  /** Top-N agencies by hours within the given list, bounded by {@code min(5, agency count)}. */
  private List<NamedTotal> topByAgency(List<ServiceHour> hours) {
    List<CommunityAgency> ags = agencies.findAll();
    int n = Math.min(TOP_N, ags.size());
    List<NamedTotal> out = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      CommunityAgency ag = ags.get(i);
      double total =
          hours.stream()
              .filter(h -> h.getCommAg() != null && h.getCommAg().getId().equals(ag.getId()))
              .mapToDouble(ServiceHour::getDuration)
              .sum();
      out.add(new NamedTotal(ag.getName(), round2(total)));
    }
    return out;
  }

  private List<NamedTotal> topByCampusOrg(List<ServiceHour> hours) {
    List<CampusOrg> orgs = campusOrgs.findAll();
    int n = Math.min(TOP_N, orgs.size());
    List<NamedTotal> out = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      CampusOrg org = orgs.get(i);
      double total =
          hours.stream()
              .filter(h -> h.getCampusOrg() != null && h.getCampusOrg().getId().equals(org.getId()))
              .mapToDouble(ServiceHour::getDuration)
              .sum();
      out.add(new NamedTotal(org.getName(), round2(total)));
    }
    return out;
  }

  private List<NamedTotal> topByEvent(List<ServiceHour> hours) {
    List<Event> evs = events.findAll();
    int n = Math.min(TOP_N, evs.size());
    List<NamedTotal> out = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      Event ev = evs.get(i);
      double total =
          hours.stream()
              .filter(h -> h.getEvent() != null && h.getEvent().getId().equals(ev.getId()))
              .mapToDouble(ServiceHour::getDuration)
              .sum();
      out.add(new NamedTotal(ev.getName(), round2(total)));
    }
    return out;
  }

  /**
   * Semester membership by month, matching the Grails ranges (Calendar.MONTH is 0-based there;
   * {@code getMonthValue()} is 1-based here). Janterm = January; Spring = Feb–May; Summer =
   * Jun–Aug; Fall = Sep–Dec.
   */
  private static boolean inSemester(ServiceHour hour, String semester) {
    int month = hour.getStartTime().getMonthValue();
    return switch (semester) {
      case "Janterm" -> month == 1;
      case "Spring" -> month >= 2 && month <= 5;
      case "Summer" -> month >= 6 && month <= 8;
      case "Fall" -> month >= 9 && month <= 12;
      default -> false;
    };
  }

  private List<ServiceHour> approvedHours() {
    return serviceHours.findByStatus(Status.APPROVED);
  }

  private static double average(double total, long count) {
    return count == 0 ? 0 : round2(total / count);
  }

  private static double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
