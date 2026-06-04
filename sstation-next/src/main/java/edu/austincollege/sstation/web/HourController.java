package edu.austincollege.sstation.web;

import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.Status;
import edu.austincollege.sstation.repository.CampusOrgRepository;
import edu.austincollege.sstation.repository.CommunityAgencyRepository;
import edu.austincollege.sstation.repository.EventRepository;
import edu.austincollege.sstation.repository.ServiceHourRepository;
import edu.austincollege.sstation.repository.StudentRepository;
import edu.austincollege.sstation.service.AuditService;
import edu.austincollege.sstation.service.NotificationService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Service-hour CRUD, the quick approve/reject endpoint, and the per-hour audit view (TC-106c). Port
 * of the Grails {@code HourController} + {@code HomeController.ajaxUpdateStatus}.
 *
 * <p>CRUD is open to admins and moderators; <b>status changes are ADMIN-only</b> (matching the
 * Grails "Only admin can change the status" rule), and every status change is written to the audit
 * trail (TC-027).
 */
@Controller
@RequestMapping("/admin/hours")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class HourController {

  private final ServiceHourRepository serviceHours;
  private final StudentRepository students;
  private final EventRepository events;
  private final CampusOrgRepository campusOrgs;
  private final CommunityAgencyRepository agencies;
  private final AuditService audit;
  private final NotificationService notifications;

  public HourController(
      ServiceHourRepository serviceHours,
      StudentRepository students,
      EventRepository events,
      CampusOrgRepository campusOrgs,
      CommunityAgencyRepository agencies,
      AuditService audit,
      NotificationService notifications) {
    this.serviceHours = serviceHours;
    this.students = students;
    this.events = events;
    this.campusOrgs = campusOrgs;
    this.agencies = agencies;
    this.audit = audit;
    this.notifications = notifications;
  }

  @InitBinder
  void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
  }

  @ModelAttribute("statuses")
  Status[] statuses() {
    return Status.values();
  }

  @GetMapping
  public String list(Model model) {
    model.addAttribute("hours", serviceHours.findAllWithRefs());
    model.addAttribute("title", "All service hours");
    return "hours/list";
  }

  @GetMapping("/pending")
  public String pending(Model model) {
    model.addAttribute("hours", serviceHours.findByStatusWithRefs(Status.PENDING));
    model.addAttribute("title", "Pending service hours");
    return "hours/list";
  }

  @GetMapping("/new")
  public String newForm(Model model) {
    ServiceHourForm form = new ServiceHourForm();
    form.setStatus(Status.PENDING);
    form.setStartTime(LocalDateTime.now());
    model.addAttribute("form", form);
    model.addAttribute("heading", "New Service Hour");
    addReferenceData(model);
    return "hours/form";
  }

  @PostMapping
  public String create(
      @Valid @ModelAttribute("form") ServiceHourForm form,
      BindingResult binding,
      Authentication auth,
      Model model,
      RedirectAttributes flash) {
    if (binding.hasErrors()) {
      model.addAttribute("heading", "New Service Hour");
      addReferenceData(model);
      return "hours/form";
    }
    ServiceHour hour = new ServiceHour();
    apply(form, hour);
    serviceHours.save(hour);
    audit.record(hour, null, hour.getStatus(), auth.getName(), "Service hour created");
    flash.addFlashAttribute("message", "Service hour created.");
    return "redirect:/admin/hours";
  }

  @GetMapping("/{id}/edit")
  public String editForm(@PathVariable Long id, Model model) {
    ServiceHour hour = serviceHours.findById(id).orElseThrow();
    model.addAttribute("form", ServiceHourForm.fromEntity(hour));
    model.addAttribute("hourId", id);
    model.addAttribute("heading", "Edit Service Hour");
    addReferenceData(model);
    return "hours/form";
  }

  @PostMapping("/{id}")
  public String update(
      @PathVariable Long id,
      @Valid @ModelAttribute("form") ServiceHourForm form,
      BindingResult binding,
      Authentication auth,
      Model model,
      RedirectAttributes flash) {
    if (binding.hasErrors()) {
      model.addAttribute("hourId", id);
      model.addAttribute("heading", "Edit Service Hour");
      addReferenceData(model);
      return "hours/form";
    }
    ServiceHour hour = serviceHours.findById(id).orElseThrow();
    Status oldStatus = hour.getStatus();
    apply(form, hour);
    serviceHours.save(hour);
    if (oldStatus != hour.getStatus()) {
      audit.record(hour, oldStatus, hour.getStatus(), auth.getName(), "Status changed via edit");
      notifications.notifyStatusChange(hour, hour.getStatus());
    }
    flash.addFlashAttribute("message", "Service hour updated.");
    return "redirect:/admin/hours";
  }

  @PostMapping("/{id}/delete")
  public String delete(@PathVariable Long id, RedirectAttributes flash) {
    serviceHours.deleteById(id); // DB cascade removes the audit rows
    flash.addFlashAttribute("message", "Service hour deleted.");
    return "redirect:/admin/hours";
  }

  /**
   * Quick approve/reject — the modern, CSRF-protected replacement for the Grails {@code
   * ajaxUpdateStatus} AJAX call. ADMIN-only; writes an audit entry. Returns JSON.
   */
  @PostMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseBody
  public ResponseEntity<Map<String, Object>> updateStatus(
      @PathVariable Long id, @RequestParam Status status, Authentication auth) {
    // Fetch the student up front (open-in-view is off) so the notification can read its email.
    ServiceHour hour = serviceHours.findByIdWithStudent(id).orElse(null);
    if (hour == null) {
      return ResponseEntity.notFound().build();
    }
    Status from = hour.getStatus();
    if (from != status) {
      hour.setStatus(status);
      hour.setLastModified(LocalDateTime.now());
      serviceHours.save(hour);
      audit.record(hour, from, status, auth.getName(), "Quick status change");
      notifications.notifyStatusChange(hour, status);
    }
    return ResponseEntity.ok(Map.of("id", id, "status", status.name()));
  }

  /** Per-hour audit trail — ADMIN-only (TC-027). */
  @GetMapping("/{id}/audit")
  @PreAuthorize("hasRole('ADMIN')")
  public String auditTrail(@PathVariable Long id, Model model) {
    ServiceHour hour = serviceHours.findByIdWithStudent(id).orElseThrow();
    model.addAttribute("hour", hour);
    model.addAttribute("entries", audit.historyFor(hour));
    return "hours/audit";
  }

  // ----- helpers -----

  private void apply(ServiceHourForm form, ServiceHour hour) {
    hour.setStudent(students.findById(form.getStudentId()).orElseThrow());
    hour.setDescription(form.getDescription());
    hour.setDuration(form.getDuration());
    hour.setStartTime(form.getStartTime());
    hour.setStatus(form.getStatus());
    hour.setLastModified(LocalDateTime.now());
    hour.setEvent(
        form.getEventId() == null ? null : events.findById(form.getEventId()).orElse(null));
    hour.setCampusOrg(
        form.getCampusOrgId() == null
            ? null
            : campusOrgs.findById(form.getCampusOrgId()).orElse(null));
    hour.setCommAg(
        form.getCommAgId() == null ? null : agencies.findById(form.getCommAgId()).orElse(null));
    hour.setOtherCamOrg(form.getOtherCamOrg());
    hour.setOtherCommAg(form.getOtherCommAg());
    hour.setEventContactName(form.getEventContactName());
    hour.setEventContactPhone(form.getEventContactPhone());
    hour.setEventContactEmail(form.getEventContactEmail());
  }

  private void addReferenceData(Model model) {
    model.addAttribute("students", students.findAll());
    model.addAttribute("events", events.findAll());
    model.addAttribute("campusOrgs", campusOrgs.findAll());
    model.addAttribute("agencies", agencies.findAll());
  }
}
