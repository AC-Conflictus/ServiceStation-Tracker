package edu.austincollege.sstation.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.austincollege.sstation.domain.Event;
import edu.austincollege.sstation.domain.EventSignup;
import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.SignupStatus;
import edu.austincollege.sstation.domain.Status;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.repository.EventRepository;
import edu.austincollege.sstation.repository.EventSignupRepository;
import edu.austincollege.sstation.repository.ServiceHourAuditLogRepository;
import edu.austincollege.sstation.repository.ServiceHourRepository;
import edu.austincollege.sstation.repository.StudentRepository;
import edu.austincollege.sstation.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/** Coverage for {@link EventSignupService} and the V3 schema (TC-108f). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventSignupServiceTest {

  @Autowired private EventSignupRepository signups;
  @Autowired private ServiceHourRepository serviceHours;
  @Autowired private ServiceHourAuditLogRepository auditLogs;
  @Autowired private StudentRepository students;
  @Autowired private EventRepository events;
  @Autowired private UserRepository users;

  private EventSignupService service;
  private Student sam;
  private Event event;

  @BeforeEach
  void setUp() {
    service = new EventSignupService(signups, serviceHours, new AuditService(auditLogs, users));
    sam = students.save(student());
    event = events.save(event());
  }

  @Test
  void signUpIsIdempotentPerStudentAndEvent() {
    assertThat(service.signUp(sam, event)).isTrue();
    assertThat(service.signUp(sam, event)).isFalse(); // already signed up
    assertThat(signups.findByStudent(sam)).hasSize(1);
  }

  @Test
  void convertAttendedCreatesPendingHoursAuditsAndMarksConverted() {
    service.signUp(sam, event);
    EventSignup signup = signups.findByStudent(sam).get(0);
    service.updateStatus(signup.getId(), SignupStatus.ATTENDED);

    int converted = service.convertAttendedToHours(event, "admin");

    assertThat(converted).isEqualTo(1);
    List<ServiceHour> hours = serviceHours.findByStudent(sam);
    assertThat(hours).hasSize(1);
    ServiceHour hour = hours.get(0);
    assertThat(hour.getStatus()).isEqualTo(Status.PENDING);
    assertThat(hour.getDuration()).isZero();
    assertThat(hour.getEvent().getId()).isEqualTo(event.getId());
    assertThat(auditLogs.count()).isEqualTo(1);
    assertThat(signups.findById(signup.getId()).orElseThrow().isConverted()).isTrue();
  }

  @Test
  void convertIsNotRepeatedForAlreadyConvertedSignups() {
    service.signUp(sam, event);
    EventSignup signup = signups.findByStudent(sam).get(0);
    service.updateStatus(signup.getId(), SignupStatus.ATTENDED);

    assertThat(service.convertAttendedToHours(event, "admin")).isEqualTo(1);
    assertThat(service.convertAttendedToHours(event, "admin")).isZero(); // nothing left
    assertThat(serviceHours.findByStudent(sam)).hasSize(1);
  }

  @Test
  void onlyAttendedSignupsConvert() {
    service.signUp(sam, event); // stays SIGNED_UP
    assertThat(service.convertAttendedToHours(event, "admin")).isZero();
    assertThat(serviceHours.findByStudent(sam)).isEmpty();
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

  private static Event event() {
    Event e = new Event();
    e.setName("Great Day of Service");
    e.setDescription("Annual service event");
    e.setContact("Org Anizer");
    e.setContactPhone("555-1234");
    e.setContactEmail("organizer@austincollege.edu");
    return e;
  }
}
