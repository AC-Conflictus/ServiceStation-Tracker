package edu.austincollege.sstation.repository;

import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.Status;
import edu.austincollege.sstation.domain.Student;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceHourRepository extends JpaRepository<ServiceHour, Long> {

  List<ServiceHour> findByStatus(Status status);

  List<ServiceHour> findByStudent(Student student);

  long countByStatus(Status status);
}
