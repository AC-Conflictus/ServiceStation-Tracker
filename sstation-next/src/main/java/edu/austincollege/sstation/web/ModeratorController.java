package edu.austincollege.sstation.web;

import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.repository.StudentRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Promote/demote students to moderator (TC-106b). Port of the Grails {@code ModeratorController} —
 * <b>ADMIN-only</b>.
 *
 * <p>NOTE: like the Grails app, this toggles the {@code Student.isModerator} flag only; it does not
 * grant the {@code ROLE_MODERATOR} security authority (which lives on {@code User}). Wiring the
 * flag to the role system is intentionally out of scope here (parity port).
 */
@Controller
@RequestMapping("/admin/moderators")
@PreAuthorize("hasRole('ADMIN')")
public class ModeratorController {

  private final StudentRepository students;

  public ModeratorController(StudentRepository students) {
    this.students = students;
  }

  @GetMapping
  public String index(Model model) {
    List<Student> all = students.findAll();
    model.addAttribute(
        "moderators", all.stream().filter(s -> Boolean.TRUE.equals(s.getIsModerator())).toList());
    model.addAttribute(
        "candidates", all.stream().filter(s -> !Boolean.TRUE.equals(s.getIsModerator())).toList());
    return "moderators/list";
  }

  @PostMapping("/{id}/promote")
  public String promote(@PathVariable Long id, RedirectAttributes flash) {
    setModerator(id, true, flash, "promoted to");
    return "redirect:/admin/moderators";
  }

  @PostMapping("/{id}/demote")
  public String demote(@PathVariable Long id, RedirectAttributes flash) {
    setModerator(id, false, flash, "removed as");
    return "redirect:/admin/moderators";
  }

  private void setModerator(Long id, boolean value, RedirectAttributes flash, String verb) {
    students
        .findById(id)
        .ifPresent(
            student -> {
              student.setIsModerator(value);
              students.save(student);
              flash.addFlashAttribute(
                  "message", student.getFullName() + " " + verb + " moderator.");
            });
  }
}
