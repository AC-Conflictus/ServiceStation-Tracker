package edu.austincollege.sstation.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.Status;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.domain.User;
import edu.austincollege.sstation.repository.ServiceHourRepository;
import edu.austincollege.sstation.repository.StudentRepository;
import edu.austincollege.sstation.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Deleting a student detaches any linked login (clears User.student) and cascade-removes the
 * student's own service hours (TC-106b).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StudentCrudServiceTest {

  @Autowired private StudentRepository students;
  @Autowired private UserRepository users;
  @Autowired private ServiceHourRepository serviceHours;

  private StudentCrudService service;

  @BeforeEach
  void setUp() {
    service = new StudentCrudService(students, users);
  }

  @Test
  void deletingStudentClearsLinkedUserFkAndRemovesTheirHours() {
    Student sam = students.save(student());
    ServiceHour hour = serviceHours.save(hour(sam));
    User login = new User("student", "{noop}x");
    login.setStudent(sam);
    Long userId = users.save(login).getId();

    service.deleteStudent(sam.getId());

    assertThat(students.findById(sam.getId())).isEmpty();
    assertThat(serviceHours.findById(hour.getId())).isEmpty(); // cascaded
    assertThat(users.findById(userId)).isPresent(); // user kept
    assertThat(users.findById(userId).orElseThrow().getStudent()).isNull(); // FK cleared
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
    h.setStatus(Status.APPROVED);
    h.setDuration(2.0);
    h.setStartTime(LocalDateTime.now());
    h.setLastModified(LocalDateTime.now());
    h.setStudent(s);
    return h;
  }
}
