package edu.austincollege.sstation.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Join entity binding a {@link User} to a {@link Role}. Ported from the Grails {@code AcUserAcRole}
 * many-to-many join, keeping the composite (user, role) primary key.
 */
@Entity
@Table(name = "user_roles")
@IdClass(UserRole.UserRoleId.class)
public class UserRole {

  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;

  protected UserRole() {}

  public UserRole(User user, Role role) {
    this.user = user;
    this.role = role;
  }

  public User getUser() {
    return user;
  }

  public Role getRole() {
    return role;
  }

  /** Composite-key class for {@link UserRole}. Fields match the {@code @Id} property names. */
  public static class UserRoleId implements Serializable {

    private Long user;
    private Long role;

    public UserRoleId() {}

    public UserRoleId(Long user, Long role) {
      this.user = user;
      this.role = role;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof UserRoleId that)) {
        return false;
      }
      return Objects.equals(user, that.user) && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
      return Objects.hash(user, role);
    }
  }
}
