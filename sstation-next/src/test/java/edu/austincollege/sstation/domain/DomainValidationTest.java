package edu.austincollege.sstation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Bean Validation coverage for the ported entities (TC-103). Pure validator, no Spring context. */
class DomainValidationTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  @Test
  void validRoleHasNoViolations() {
    assertThat(validator.validate(new Role("ROLE_ADMIN"))).isEmpty();
  }

  @Test
  void blankRoleAuthorityIsRejected() {
    Set<ConstraintViolation<Role>> violations = validator.validate(new Role("  "));
    assertThat(violations)
        .extracting(ConstraintViolation::getPropertyPath)
        .extracting(Object::toString)
        .contains("authority");
  }

  @Test
  void validUserHasNoViolations() {
    assertThat(validator.validate(new User("admin", "{bcrypt}hash"))).isEmpty();
  }

  @Test
  void blankUsernameAndPasswordAreRejected() {
    Set<ConstraintViolation<User>> violations = validator.validate(new User("", ""));
    assertThat(violations)
        .extracting(v -> v.getPropertyPath().toString())
        .contains("username", "password");
  }

  @Test
  void validStudentHasNoViolations() {
    assertThat(validator.validate(validStudent())).isEmpty();
  }

  @Test
  void studentRejectsBlankNamesNullStatusAndBadEmail() {
    Student s = validStudent();
    s.setFirstname(" ");
    s.setLastname(" ");
    s.setStatus(null);
    s.setAcEmail("not-an-email");
    assertThat(validator.validate(s))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("firstname", "lastname", "status", "acEmail");
  }

  @Test
  void validServiceHourHasNoViolations() {
    assertThat(validator.validate(validServiceHour())).isEmpty();
  }

  @Test
  void serviceHourRejectsNullStatusAndTimes() {
    ServiceHour h = validServiceHour();
    h.setStatus(null);
    h.setStartTime(null);
    h.setLastModified(null);
    assertThat(validator.validate(h))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("status", "startTime", "lastModified");
  }

  @Test
  void validEventHasNoViolations() {
    assertThat(validator.validate(validEvent())).isEmpty();
  }

  @Test
  void eventRejectsBadEmailAndBlankName() {
    Event e = validEvent();
    e.setName(" ");
    e.setContactEmail("nope");
    assertThat(validator.validate(e))
        .extracting(v -> v.getPropertyPath().toString())
        .contains("name", "contactEmail");
  }

  private static Student validStudent() {
    Student s = new Student();
    s.setFirstname("Sam");
    s.setLastname("Student");
    s.setAcid("AC50000");
    s.setAcEmail("student@austincollege.edu");
    s.setStatus('A');
    s.setClassification(Classification.JR);
    s.setIsModerator(false);
    return s;
  }

  private static ServiceHour validServiceHour() {
    ServiceHour h = new ServiceHour();
    h.setDescription("service");
    h.setStatus(Status.PENDING);
    h.setDuration(2.5);
    h.setStartTime(LocalDateTime.now().minusDays(1));
    h.setLastModified(LocalDateTime.now());
    h.setStudent(validStudent());
    return h;
  }

  private static Event validEvent() {
    Event e = new Event();
    e.setName("Great Day of Service");
    e.setDescription("Contact the Service Station office to sign up!");
    e.setContact("Jane Doe");
    e.setContactPhone("9038132000");
    e.setContactEmail("jdoe@austincollege.edu");
    return e;
  }
}
