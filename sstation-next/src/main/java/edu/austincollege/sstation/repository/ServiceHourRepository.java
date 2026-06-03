package edu.austincollege.sstation.repository;

import edu.austincollege.sstation.domain.CampusOrg;
import edu.austincollege.sstation.domain.CommunityAgency;
import edu.austincollege.sstation.domain.Event;
import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.Status;
import edu.austincollege.sstation.domain.Student;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceHourRepository extends JpaRepository<ServiceHour, Long> {

  List<ServiceHour> findByStatus(Status status);

  List<ServiceHour> findByStudent(Student student);

  long countByStatus(Status status);

  // Fetch-joined variants for the admin list/pending views, which render the (LAZY) student/event/
  // org/agency associations. open-in-view is off, so they must be loaded inside the query.

  @Query(
      "select distinct s from ServiceHour s "
          + "left join fetch s.student left join fetch s.event "
          + "left join fetch s.campusOrg left join fetch s.commAg")
  List<ServiceHour> findAllWithRefs();

  @Query(
      "select distinct s from ServiceHour s "
          + "left join fetch s.student left join fetch s.event "
          + "left join fetch s.campusOrg left join fetch s.commAg where s.status = :status")
  List<ServiceHour> findByStatusWithRefs(@Param("status") Status status);

  @Query("select s from ServiceHour s left join fetch s.student where s.id = :id")
  Optional<ServiceHour> findByIdWithStudent(@Param("id") Long id);

  // When a reference entity is deleted we null the (nullable) FK on its service hours rather than
  // cascade-deleting the hours like the Grails app did (TC-106 decision — avoids silent data loss).

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update ServiceHour s set s.event = null where s.event = :event")
  int detachEvent(@Param("event") Event event);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update ServiceHour s set s.campusOrg = null where s.campusOrg = :org")
  int detachCampusOrg(@Param("org") CampusOrg org);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update ServiceHour s set s.commAg = null where s.commAg = :agency")
  int detachCommunityAgency(@Param("agency") CommunityAgency agency);
}
