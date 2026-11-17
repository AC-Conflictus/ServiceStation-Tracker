package edu.austincollege.sstation.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Aggregate observations only; signing secrets are derived from deployment configuration. */
@Entity
@Table(name = "event_site_availability")
public class SiteAvailability {
  @Id private Long siteId;

  @Column(nullable = false, unique = true, length = 100)
  private String syncKey;

  private Integer capacity;
  private Integer remaining;
  private Boolean acceptingResponses;
  private Instant observedAt;

  protected SiteAvailability() {}

  public SiteAvailability(Long siteId, String syncKey) {
    this.siteId = siteId;
    this.syncKey = syncKey;
  }

  public Long getSiteId() {
    return siteId;
  }

  public String getSyncKey() {
    return syncKey;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public Integer getRemaining() {
    return remaining;
  }

  public Boolean getAcceptingResponses() {
    return acceptingResponses;
  }

  public Instant getObservedAt() {
    return observedAt;
  }
}
