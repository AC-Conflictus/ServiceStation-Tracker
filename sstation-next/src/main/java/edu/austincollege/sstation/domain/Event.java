package edu.austincollege.sstation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * A service event (e.g. "Great Day of Service", "JanServe"). Ported from Grails {@code Event}; all
 * original descriptive/contact fields are required; the default site schedule is optional.
 */
@Entity
@Table(name = "events")
public class Event {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false)
  private String name;

  @NotBlank
  @Column(nullable = false, length = 10000)
  private String description;

  @NotBlank
  @Column(nullable = false)
  private String contact;

  @NotBlank
  @Column(nullable = false)
  private String contactPhone;

  @NotBlank
  @Email
  @Column(nullable = false)
  private String contactEmail;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EventRegistrationMode registrationMode = EventRegistrationMode.DIRECT;

  public EventRegistrationMode getRegistrationMode() {
    return registrationMode;
  }

  public void setRegistrationMode(EventRegistrationMode mode) {
    registrationMode = mode;
  }

  public boolean isSiteRegistration() {
    return registrationMode == EventRegistrationMode.SITE_FORMS;
  }

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime startsAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime endsAt;

  @AssertTrue(message = "Set both start and end, with end after start, or leave both blank.")
  public boolean isScheduleValid() {
    return startsAt == null && endsAt == null
        || startsAt != null && endsAt != null && endsAt.isAfter(startsAt);
  }

  public LocalDateTime getStartsAt() {
    return startsAt;
  }

  public void setStartsAt(LocalDateTime value) {
    startsAt = value;
  }

  public LocalDateTime getEndsAt() {
    return endsAt;
  }

  public void setEndsAt(LocalDateTime value) {
    endsAt = value;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getContact() {
    return contact;
  }

  public void setContact(String contact) {
    this.contact = contact;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public void setContactPhone(String contactPhone) {
    this.contactPhone = contactPhone;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  @Override
  public String toString() {
    return name;
  }
}
