package edu.austincollege.sstation.web;

import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.Status;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Form-backing object for creating/editing a {@link ServiceHour} (TC-106c). Kept separate from the
 * entity so the FK selects can bind as ids and the {@code datetime-local} input can bind to {@link
 * LocalDateTime}, without leaking web concerns into the JPA entity.
 */
public class ServiceHourForm {

  @NotNull private Long studentId;

  @NotNull private String description;

  @Positive private double duration;

  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime startTime;

  @NotNull private Status status;

  // Nullable FK selects + free-text fallbacks (mirrors the Grails other* fields).
  private Long eventId;
  private Long campusOrgId;
  private Long commAgId;
  private String otherCamOrg;
  private String otherCommAg;

  private String eventContactName;
  private String eventContactPhone;

  @Email private String eventContactEmail;

  /** Builds a form pre-populated from an existing entity (for the edit screen). */
  public static ServiceHourForm fromEntity(ServiceHour h) {
    ServiceHourForm f = new ServiceHourForm();
    f.studentId = h.getStudent() != null ? h.getStudent().getId() : null;
    f.description = h.getDescription();
    f.duration = h.getDuration();
    f.startTime = h.getStartTime();
    f.status = h.getStatus();
    f.eventId = h.getEvent() != null ? h.getEvent().getId() : null;
    f.campusOrgId = h.getCampusOrg() != null ? h.getCampusOrg().getId() : null;
    f.commAgId = h.getCommAg() != null ? h.getCommAg().getId() : null;
    f.otherCamOrg = h.getOtherCamOrg();
    f.otherCommAg = h.getOtherCommAg();
    f.eventContactName = h.getEventContactName();
    f.eventContactPhone = h.getEventContactPhone();
    f.eventContactEmail = h.getEventContactEmail();
    return f;
  }

  public Long getStudentId() {
    return studentId;
  }

  public void setStudentId(Long studentId) {
    this.studentId = studentId;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Long getEventId() {
    return eventId;
  }

  public void setEventId(Long eventId) {
    this.eventId = eventId;
  }

  public Long getCampusOrgId() {
    return campusOrgId;
  }

  public void setCampusOrgId(Long campusOrgId) {
    this.campusOrgId = campusOrgId;
  }

  public Long getCommAgId() {
    return commAgId;
  }

  public void setCommAgId(Long commAgId) {
    this.commAgId = commAgId;
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
}
