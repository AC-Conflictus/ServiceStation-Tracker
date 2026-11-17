package edu.austincollege.sstation.repository;

import edu.austincollege.sstation.domain.EventSite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventSiteRepository extends JpaRepository<EventSite, Long> {
  List<EventSite> findByEventIdOrderByDisplayOrderAscNameAsc(Long eventId);

  Optional<EventSite> findByIdAndEventId(Long id, Long eventId);
}
