package edu.austincollege.sstation.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.austincollege.sstation.domain.CampusOrg;
import edu.austincollege.sstation.domain.CommunityAgency;
import edu.austincollege.sstation.domain.Event;
import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.Status;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.repository.CampusOrgRepository;
import edu.austincollege.sstation.repository.CommunityAgencyRepository;
import edu.austincollege.sstation.repository.EventRepository;
import edu.austincollege.sstation.repository.ServiceHourRepository;
import edu.austincollege.sstation.repository.StudentRepository;
import edu.austincollege.sstation.service.ReportData.SemesterReport;
import edu.austincollege.sstation.service.ReportData.SummaryReport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Coverage for the six-report {@link ReportService} (TC-105b). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReportServiceTest {

  @Autowired private ServiceHourRepository serviceHours;
  @Autowired private StudentRepository students;
  @Autowired private EventRepository events;
  @Autowired private CampusOrgRepository campusOrgs;
  @Autowired private CommunityAgencyRepository agencies;

  private ReportService reports;
  private int currentYear;
  private Student sam;
  private Event janserve;
  private CampusOrg think;
  private CommunityAgency crisis;

  @BeforeEach
  void setUp() {
    reports = new ReportService(serviceHours, students, events, campusOrgs, agencies);
    currentYear = LocalDate.now().getYear();
    sam = students.save(student());
    janserve = events.save(event("JanServe"));
    think = campusOrgs.save(campusOrg("THINK"));
    crisis = agencies.save(agency("Crisis Center"));
  }

  @Test
  void yearOptionsAreCurrentYearAndPreviousFourDescending() {
    assertThat(reports.yearOptions())
        .containsExactly(
            currentYear, currentYear - 1, currentYear - 2, currentYear - 3, currentYear - 4);
  }

  @Test
  void eventTotalsCountApprovedHoursForThatEvent() {
    save(Status.APPROVED, 3.0, LocalDateTime.now(), janserve, think, crisis);
    save(Status.PENDING, 5.0, LocalDateTime.now(), janserve, think, crisis); // not approved
    save(Status.APPROVED, 2.0, LocalDateTime.now(), null, think, crisis); // null event

    var totals = reports.eventTotals();

    assertThat(totals)
        .anySatisfy(
            t -> {
              assertThat(t.name()).isEqualTo("JanServe");
              assertThat(t.total()).isEqualTo(3.0);
            });
  }

  @Test
  void communityAgencyTotalsAreNullSafeAndCountAllStatuses() {
    save(Status.APPROVED, 2.0, LocalDateTime.now(), janserve, think, crisis);
    save(Status.REJECTED, 1.5, LocalDateTime.now(), janserve, think, crisis); // all statuses count
    save(Status.APPROVED, 4.0, LocalDateTime.now(), janserve, think, null); // null agency: ignored

    var totals = reports.communityAgencyTotals();

    assertThat(totals)
        .anySatisfy(
            t -> {
              assertThat(t.name()).isEqualTo("Crisis Center");
              assertThat(t.total()).isEqualTo(3.5);
            });
  }

  @Test
  void summaryUsesRequestedYearAndIsBoundsSafeWithSparseData() {
    save(Status.APPROVED, 2.0, LocalDateTime.now(), janserve, think, crisis);
    save(Status.APPROVED, 3.0, LocalDateTime.now().minusYears(1), janserve, think, crisis);

    SummaryReport thisYear = reports.summary(currentYear);
    assertThat(thisYear.year()).isEqualTo(currentYear);
    assertThat(thisYear.totalHours()).isEqualTo(2.0);
    // Only one agency/org/event exists — top-N must not run past the end (TC-003).
    assertThat(thisYear.topAgencies()).hasSize(1);
    assertThat(thisYear.topCampusOrgs()).hasSize(1);
    assertThat(thisYear.topEvents()).hasSize(1);

    SummaryReport lastYear = reports.summary(currentYear - 1);
    assertThat(lastYear.totalHours()).isEqualTo(3.0);
  }

  @Test
  void summaryDefaultsToCurrentYearWhenNull() {
    save(Status.APPROVED, 7.0, LocalDateTime.now(), janserve, think, crisis);
    assertThat(reports.summary(null).year()).isEqualTo(currentYear);
    assertThat(reports.summary(null).totalHours()).isEqualTo(7.0);
  }

  @Test
  void semesterFiltersByMonthRange() {
    // Spring = Feb–May. Put one hour in March, one in October (Fall).
    save(Status.APPROVED, 2.0, dateInMonth(3), janserve, think, crisis);
    save(Status.APPROVED, 5.0, dateInMonth(10), janserve, think, crisis);

    SemesterReport spring = reports.semester(currentYear, "Spring");
    assertThat(spring.totalHours()).isEqualTo(2.0);

    SemesterReport fall = reports.semester(currentYear, "Fall");
    assertThat(fall.totalHours()).isEqualTo(5.0);
  }

  @Test
  void fiveYearKpisHaveFiveRowsAndComputeAverages() {
    save(Status.APPROVED, 4.0, LocalDateTime.now(), janserve, think, crisis);

    var kpis = reports.fiveYearKpis();
    assertThat(kpis).hasSize(5);
    var thisYear = kpis.get(0);
    assertThat(thisYear.year()).isEqualTo(currentYear);
    assertThat(thisYear.total()).isEqualTo(4.0);
    assertThat(thisYear.avgByStudent()).isEqualTo(4.0); // 4 hours / 1 student
  }

  // --- helpers ---

  private void save(
      Status status,
      double duration,
      LocalDateTime start,
      Event event,
      CampusOrg org,
      CommunityAgency ag) {
    ServiceHour h = new ServiceHour();
    h.setDescription("service");
    h.setStatus(status);
    h.setDuration(duration);
    h.setStartTime(start);
    h.setLastModified(LocalDateTime.now());
    h.setStudent(sam);
    h.setEvent(event);
    h.setCampusOrg(org);
    h.setCommAg(ag);
    serviceHours.save(h);
  }

  private LocalDateTime dateInMonth(int month) {
    return LocalDateTime.of(currentYear, month, 15, 10, 0);
  }

  private static Student student() {
    Student s = new Student();
    s.setFirstname("Sam");
    s.setLastname("Student");
    s.setAcid("AC50000");
    s.setAcEmail("student@austincollege.edu");
    s.setStatus('A');
    s.setIsModerator(false);
    return s;
  }

  private static Event event(String name) {
    Event e = new Event();
    e.setName(name);
    e.setDescription("desc");
    e.setContact("Jane Doe");
    e.setContactPhone("9038132000");
    e.setContactEmail("jdoe@austincollege.edu");
    return e;
  }

  private static CampusOrg campusOrg(String name) {
    CampusOrg o = new CampusOrg();
    o.setName(name);
    o.setDescription("desc");
    o.setContact("Jane Doe");
    o.setContactPhone("9038132000");
    o.setContactEmail("jdoe@austincollege.edu");
    return o;
  }

  private static CommunityAgency agency(String name) {
    CommunityAgency a = new CommunityAgency();
    a.setName(name);
    a.setAddress("900 N. Grand Ave.");
    a.setDescription("desc");
    a.setContact("Jane Doe");
    a.setContactPhone("9038132000");
    a.setContactEmail("jdoe@austincollege.edu");
    return a;
  }
}
