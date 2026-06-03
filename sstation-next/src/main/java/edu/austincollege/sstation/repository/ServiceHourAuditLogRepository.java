package edu.austincollege.sstation.repository;

import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.ServiceHourAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceHourAuditLogRepository extends JpaRepository<ServiceHourAuditLog, Long> {

  /** Audit entries for a hour, newest first, with the (LAZY) actor fetched for the view. */
  @Query(
      "select a from ServiceHourAuditLog a left join fetch a.actor "
          + "where a.serviceHour = :hour order by a.timestamp desc")
  List<ServiceHourAuditLog> findByServiceHourWithActor(@Param("hour") ServiceHour hour);
}
