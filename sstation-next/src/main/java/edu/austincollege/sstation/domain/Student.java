package edu.austincollege.sstation.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * A student's service-hours profile. Renamed from Grails {@code AcStudent}. In the Grails app this
 * was coupled to the login user only by an email string; the real FK now lives on {@link User}
 * (fixes TC-009).
 */
@Entity
@Table(name = "students")
public class Student {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false)
  private String firstname;

  @NotBlank
  @Column(nullable = false)
  private String lastname;

  @NotBlank
  @Column(nullable = false, unique = true)
  private String acid;

  @NotBlank
  @Email
  @Column(name = "ac_email", nullable = false)
  private String acEmail;

  @Column(name = "ac_box")
  private String acBox;

  private String phone;

  @Column(name = "ac_year")
  private Integer acYear;

  /** Active flag, carried over verbatim from the Grails {@code Character status} field ('A'). */
  @NotNull
  @Column(nullable = false, length = 1)
  private Character status;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private Classification classification;

  @Column(name = "is_moderator")
  private Boolean isModerator = Boolean.FALSE;

  @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ServiceHour> serviceHours = new ArrayList<>();

  public Long getId() {
    return id;
  }

  public String getFirstname() {
    return firstname;
  }

  public void setFirstname(String firstname) {
    this.firstname = firstname;
  }

  public String getLastname() {
    return lastname;
  }

  public void setLastname(String lastname) {
    this.lastname = lastname;
  }

  public String getAcid() {
    return acid;
  }

  public void setAcid(String acid) {
    this.acid = acid;
  }

  public String getAcEmail() {
    return acEmail;
  }

  public void setAcEmail(String acEmail) {
    this.acEmail = acEmail;
  }

  public String getAcBox() {
    return acBox;
  }

  public void setAcBox(String acBox) {
    this.acBox = acBox;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public Integer getAcYear() {
    return acYear;
  }

  public void setAcYear(Integer acYear) {
    this.acYear = acYear;
  }

  public Character getStatus() {
    return status;
  }

  public void setStatus(Character status) {
    this.status = status;
  }

  public Classification getClassification() {
    return classification;
  }

  public void setClassification(Classification classification) {
    this.classification = classification;
  }

  public Boolean getIsModerator() {
    return isModerator;
  }

  public void setIsModerator(Boolean isModerator) {
    this.isModerator = isModerator;
  }

  public List<ServiceHour> getServiceHours() {
    return serviceHours;
  }

  /** Keeps both sides of the bidirectional association in sync (mirrors GORM addTo*). */
  public void addServiceHour(ServiceHour hour) {
    serviceHours.add(hour);
    hour.setStudent(this);
  }

  public void removeServiceHour(ServiceHour hour) {
    serviceHours.remove(hour);
    hour.setStudent(null);
  }

  public String getFullName() {
    return firstname + " " + lastname;
  }

  @Override
  public String toString() {
    return getFullName();
  }
}
