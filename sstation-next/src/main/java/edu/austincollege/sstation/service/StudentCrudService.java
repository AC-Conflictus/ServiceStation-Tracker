package edu.austincollege.sstation.service;

import edu.austincollege.sstation.repository.StudentRepository;
import edu.austincollege.sstation.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional student deletion (TC-106b). Before removing the student we clear the {@code
 * User.student} FK on any login linked to them (otherwise the FK constraint would block the
 * delete); the student's own service hours are then removed by the {@code cascade = ALL /
 * orphanRemoval} mapping on {@code Student.serviceHours}.
 */
@Service
public class StudentCrudService {

  private final StudentRepository students;
  private final UserRepository users;

  public StudentCrudService(StudentRepository students, UserRepository users) {
    this.students = students;
    this.users = users;
  }

  @Transactional
  public void deleteStudent(Long id) {
    students
        .findById(id)
        .ifPresent(
            student -> {
              // Clear the User.student FK first. This bulk update clears the persistence context
              // (so any loaded User is re-read), which also detaches `student`...
              users.detachStudent(student);
              // ...so delete by id to re-load a managed student; em.remove then cascades to the
              // student's own service hours (cascade = ALL / orphanRemoval).
              students.deleteById(id);
            });
  }
}
