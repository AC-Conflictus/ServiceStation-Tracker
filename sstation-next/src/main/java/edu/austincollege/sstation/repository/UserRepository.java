package edu.austincollege.sstation.repository;

import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);

  boolean existsByUsername(String username);

  /**
   * The student profile linked to a username, fully loaded — avoids touching the LAZY {@code
   * User.student} proxy outside its session (LazyInitializationException). Empty when the account
   * has no student profile (admin/moderator).
   */
  @Query("select u.student from User u where u.username = :username")
  Optional<Student> findStudentByUsername(@Param("username") String username);

  /**
   * Clears the student FK on any user linked to the given student, before the student is deleted.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update User u set u.student = null where u.student = :student")
  int detachStudent(@Param("student") Student student);
}
