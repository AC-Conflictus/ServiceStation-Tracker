package edu.austincollege.sstation.repository;

import edu.austincollege.sstation.domain.SiteAvailability;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface SiteAvailabilityRepository extends JpaRepository<SiteAvailability, Long> {
  Optional<SiteAvailability> findBySyncKey(String syncKey);

  List<SiteAvailability> findBySiteIdIn(List<Long> ids);

  // One database operation: a delayed request cannot replace a newer observation or a rotated key.
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update SiteAvailability a set a.capacity = :capacity, a.remaining = :remaining,
      a.acceptingResponses = :accepting, a.observedAt = :observedAt
      where a.siteId = :siteId and a.syncKey = :syncKey
      and (a.observedAt is null or a.observedAt < :observedAt)
      """)
  int observe(
      Long siteId,
      String syncKey,
      int capacity,
      int remaining,
      boolean accepting,
      Instant observedAt);
}
