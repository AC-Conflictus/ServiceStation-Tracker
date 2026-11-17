package edu.austincollege.sstation.web;

import edu.austincollege.sstation.domain.EventRegistrationMode;
import edu.austincollege.sstation.domain.EventSite;
import edu.austincollege.sstation.repository.EventRepository;
import edu.austincollege.sstation.repository.EventSiteRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** ADMIN-only site setup and choice of event registration workflow. */
@Controller
@RequestMapping("/admin/events/{eventId}/sites")
@PreAuthorize("hasRole('ADMIN')")
public class EventSiteController {
  private final EventRepository events;
  private final EventSiteRepository sites;

  public EventSiteController(EventRepository events, EventSiteRepository sites) {
    this.events = events;
    this.sites = sites;
  }

  @InitBinder("site")
  void bindFields(WebDataBinder binder) {
    binder.setAllowedFields(
        "name",
        "location",
        "description",
        "requirements",
        "registrationUrl",
        "displayOrder",
        "startsAt",
        "endsAt");
  }

  private void event(Long eventId, Model model) {
    model.addAttribute(
        "event",
        events
            .findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
  }

  private EventSite site(Long eventId, Long id) {
    return sites
        .findByIdAndEventId(id, eventId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @GetMapping
  public String list(@PathVariable Long eventId, Model model) {
    event(eventId, model);
    model.addAttribute("sites", sites.findByEventIdOrderByDisplayOrderAscNameAsc(eventId));
    return "events/sites/list";
  }

  @PostMapping("/registration")
  public String registration(
      @PathVariable Long eventId, @RequestParam EventRegistrationMode registrationMode) {
    var event =
        events
            .findById(eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    event.setRegistrationMode(registrationMode);
    events.save(event);
    return "redirect:/admin/events/" + eventId + "/sites";
  }

  @GetMapping("/new")
  public String createForm(@PathVariable Long eventId, Model model) {
    event(eventId, model);
    model.addAttribute("site", new EventSite());
    return "events/sites/form";
  }

  @GetMapping("/{id}/edit")
  public String edit(@PathVariable Long eventId, @PathVariable Long id, Model model) {
    event(eventId, model);
    model.addAttribute("site", site(eventId, id));
    model.addAttribute("siteId", id);
    return "events/sites/form";
  }

  @PostMapping
  public String create(
      @PathVariable Long eventId,
      @Valid @ModelAttribute("site") EventSite site,
      BindingResult binding,
      Model model) {
    event(eventId, model);
    if (binding.hasErrors()) return "events/sites/form";
    site.setEvent(events.findById(eventId).orElseThrow());
    sites.save(site);
    return "redirect:/admin/events/" + eventId + "/sites";
  }

  @PostMapping("/{id}")
  public String update(
      @PathVariable Long eventId,
      @PathVariable Long id,
      @Valid @ModelAttribute("site") EventSite input,
      BindingResult binding,
      Model model) {
    event(eventId, model);
    EventSite target = site(eventId, id);
    model.addAttribute("siteId", id);
    if (binding.hasErrors()) return "events/sites/form";
    target.setName(input.getName());
    target.setLocation(input.getLocation());
    target.setDescription(input.getDescription());
    target.setRequirements(input.getRequirements());
    target.setRegistrationUrl(input.getRegistrationUrl());
    target.setDisplayOrder(input.getDisplayOrder());
    target.setStartsAt(input.getStartsAt());
    target.setEndsAt(input.getEndsAt());
    sites.save(target);
    return "redirect:/admin/events/" + eventId + "/sites";
  }

  @PostMapping("/{id}/delete")
  public String delete(@PathVariable Long eventId, @PathVariable Long id) {
    sites.delete(site(eventId, id));
    return "redirect:/admin/events/" + eventId + "/sites";
  }
}
