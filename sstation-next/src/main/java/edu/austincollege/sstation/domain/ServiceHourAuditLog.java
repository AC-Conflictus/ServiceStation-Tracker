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
import java.time.LocalDateTime;

/**
 * One entry in the audit trail of a {@link ServiceHour}'s status changes (TC-027 / TC-106c).
 * Written on every status mutation — create, edit, and quick approve/reject. Answers "who changed
 * my hours, and from what to what?".
 */
@Entity
@Table(name = "service_hour_audit_log")
public class ServiceHourAuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "service_hour_id", nullable = false)
  private ServiceHour serviceHour;

  /** The user who made the change; null if it couldn't be resolved (e.g. a system action). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_id")
  private User actor;

  /** Previous status; null when the hour was just created. */
  @Enumerated(EnumType.STRING)
  @Column(name = "from_status", length = 20)
  private Status fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_status", nullable = false, length = 20)
  private Status toStatus;

  @Column(name = "changed_at", nullable = false)
  private LocalDateTime timestamp;

  @Column(length = 500)
  private String note;

  protected ServiceHourAuditLog() {}

  public ServiceHourAuditLog(
      ServiceHour serviceHour,
      User actor,
      Status fromStatus,
      Status toStatus,
      LocalDateTime timestamp,
      String note) {
    this.serviceHour = serviceHour;
    this.actor = actor;
    this.fromStatus = fromStatus;
    this.toStatus = toStatus;
    this.timestamp = timestamp;
    this.note = note;
  }

  public Long getId() {
    return id;
  }

  public ServiceHour getServiceHour() {
    return serviceHour;
  }

  public User getActor() {
    return actor;
  }

  public Status getFromStatus() {
    return fromStatus;
  }

  public Status getToStatus() {
    return toStatus;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public String getNote() {
    return note;
  }
}
