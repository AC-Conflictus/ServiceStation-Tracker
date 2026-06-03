package edu.austincollege.sstation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * A community / non-profit agency that service is performed for. Renamed from the Grails {@code
 * CommAg} domain class (per the Lane 7 rename map); adds {@code address} which {@code CampusOrg}
 * lacks.
 */
@Entity
@Table(name = "community_agencies")
public class CommunityAgency {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank
  @Column(nullable = false)
  private String name;

  @NotBlank
  @Column(nullable = false)
  private String address;

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

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
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
