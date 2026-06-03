package edu.austincollege.sstation.domain;

/**
 * Academic classification of a {@link Student}. Ported from the Grails {@code
 * sstation.Classification} enum (its private display name is exposed here via {@link
 * #getDisplayName()}). Persisted as its name via {@code @Enumerated(EnumType.STRING)}.
 */
public enum Classification {
  FR("Freshman"),
  SO("Sophomore"),
  JR("Junior"),
  SR("Senior"),
  GRAD("Graduate Student"),
  OTHER("Other");

  private final String displayName;

  Classification(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
