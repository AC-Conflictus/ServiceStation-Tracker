package edu.austincollege.sstation.web;

import edu.austincollege.sstation.domain.Event;
import edu.austincollege.sstation.repository.EventRepository;
import edu.austincollege.sstation.service.ReferenceCrudService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** CRUD for service {@link Event}s (TC-106). Port of the Grails {@code EventController}. */
@Controller
@RequestMapping("/admin/events")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class EventController {

  private final EventRepository events;
  private final ReferenceCrudService crud;

  public EventController(EventRepository events, ReferenceCrudService crud) {
    this.events = events;
    this.crud = crud;
  }

  @GetMapping
  public String list(Model model) {
    model.addAttribute("events", events.findAll());
    return "events/list";
  }

  @GetMapping("/new")
  public String newForm(Model model) {
    model.addAttribute("event", new Event());
    model.addAttribute("heading", "New Event");
    return "events/form";
  }

  @PostMapping
  public String create(
      @Valid @ModelAttribute("event") Event event,
      BindingResult binding,
      Model model,
      RedirectAttributes flash) {
    if (binding.hasErrors()) {
      model.addAttribute("heading", "New Event");
      return "events/form";
    }
    events.save(event);
    flash.addFlashAttribute("message", "Event \"" + event.getName() + "\" created.");
    return "redirect:/admin/events";
  }

  @GetMapping("/{id}/edit")
  public String editForm(@PathVariable Long id, Model model) {
    Event event = events.findById(id).orElseThrow();
    model.addAttribute("event", event);
    model.addAttribute("heading", "Edit " + event.getName());
    return "events/form";
  }

  @PostMapping("/{id}")
  public String update(
      @PathVariable Long id,
      @Valid @ModelAttribute("event") Event event,
      BindingResult binding,
      Model model,
      RedirectAttributes flash) {
    if (binding.hasErrors()) {
      model.addAttribute("heading", "Edit event");
      return "events/form";
    }
    Event existing = events.findById(id).orElseThrow();
    existing.setName(event.getName());
    existing.setDescription(event.getDescription());
    existing.setContact(event.getContact());
    existing.setContactPhone(event.getContactPhone());
    existing.setContactEmail(event.getContactEmail());
    events.save(existing);
    flash.addFlashAttribute("message", "Event \"" + existing.getName() + "\" updated.");
    return "redirect:/admin/events";
  }

  @PostMapping("/{id}/delete")
  public String delete(@PathVariable Long id, RedirectAttributes flash) {
    crud.deleteEvent(id);
    flash.addFlashAttribute(
        "message", "Event deleted (its service hours were kept, event cleared).");
    return "redirect:/admin/events";
  }
}
