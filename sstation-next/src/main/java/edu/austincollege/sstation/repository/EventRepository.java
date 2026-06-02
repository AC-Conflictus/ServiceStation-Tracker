package edu.austincollege.sstation.repository;

import edu.austincollege.sstation.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {}
