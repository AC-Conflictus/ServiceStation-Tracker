package edu.austincollege.sstation.web;

import edu.austincollege.sstation.domain.Event;
import edu.austincollege.sstation.domain.EventSignup;
import edu.austincollege.sstation.domain.SignupStatus;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.repository.EventRepository;
import edu.austincollege.sstation.repository.UserRepository;
import edu.austincollege.sstation.service.EventSignupService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Event sign-up flow (TC-026 / TC-108f). Students browse events and sign up; admins/moderators view
 * the per-event roster, mark attendance, and convert attended sign-ups into service hours.
 */
@Controller
public class EventSignupController {

  private final EventSignupService signups;
  private final EventRepository events;
  private final UserRepository users;

  public EventSignupController(
      EventSignupService signups, EventRepository events, UserRepository users) {
    this.signups = signups;
    this.events = events;
    this.users = users;
  }

  // ----- student side -----

  @GetMapping("/student/events")
  @PreAuthorize("hasRole('STUDENT')")
  public String studentEvents(Authentication auth, Model model) {
    Optional<Student> student = currentStudent(auth);
    Map<Long, SignupStatus> mine = new LinkedHashMap<>();
    student.ifPresent(
        s -> {
          for (EventSignup signup : signups.signupsForStudent(s)) {
            mine.put(signup.getEvent().getId(), signup.getStatus());
          }
        });
    model.addAttribute("events", events.findAll());
    model.addAttribute("mine", mine);
    model.addAttribute("hasProfile", student.isPresent());
    return "student/events";
  }

  @PostMapping("/student/events/{eventId}/signup")
  @PreAuthorize("hasRole('STUDENT')")
  public String signUp(@PathVariable Long eventId, Authentication auth, RedirectAttributes flash) {
    Optional<Student> student = currentStudent(auth);
    Event event = events.findById(eventId).orElseThrow();
    if (student.isEmpty()) {
      flash.addFlashAttribute("message", "Your login isn't linked to a student profile.");
    } else if (signups.signUp(student.get(), event)) {
      flash.addFlashAttribute("message", "Signed up for " + event.getName() + ".");
    } else {
      flash.addFlashAttribute("message", "You're already signed up for " + event.getName() + ".");
    }
    return "redirect:/student/events";
  }

  // ----- admin/moderator side -----

  @GetMapping("/admin/events/{id}/roster")
  @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
  public String roster(@PathVariable Long id, Model model) {
    Event event = events.findById(id).orElseThrow();
    model.addAttribute("event", event);
    model.addAttribute("roster", signups.roster(event));
    model.addAttribute("signupStatuses", SignupStatus.values());
    return "events/roster";
  }

  @PostMapping("/admin/signups/{id}/status")
  @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
  public String markStatus(
      @PathVariable Long id,
      @RequestParam SignupStatus status,
      @RequestParam Long eventId,
      RedirectAttributes flash) {
    signups.updateStatus(id, status);
    flash.addFlashAttribute("message", "Attendance updated.");
    return "redirect:/admin/events/" + eventId + "/roster";
  }

  @PostMapping("/admin/events/{id}/convert")
  @PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
  public String convert(@PathVariable Long id, Authentication auth, RedirectAttributes flash) {
    Event event = events.findById(id).orElseThrow();
    int converted = signups.convertAttendedToHours(event, auth.getName());
    flash.addFlashAttribute(
        "message",
        converted == 0
            ? "No new attended sign-ups to convert."
            : "Created " + converted + " pending service-hour record(s) from attendance.");
    return "redirect:/admin/events/" + id + "/roster";
  }

  private Optional<Student> currentStudent(Authentication auth) {
    return users.findStudentByUsername(auth.getName());
  }
}
