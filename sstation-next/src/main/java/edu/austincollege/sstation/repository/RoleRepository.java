package edu.austincollege.sstation.repository;

import edu.austincollege.sstation.domain.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

  Optional<Role> findByAuthority(String authority);
}
