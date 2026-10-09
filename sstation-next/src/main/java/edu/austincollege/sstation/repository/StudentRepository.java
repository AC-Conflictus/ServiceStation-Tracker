package edu.austincollege.sstation.repository;

import edu.austincollege.sstation.domain.Student;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {

  Optional<Student> findByAcid(String acid);

  Optional<Student> findByAcEmail(String acEmail);

  /** TC-124: directories and registrar exports disagree on case, so email matching must not. */
  Optional<Student> findByAcEmailIgnoreCase(String acEmail);
}
