package edu.austincollege.sstation.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;

/**
 * Generic contact info.
 *
 * <p>NOTE: the Grails {@code Contact} domain class was <em>empty</em> (no fields) and is not
 * referenced anywhere — {@code ServiceHour} stores contact details as flat strings instead. It is
 * ported here to keep the entity set complete (TC-103) and is given the obvious name/phone/email
 * shape so it is usable if a future card wires it in. Currently unreferenced; safe to drop if it
 * stays unused through the rewrite. Flagged for the dev.
 */
@Entity
@Table(name = "contacts")
public class Contact {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String phone;

  @Email private String email;

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
