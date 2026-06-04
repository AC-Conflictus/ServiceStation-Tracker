package edu.austincollege.sstation.domain;

/**
 * Lifecycle of an {@link EventSignup} (TC-026 / TC-108f): a student signs up, then the office marks
 * whether they attended. Persisted as its name via {@code @Enumerated(EnumType.STRING)}.
 */
public enum SignupStatus {
  SIGNED_UP("Signed up"),
  ATTENDED("Attended"),
  NO_SHOW("No-show");

  private final String displayName;

  SignupStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
