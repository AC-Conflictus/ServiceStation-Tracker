package edu.austincollege.sstation.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.austincollege.sstation.domain.CampusOrg;
import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.Status;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.repository.CampusOrgRepository;
import edu.austincollege.sstation.repository.ServiceHourRepository;
import edu.austincollege.sstation.repository.StudentRepository;
import edu.austincollege.sstation.service.StudentData.Dashboard;
import edu.austincollege.sstation.service.StudentData.Report;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Coverage for {@link StudentStatsService} (TC-105c). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StudentStatsServiceTest {

  @Autowired private ServiceHourRepository serviceHours;
  @Autowired private StudentRepository students;
  @Autowired private CampusOrgRepository campusOrgs;

  private StudentStatsService service;
  private int currentYear;
  private Student sam;
  private CampusOrg think;

  @BeforeEach
  void setUp() {
    service = new StudentStatsService(serviceHours, campusOrgs);
    currentYear = LocalDate.now().getYear();
    sam = students.save(student());
    think = campusOrgs.save(campusOrg());
  }

  @Test
  void dashboardSumsByStatusAndSortsHoursNewestFirst() {
    save(Status.APPROVED, 2.0, LocalDateTime.of(currentYear, 3, 10, 9, 0), think);
    save(Status.APPROVED, 3.0, LocalDateTime.of(currentYear, 5, 10, 9, 0), think);
    save(Status.PENDING, 4.0, LocalDateTime.of(currentYear, 6, 10, 9, 0), think);
    save(Status.REJECTED, 1.0, LocalDateTime.of(currentYear, 1, 10, 9, 0), think);

    Dashboard d = service.dashboard(sam);

    assertThat(d.studentName()).isEqualTo("Sam Student");
    assertThat(d.grandTotal()).isEqualTo(10.0);
    assertThat(d.approvedTotal()).isEqualTo(5.0);
    assertThat(d.pendingTotal()).isEqualTo(4.0);
    assertThat(d.rejectedTotal()).isEqualTo(1.0);
    assertThat(d.hours()).hasSize(4);
    // newest first
    assertThat(d.hours().get(0).startTime()).isEqualTo(LocalDateTime.of(currentYear, 6, 10, 9, 0));
  }

  @Test
  void dashboardIsNullSafeForMissingFkNames() {
    ServiceHour h = save(Status.APPROVED, 2.0, LocalDateTime.now(), null);
    h.setOtherCamOrg("Unlisted campus org");
    serviceHours.save(h);

    Dashboard d = service.dashboard(sam);

    assertThat(d.hours()).hasSize(1);
    assertThat(d.hours().get(0).eventName()).isEqualTo("—"); // null event
    assertThat(d.hours().get(0).campusOrgName()).isEqualTo("Unlisted campus org"); // fallback
    assertThat(d.hours().get(0).communityAgencyName()).isEqualTo("—");
  }

  @Test
  void reportBucketsApprovedHoursBySemester() {
    save(Status.APPROVED, 2.0, LocalDateTime.of(currentYear, 3, 10, 9, 0), think); // Spring
    save(Status.APPROVED, 1.5, LocalDateTime.of(currentYear, 10, 10, 9, 0), think); // Fall
    save(Status.PENDING, 9.0, LocalDateTime.of(currentYear, 3, 10, 9, 0), think); // not approved

    Report r = service.report(sam);

    assertThat(r.bySemester())
        .anySatisfy(
            b -> {
              assertThat(b.label()).isEqualTo(currentYear + " SP");
              assertThat(b.hours()).isEqualTo(2.0);
            })
        .anySatisfy(
            b -> {
              assertThat(b.label()).isEqualTo(currentYear + " FA");
              assertThat(b.hours()).isEqualTo(1.5);
            });
  }

  @Test
  void reportBucketsApprovedHoursByCampusOrg() {
    save(Status.APPROVED, 2.0, LocalDateTime.now(), think);
    save(Status.APPROVED, 3.0, LocalDateTime.now(), think);

    Report r = service.report(sam);

    assertThat(r.byCampusOrg())
        .anySatisfy(
            b -> {
              assertThat(b.label()).isEqualTo("THINK");
              assertThat(b.hours()).isEqualTo(5.0);
              assertThat(b.count()).isEqualTo(2);
            });
  }

  // --- helpers ---

  private ServiceHour save(Status status, double duration, LocalDateTime start, CampusOrg org) {
    ServiceHour h = new ServiceHour();
    h.setDescription("service");
    h.setStatus(status);
    h.setDuration(duration);
    h.setStartTime(start);
    h.setLastModified(LocalDateTime.now());
    h.setStudent(sam);
    h.setCampusOrg(org);
    return serviceHours.save(h);
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

  private static CampusOrg campusOrg() {
    CampusOrg o = new CampusOrg();
    o.setName("THINK");
    o.setDescription("desc");
    o.setContact("Jane Doe");
    o.setContactPhone("9038132000");
    o.setContactEmail("jdoe@austincollege.edu");
    return o;
  }
}
