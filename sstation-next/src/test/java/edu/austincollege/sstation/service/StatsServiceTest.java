package edu.austincollege.sstation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import edu.austincollege.sstation.domain.Classification;
import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.Status;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.repository.ServiceHourRepository;
import edu.austincollege.sstation.repository.StudentRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Unit coverage for the {@link StatsService} port of HourService (TC-105a). Runs against the real
 * Flyway schema; the service is wired by hand from the autowired repositories.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StatsServiceTest {

  @Autowired private ServiceHourRepository serviceHours;
  @Autowired private StudentRepository students;

  private StatsService stats;
  private int currentYear;

  @BeforeEach
  void setUp() {
    stats = new StatsService(serviceHours, students);
    currentYear = LocalDate.now().getYear();
  }

  @Test
  void overallKpisCountStudentsApprovedHoursAndPending() {
    Student sam = students.save(student("AC1", Classification.JR));
    // 2 approved hours this year (2.0 + 3.0), 1 pending this year (4.0)
    serviceHours.save(hour(sam, Status.APPROVED, 2.0, LocalDateTime.now().withMonth(3)));
    serviceHours.save(hour(sam, Status.APPROVED, 3.0, LocalDateTime.now().withMonth(4)));
    serviceHours.save(hour(sam, Status.PENDING, 4.0, LocalDateTime.now().withMonth(5)));

    AdminDashboardData d = stats.adminDashboard();

    assertThat(d.overall().totalStudents()).isEqualTo(1);
    assertThat(d.overall().totalHours()).isEqualTo(5.0); // approved only
    assertThat(d.overall().totalThisYear()).isEqualTo(5.0);
    assertThat(d.overall().pendingTotal()).isEqualTo(1);
    assertThat(d.overall().averagePerStudent()).isEqualTo(5.0); // 5 approved / 1 student
  }

  @Test
  void fiveYearTrendIsAscendingAndEndsOnCurrentYear() {
    Student sam = students.save(student("AC2", Classification.SR));
    serviceHours.save(hour(sam, Status.APPROVED, 6.0, LocalDateTime.now()));
    serviceHours.save(
        hour(sam, Status.APPROVED, 1.5, LocalDateTime.now().minusYears(2).withDayOfYear(100)));

    AdminDashboardData.FiveYearTrend t = stats.adminDashboard().fiveYear();

    assertThat(t.years())
        .containsExactly(
            currentYear - 4, currentYear - 3, currentYear - 2, currentYear - 1, currentYear);
    assertThat(t.totals()).hasSize(5);
    assertThat(t.totals().get(4)).isEqualTo(6.0); // current year
    assertThat(t.totals().get(2)).isEqualTo(1.5); // two years ago
  }

  @Test
  void monthlyTrendHasTwelveBuckets() {
    Student sam = students.save(student("AC3", Classification.FR));
    serviceHours.save(hour(sam, Status.APPROVED, 2.0, LocalDateTime.now().withMonth(1)));

    AdminDashboardData.MonthlyTrend m = stats.adminDashboard().monthly();

    assertThat(m.monthly()).hasSize(12);
    assertThat(m.monthly().get(0)).isEqualTo(2.0); // January
  }

  @Test
  void pieFractionsSumToOneAcrossStatuses() {
    Student sam = students.save(student("AC4", Classification.SO));
    serviceHours.save(hour(sam, Status.APPROVED, 3.0, LocalDateTime.now()));
    serviceHours.save(hour(sam, Status.REJECTED, 1.0, LocalDateTime.now()));

    var status = stats.adminDashboard().byStatus();

    double sum = status.stream().mapToDouble(AdminDashboardData.Slice::fraction).sum();
    assertThat(sum).isCloseTo(1.0, within(1e-9));
    assertThat(status).hasSize(3); // PENDING, APPROVED, REJECTED
  }

  @Test
  void emptyDatabaseProducesZerosNotErrors() {
    AdminDashboardData d = stats.adminDashboard();
    assertThat(d.overall().totalStudents()).isZero();
    assertThat(d.overall().totalHours()).isZero();
    assertThat(d.overall().averagePerStudent()).isZero();
    assertThat(d.fiveYear().totals()).hasSize(5);
    assertThat(d.byClassification()).hasSize(Classification.values().length);
  }

  private static Student student(String acid, Classification c) {
    Student s = new Student();
    s.setFirstname("Sam");
    s.setLastname("Student");
    s.setAcid(acid);
    s.setAcEmail("s@austincollege.edu");
    s.setStatus('A');
    s.setClassification(c);
    s.setIsModerator(false);
    return s;
  }

  private static ServiceHour hour(Student s, Status status, double duration, LocalDateTime start) {
    ServiceHour h = new ServiceHour();
    h.setDescription("service");
    h.setStatus(status);
    h.setDuration(duration);
    h.setStartTime(start);
    h.setLastModified(LocalDateTime.now());
    h.setStudent(s);
    return h;
  }
}
