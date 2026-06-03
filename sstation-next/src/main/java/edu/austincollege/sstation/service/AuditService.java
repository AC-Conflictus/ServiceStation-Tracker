package edu.austincollege.sstation.service;

import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.ServiceHourAuditLog;
import edu.austincollege.sstation.domain.Status;
import edu.austincollege.sstation.domain.User;
import edu.austincollege.sstation.repository.ServiceHourAuditLogRepository;
import edu.austincollege.sstation.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes and reads the {@link ServiceHourAuditLog} (TC-027 / TC-106c). Every status mutation on a
 * service hour should go through {@link #record}.
 */
@Service
public class AuditService {

  private final ServiceHourAuditLogRepository auditLogs;
  private final UserRepository users;

  public AuditService(ServiceHourAuditLogRepository auditLogs, UserRepository users) {
    this.auditLogs = auditLogs;
    this.users = users;
  }

  /**
   * Records a status change. {@code from} is null when the hour was just created. {@code
   * actorUsername} is resolved to a {@link User}; an unknown name records a null actor rather than
   * failing.
   */
  @Transactional
  public void record(ServiceHour hour, Status from, Status to, String actorUsername, String note) {
    User actor = actorUsername == null ? null : users.findByUsername(actorUsername).orElse(null);
    auditLogs.save(new ServiceHourAuditLog(hour, actor, from, to, LocalDateTime.now(), note));
  }

  @Transactional(readOnly = true)
  public List<ServiceHourAuditLog> historyFor(ServiceHour hour) {
    return auditLogs.findByServiceHourWithActor(hour);
  }
}
