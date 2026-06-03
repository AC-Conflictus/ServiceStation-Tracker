package edu.austincollege.sstation.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.ServiceHourAuditLog;
import edu.austincollege.sstation.domain.Status;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.domain.User;
import edu.austincollege.sstation.repository.ServiceHourAuditLogRepository;
import edu.austincollege.sstation.repository.ServiceHourRepository;
import edu.austincollege.sstation.repository.StudentRepository;
import edu.austincollege.sstation.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/** Coverage for {@link AuditService} and the audit-log cascade (TC-106c). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuditServiceTest {

  @Autowired private ServiceHourAuditLogRepository auditLogs;
  @Autowired private ServiceHourRepository serviceHours;
  @Autowired private StudentRepository students;
  @Autowired private UserRepository users;
  @Autowired private TestEntityManager em;

  private AuditService service;
  private ServiceHour hour;

  @BeforeEach
  void setUp() {
    service = new AuditService(auditLogs, users);
    Student sam = students.save(student());
    hour = serviceHours.save(hour(sam));
    users.save(new User("admin", "{noop}x"));
  }

  @Test
  void recordResolvesActorAndStoresTransition() {
    service.record(hour, Status.PENDING, Status.APPROVED, "admin", "Quick status change");

    List<ServiceHourAuditLog> history = service.historyFor(hour);
    assertThat(history).hasSize(1);
    ServiceHourAuditLog entry = history.get(0);
    assertThat(entry.getActor()).isNotNull();
    assertThat(entry.getActor().getUsername()).isEqualTo("admin");
    assertThat(entry.getFromStatus()).isEqualTo(Status.PENDING);
    assertThat(entry.getToStatus()).isEqualTo(Status.APPROVED);
    assertThat(entry.getNote()).isEqualTo("Quick status change");
  }

  @Test
  void unknownActorIsRecordedAsNullNotAnError() {
    service.record(hour, null, Status.PENDING, "ghost", "Service hour created");

    ServiceHourAuditLog entry = service.historyFor(hour).get(0);
    assertThat(entry.getActor()).isNull();
    assertThat(entry.getFromStatus()).isNull();
    assertThat(entry.getToStatus()).isEqualTo(Status.PENDING);
  }

  @Test
  void historyCollectsAllEntriesForTheHour() {
    service.record(hour, null, Status.PENDING, "admin", "created");
    service.record(hour, Status.PENDING, Status.APPROVED, "admin", "approved");
    assertThat(service.historyFor(hour)).hasSize(2);
  }

  @Test
  void deletingTheServiceHourCascadesItsAuditRows() {
    service.record(hour, null, Status.PENDING, "admin", "created");
    assertThat(auditLogs.count()).isEqualTo(1);

    serviceHours.deleteById(hour.getId());
    em.flush(); // push the service_hours delete so the DB ON DELETE CASCADE fires
    em.clear();

    assertThat(auditLogs.count()).isZero(); // ON DELETE CASCADE removed the audit rows
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

  private static ServiceHour hour(Student s) {
    ServiceHour h = new ServiceHour();
    h.setDescription("service");
    h.setStatus(Status.PENDING);
    h.setDuration(2.0);
    h.setStartTime(LocalDateTime.now());
    h.setLastModified(LocalDateTime.now());
    h.setStudent(s);
    return h;
  }
}
