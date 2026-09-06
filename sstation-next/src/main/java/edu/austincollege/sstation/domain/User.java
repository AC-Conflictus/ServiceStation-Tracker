package edu.austincollege.sstation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/**
 * An authentication principal. Renamed from Grails {@code AcUser}.
 *
 * <p>Two Grails bugs are fixed here at the source:
 *
 * <ul>
 *   <li><b>TC-009</b>: a real {@link ManyToOne} FK to {@link Student} replaces the old email-string
 *       match (nullable — admin/moderator accounts have no student profile).
 *   <li><b>TC-008</b>: this entity no longer encodes its own password. Encoding is the security
 *       layer's job (BCrypt in TC-104); there is no plaintext fallback to leak.
 * </ul>
 */
@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false, unique = true)
  private String username;

  /** BCrypt-encoded password hash. Never a plaintext value. */
  @NotBlank
  @Column(nullable = false)
  private String password;

  @Column(nullable = false)
  private boolean enabled = true;

  @Column(name = "account_expired", nullable = false)
  private boolean accountExpired = false;

  @Column(name = "account_locked", nullable = false)
  private boolean accountLocked = false;

  @Column(name = "password_expired", nullable = false)
  private boolean passwordExpired = false;

  /**
   * TC-121: forces the holder onto {@code /change-password} before they can use anything else.
   *
   * <p>Distinct from {@link #passwordExpired}, which maps to Spring Security's {@code
   * credentialsExpired} and fails authentication outright. This one lets the user in and then
   * corners them, which is the only workable behaviour for an account whose password arrived in an
   * environment variable.
   */
  @Column(name = "must_change_password", nullable = false)
  private boolean mustChangePassword = false;

  /** The real FK that the Grails app lacked (TC-009). Null for non-student accounts. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "student_id")
  private Student student;

  protected User() {}

  public User(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isAccountExpired() {
    return accountExpired;
  }

  public void setAccountExpired(boolean accountExpired) {
    this.accountExpired = accountExpired;
  }

  public boolean isAccountLocked() {
    return accountLocked;
  }

  public void setAccountLocked(boolean accountLocked) {
    this.accountLocked = accountLocked;
  }

  public boolean isPasswordExpired() {
    return passwordExpired;
  }

  public void setPasswordExpired(boolean passwordExpired) {
    this.passwordExpired = passwordExpired;
  }

  public boolean isMustChangePassword() {
    return mustChangePassword;
  }

  public void setMustChangePassword(boolean mustChangePassword) {
    this.mustChangePassword = mustChangePassword;
  }

  public Student getStudent() {
    return student;
  }

  public void setStudent(Student student) {
    this.student = student;
  }

  @Override
  public String toString() {
    return username;
  }
}
