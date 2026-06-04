package edu.austincollege.sstation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * A student's sign-up for a service {@link Event} (TC-026 / TC-108f). The office later marks the
 * outcome ({@link SignupStatus}); attended sign-ups can be converted one-click into {@link
 * ServiceHour} records. A student can sign up for a given event only once (unique constraint).
 */
@Entity
@Table(
    name = "event_signups",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_signup_student_event",
            columnNames = {"student_id", "event_id"}))
public class EventSignup {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id", nullable = false)
  private Student student;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id", nullable = false)
  private Event event;

  @Column(name = "signup_time", nullable = false)
  private LocalDateTime signupTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SignupStatus status;

  /** True once this sign-up has been converted to a ServiceHour — prevents double-conversion. */
  @Column(nullable = false)
  private boolean converted = false;

  protected EventSignup() {}

  public EventSignup(Student student, Event event, LocalDateTime signupTime, SignupStatus status) {
    this.student = student;
    this.event = event;
    this.signupTime = signupTime;
    this.status = status;
  }

  public Long getId() {
    return id;
  }

  public Student getStudent() {
    return student;
  }

  public Event getEvent() {
    return event;
  }

  public LocalDateTime getSignupTime() {
    return signupTime;
  }

  public SignupStatus getStatus() {
    return status;
  }

  public void setStatus(SignupStatus status) {
    this.status = status;
  }

  public boolean isConverted() {
    return converted;
  }

  public void setConverted(boolean converted) {
    this.converted = converted;
  }
}
