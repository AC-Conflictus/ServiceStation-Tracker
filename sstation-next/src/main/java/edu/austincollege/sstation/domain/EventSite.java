package edu.austincollege.sstation.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/** A nonprofit site belonging to one event occurrence; registration is external. */
@Entity
@Table(name = "event_sites")
public class EventSite {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", nullable = false)
  private Event event;

  @NotBlank
  @Size(max = 255)
  @Column(nullable = false)
  private String name;

  @NotBlank
  @Size(max = 1000)
  @Column(nullable = false, length = 1000)
  private String location;

  @Size(max = 5000)
  @Column(length = 5000)
  private String description;

  @Size(max = 5000)
  @Column(length = 5000)
  private String requirements;

  @Size(max = 2000)
  @Pattern(regexp = "^$|https://[^\\s]+", message = "Use an HTTPS link, or leave blank.")
  @Column(length = 2000)
  private String registrationUrl;

  @Min(0)
  private int displayOrder;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime startsAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime endsAt;

  public Long getId() {
    return id;
  }

  public Event getEvent() {
    return event;
  }

  public void setEvent(Event event) {
    this.event = event;
  }

  @AssertTrue(
      message =
          "Set both override times, with end after start, or leave both blank to use the event schedule.")
  public boolean isScheduleValid() {
    return startsAt == null && endsAt == null
        || startsAt != null && endsAt != null && endsAt.isAfter(startsAt);
  }

  public String getName() {
    return name;
  }

  public void setName(String value) {
    name = value;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String value) {
    location = value;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String value) {
    description = value;
  }

  public String getRequirements() {
    return requirements;
  }

  public void setRequirements(String value) {
    requirements = value;
  }

  public String getRegistrationUrl() {
    return registrationUrl;
  }

  public void setRegistrationUrl(String value) {
    registrationUrl = value;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(int value) {
    displayOrder = value;
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
}
