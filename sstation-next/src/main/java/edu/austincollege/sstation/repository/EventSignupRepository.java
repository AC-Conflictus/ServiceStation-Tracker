package edu.austincollege.sstation.repository;

import edu.austincollege.sstation.domain.Event;
import edu.austincollege.sstation.domain.EventSignup;
import edu.austincollege.sstation.domain.SignupStatus;
import edu.austincollege.sstation.domain.Student;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventSignupRepository extends JpaRepository<EventSignup, Long> {

  boolean existsByStudentAndEvent(Student student, Event event);

  List<EventSignup> findByStudent(Student student);

  // Fetch-joined variants for the views (open-in-view is off, so the LAZY student/event must be
  // loaded inside the query before the template walks them).

  @Query("select s from EventSignup s left join fetch s.event where s.student = :student")
  List<EventSignup> findByStudentWithEvent(@Param("student") Student student);

  @Query(
      "select s from EventSignup s left join fetch s.student where s.event = :event"
          + " order by s.signupTime")
  List<EventSignup> findByEventWithStudent(@Param("event") Event event);

  @Query("select s from EventSignup s left join fetch s.student where s.id = :id")
  Optional<EventSignup> findByIdWithStudent(@Param("id") Long id);

  @Query(
      "select s from EventSignup s left join fetch s.student"
          + " where s.event = :event and s.status = :status and s.converted = false")
  List<EventSignup> findUnconvertedByEventAndStatus(
      @Param("event") Event event, @Param("status") SignupStatus status);
}
