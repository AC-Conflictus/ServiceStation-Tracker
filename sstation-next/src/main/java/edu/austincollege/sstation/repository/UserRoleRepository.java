package edu.austincollege.sstation.repository;

import edu.austincollege.sstation.domain.User;
import edu.austincollege.sstation.domain.UserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.UserRoleId> {

  /**
   * Authority strings granted to a user. Eager projection so the security layer avoids lazy init.
   */
  @Query("select ur.role.authority from UserRole ur where ur.user = :user")
  List<String> findAuthoritiesByUser(@Param("user") User user);

  boolean existsByUserAndRole(User user, edu.austincollege.sstation.domain.Role role);
}
