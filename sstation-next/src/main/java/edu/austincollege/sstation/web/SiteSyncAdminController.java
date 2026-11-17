package edu.austincollege.sstation.web;

import edu.austincollege.sstation.repository.EventSiteRepository;
import edu.austincollege.sstation.service.SiteAvailabilityService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin/events/{eventId}/sites/{siteId}/availability")
@PreAuthorize("hasRole('ADMIN')")
public class SiteSyncAdminController {
  private final EventSiteRepository sites;
  private final SiteAvailabilityService availability;

  public SiteSyncAdminController(EventSiteRepository sites, SiteAvailabilityService availability) {
    this.sites = sites;
    this.availability = availability;
  }

  private void page(Long eventId, Long siteId, Model model, HttpServletResponse response) {
    var site =
        sites
            .findByIdAndEventId(siteId, eventId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    model.addAttribute("site", site);
    model.addAttribute("eventId", eventId);
    model.addAttribute("syncEnabled", availability.isEnabled());
    model.addAttribute("availability", availability.views(java.util.List.of(site)).get(siteId));
    response.setHeader("Cache-Control", "no-store");
    response.setHeader("Referrer-Policy", "no-referrer");
  }

  @GetMapping
  public String settings(
      @PathVariable Long eventId,
      @PathVariable Long siteId,
      Model model,
      HttpServletResponse response) {
    page(eventId, siteId, model, response);
    return "events/sites/availability";
  }

  @PostMapping("/connect")
  public String connect(
      @PathVariable Long eventId,
      @PathVariable Long siteId,
      Model model,
      HttpServletResponse response) {
    page(eventId, siteId, model, response);
    model.addAttribute("connection", availability.connect(siteId));
    // Render directly: the signing secret never enters a redirect URL or HTTP session.
    return "events/sites/availability";
  }

  @PostMapping("/disconnect")
  public String disconnect(
      @PathVariable Long eventId,
      @PathVariable Long siteId,
      Model model,
      HttpServletResponse response) {
    page(eventId, siteId, model, response);
    availability.disconnect(siteId);
    return "redirect:/admin/events/" + eventId + "/sites/" + siteId + "/availability";
  }
}
