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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * The central transactional record: one chunk of service hours logged by a {@link Student}. Ported
 * from Grails {@code ServiceHour}.
 *
 * <p>Per the TC-103 decision, the {@link #campusOrg}, {@link #commAg} and {@link #event} FKs remain
 * <b>nullable</b> (matching the Grails constraints and the {@code otherCamOrg}/{@code otherCommAg}
 * free-text escape hatches). The owning {@link #student} is required ({@code belongsTo}).
 */
@Entity
@Table(name = "service_hours")
public class ServiceHour {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 10000)
  private String description;

  @Column(name = "event_contact_name")
  private String eventContactName;

  @Column(name = "event_contact_phone")
  private String eventContactPhone;

  @Email
  @Column(name = "event_contact_email")
  private String eventContactEmail;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status;

  @Column(nullable = false)
  private double duration;

  @NotNull
  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @NotNull
  @Column(name = "last_modified", nullable = false)
  private LocalDateTime lastModified;

  /** Owning student — required ({@code belongsTo} in Grails). */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id", nullable = false)
  private Student student;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "campus_org_id")
  private CampusOrg campusOrg;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "comm_ag_id")
  private CommunityAgency commAg;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id")
  private Event event;

  @Column(name = "other_cam_org")
  private String otherCamOrg;

  @Column(name = "other_comm_ag")
  private String otherCommAg;

  public Long getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getEventContactName() {
    return eventContactName;
  }

  public void setEventContactName(String eventContactName) {
    this.eventContactName = eventContactName;
  }

  public String getEventContactPhone() {
    return eventContactPhone;
  }

  public void setEventContactPhone(String eventContactPhone) {
    this.eventContactPhone = eventContactPhone;
  }

  public String getEventContactEmail() {
    return eventContactEmail;
  }

  public void setEventContactEmail(String eventContactEmail) {
    this.eventContactEmail = eventContactEmail;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public double getDuration() {
    return duration;
  }

  public void setDuration(double duration) {
    this.duration = duration;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public LocalDateTime getLastModified() {
    return lastModified;
  }

  public void setLastModified(LocalDateTime lastModified) {
    this.lastModified = lastModified;
  }

  public Student getStudent() {
    return student;
  }

  public void setStudent(Student student) {
    this.student = student;
  }

  public CampusOrg getCampusOrg() {
    return campusOrg;
  }

  public void setCampusOrg(CampusOrg campusOrg) {
    this.campusOrg = campusOrg;
  }

  public CommunityAgency getCommAg() {
    return commAg;
  }

  public void setCommAg(CommunityAgency commAg) {
    this.commAg = commAg;
  }

  public Event getEvent() {
    return event;
  }

  public void setEvent(Event event) {
    this.event = event;
  }

  public String getOtherCamOrg() {
    return otherCamOrg;
  }

  public void setOtherCamOrg(String otherCamOrg) {
    this.otherCamOrg = otherCamOrg;
  }

  public String getOtherCommAg() {
    return otherCommAg;
  }

  public void setOtherCommAg(String otherCommAg) {
    this.otherCommAg = otherCommAg;
  }
}
