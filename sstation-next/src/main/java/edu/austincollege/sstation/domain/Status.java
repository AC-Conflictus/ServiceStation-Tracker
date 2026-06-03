package edu.austincollege.sstation.domain;

/**
 * Lifecycle of a {@link ServiceHour} record. Ported 1:1 from the Grails {@code sstation.Status}
 * enum; persisted as its name via {@code @Enumerated(EnumType.STRING)}.
 */
public enum Status {
  PENDING,
  APPROVED,
  REJECTED
}
