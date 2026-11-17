package edu.austincollege.sstation.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.austincollege.sstation.domain.Event;
import edu.austincollege.sstation.repository.EventRepository;
import edu.austincollege.sstation.repository.EventSiteRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class EventSiteControllerTest {
  @Autowired MockMvc mvc;
  @Autowired EventRepository events;
  @Autowired EventSiteRepository sites;

  private Event event() {
    Event e = new Event();
    e.setName("Site test " + UUID.randomUUID());
    e.setDescription("Synthetic event");
    e.setContact("Office");
    e.setContactPhone("9030000000");
    e.setContactEmail("office@example.invalid");
    return events.save(e);
  }

  private String path(Event e) {
    return "/admin/events/" + e.getId() + "/sites";
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void eventDefaultSchedulePersistsAndRejectsAnEndBeforeStart() throws Exception {
    Event e = event();
    long count = events.count();
    mvc.perform(
            post("/admin/events/" + e.getId())
                .param("name", e.getName())
                .param("description", e.getDescription())
                .param("contact", e.getContact())
                .param("contactPhone", e.getContactPhone())
                .param("contactEmail", e.getContactEmail())
                .param("startsAt", "2027-04-01T09:00")
                .param("endsAt", "2027-04-01T12:00")
                .with(csrf()))
        .andExpect(status().is3xxRedirection());
    assertThat(events.findById(e.getId()).orElseThrow().getStartsAt())
        .hasToString("2027-04-01T09:00");
    var invalid =
        mvc.perform(
                post("/admin/events/" + e.getId())
                    .param("name", e.getName())
                    .param("description", e.getDescription())
                    .param("contact", e.getContact())
                    .param("contactPhone", e.getContactPhone())
                    .param("contactEmail", e.getContactEmail())
                    .param("startsAt", "2027-04-01T09:00")
                    .param("endsAt", "2027-04-01T08:00")
                    .with(csrf()))
            .andExpect(model().attributeHasFieldErrors("event", "scheduleValid"))
            .andExpect(xpath("//main//form[@action='/admin/events/" + e.getId() + "']").exists())
            .andReturn();
    assertThat(events.findById(e.getId()).orElseThrow().getEndsAt())
        .hasToString("2027-04-01T12:00");
    String correctionUrl =
        org.jsoup.Jsoup.parse(invalid.getResponse().getContentAsString())
            .selectFirst("main form")
            .attr("action");
    mvc.perform(
            post(correctionUrl)
                .param("name", e.getName())
                .param("description", e.getDescription())
                .param("contact", e.getContact())
                .param("contactPhone", e.getContactPhone())
                .param("contactEmail", e.getContactEmail())
                .param("startsAt", "2027-04-01T09:00")
                .param("endsAt", "2027-04-01T13:00")
                .with(csrf()))
        .andExpect(status().is3xxRedirection());
    assertThat(events.count()).isEqualTo(count);
    assertThat(events.findById(e.getId()).orElseThrow().getEndsAt())
        .hasToString("2027-04-01T13:00");
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminCanCreateUpdateAndRemoveOwnedSitesWithoutARegistrationLink() throws Exception {
    Event e = event();
    mvc.perform(
            post(path(e))
                .param("name", "Food pantry")
                .param("location", "123 Example St")
                .with(csrf()))
        .andExpect(status().is3xxRedirection());
    var site = sites.findByEventIdOrderByDisplayOrderAscNameAsc(e.getId()).getFirst();
    mvc.perform(get(path(e) + "/" + site.getId() + "/edit")).andExpect(status().isOk());
    mvc.perform(
            post(path(e) + "/" + site.getId())
                .param("name", "Updated pantry")
                .param("location", "456 New St")
                .param("registrationUrl", "https://forms.gle/synthetic")
                .param("startsAt", "2027-04-01T09:00")
                .param("endsAt", "2027-04-01T12:00")
                .with(csrf()))
        .andExpect(status().is3xxRedirection());
    var saved = sites.findById(site.getId()).orElseThrow();
    assertThat(saved.getLocation()).isEqualTo("456 New St");
    assertThat(saved.getStartsAt()).hasToString("2027-04-01T09:00");
    mvc.perform(post(path(e) + "/" + site.getId() + "/delete").with(csrf()))
        .andExpect(status().is3xxRedirection());
    assertThat(sites.findById(site.getId())).isEmpty();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void validationRejectsBadLinksNegativeOrderAndIncompleteSchedule() throws Exception {
    Event e = event();
    mvc.perform(
            post(path(e))
                .param("name", "Site")
                .param("location", "Somewhere")
                .param("registrationUrl", "javascript:alert(1)")
                .param("displayOrder", "-1")
                .param("startsAt", "2027-04-01T09:00")
                .with(csrf()))
        .andExpect(
            model()
                .attributeHasFieldErrors(
                    "site", "registrationUrl", "displayOrder", "scheduleValid"));
    assertThat(sites.findByEventIdOrderByDisplayOrderAscNameAsc(e.getId())).isEmpty();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void sitesCannotBeReadMovedEditedOrDeletedThroughAnotherEvent() throws Exception {
    Event a = event(), b = event();
    mvc.perform(
            post(path(a))
                .param("name", "Owned site")
                .param("location", "Example")
                .param("event.id", b.getId().toString())
                .with(csrf()))
        .andExpect(status().is3xxRedirection());
    var site = sites.findByEventIdOrderByDisplayOrderAscNameAsc(a.getId()).getFirst();
    assertThat(sites.findByEventIdOrderByDisplayOrderAscNameAsc(b.getId())).isEmpty();
    mvc.perform(get(path(b) + "/" + site.getId() + "/edit")).andExpect(status().isNotFound());
    mvc.perform(
            post(path(b) + "/" + site.getId())
                .param("name", "Hijack")
                .param("location", "Elsewhere")
                .with(csrf()))
        .andExpect(status().isNotFound());
    mvc.perform(post(path(b) + "/" + site.getId() + "/delete").with(csrf()))
        .andExpect(status().isNotFound());
    assertThat(sites.findById(site.getId()).orElseThrow().getName()).isEqualTo("Owned site");
  }

  @Test
  @WithMockUser(roles = "MODERATOR")
  void moderatorCannotManageSites() throws Exception {
    Event e = event();
    mvc.perform(get(path(e))).andExpect(status().isForbidden());
    mvc.perform(post(path(e)).param("name", "No").param("location", "No").with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void studentCannotManageSites() throws Exception {
    Event e = event();
    mvc.perform(get(path(e))).andExpect(status().isForbidden());
    mvc.perform(post(path(e)).with(csrf())).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void mutationRequiresCsrfAndDeletingAnEventRemovesItsSites() throws Exception {
    Event e = event();
    mvc.perform(post(path(e)).param("name", "Site").param("location", "Example"))
        .andExpect(status().isForbidden());
    mvc.perform(post(path(e)).param("name", "Site").param("location", "Example").with(csrf()))
        .andExpect(status().is3xxRedirection());
    mvc.perform(post("/admin/events/" + e.getId() + "/delete").with(csrf()))
        .andExpect(status().is3xxRedirection());
    assertThat(sites.findByEventIdOrderByDisplayOrderAscNameAsc(e.getId())).isEmpty();
  }
}
