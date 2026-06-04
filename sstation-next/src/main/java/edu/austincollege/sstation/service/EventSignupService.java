package edu.austincollege.sstation.service;

import edu.austincollege.sstation.domain.Event;
import edu.austincollege.sstation.domain.EventSignup;
import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.SignupStatus;
import edu.austincollege.sstation.domain.Status;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.repository.EventSignupRepository;
import edu.austincollege.sstation.repository.ServiceHourRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event sign-up flow (TC-026 / TC-108f): students sign up for events, the office marks attendance,
 * and attended sign-ups are converted one-click into {@link ServiceHour} records.
 */
@Service
public class EventSignupService {

  private final EventSignupRepository signups;
  private final ServiceHourRepository serviceHours;
  private final AuditService audit;

  public EventSignupService(
      EventSignupRepository signups, ServiceHourRepository serviceHours, AuditService audit) {
    this.signups = signups;
    this.serviceHours = serviceHours;
    this.audit = audit;
  }

  /** Signs a student up for an event. No-op (returns false) if they already signed up. */
  @Transactional
  public boolean signUp(Student student, Event event) {
    if (signups.existsByStudentAndEvent(student, event)) {
      return false;
    }
    signups.save(new EventSignup(student, event, LocalDateTime.now(), SignupStatus.SIGNED_UP));
    return true;
  }

  @Transactional(readOnly = true)
  public List<EventSignup> signupsForStudent(Student student) {
    return signups.findByStudentWithEvent(student);
  }

  @Transactional(readOnly = true)
  public List<EventSignup> roster(Event event) {
    return signups.findByEventWithStudent(event);
  }

  /** Sets the outcome (SIGNED_UP / ATTENDED / NO_SHOW) on a single sign-up. */
  @Transactional
  public void updateStatus(Long signupId, SignupStatus status) {
    EventSignup signup = signups.findById(signupId).orElseThrow();
    signup.setStatus(status);
    signups.save(signup);
  }

  /**
   * Converts every attended, not-yet-converted sign-up for an event into a PENDING ServiceHour
   * (duration 0 — the office fills in the real hours when reviewing). Each created hour is audited.
   * Returns the number converted.
   */
  @Transactional
  public int convertAttendedToHours(Event event, String actorUsername) {
    List<EventSignup> attended =
        signups.findUnconvertedByEventAndStatus(event, SignupStatus.ATTENDED);
    LocalDateTime now = LocalDateTime.now();
    for (EventSignup signup : attended) {
      ServiceHour hour = new ServiceHour();
      hour.setStudent(signup.getStudent());
      hour.setEvent(event);
      hour.setStatus(Status.PENDING);
      hour.setDuration(0.0);
      hour.setStartTime(signup.getSignupTime());
      hour.setLastModified(now);
      hour.setDescription("Attended event: " + event.getName());
      serviceHours.save(hour);
      audit.record(hour, null, Status.PENDING, actorUsername, "Created from event sign-up");

      signup.setConverted(true);
      signups.save(signup);
    }
    return attended.size();
  }
}
