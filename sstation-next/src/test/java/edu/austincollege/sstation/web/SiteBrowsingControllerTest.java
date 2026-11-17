package edu.austincollege.sstation.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.austincollege.sstation.domain.*;
import edu.austincollege.sstation.repository.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SiteBrowsingControllerTest {
  @Autowired MockMvc mvc;
  @Autowired EventRepository events;
  @Autowired EventSiteRepository sites;
  @Autowired EventSignupRepository signups;

  private Event event(EventRegistrationMode mode) {
    Event e = new Event();
    e.setName("Browse-" + UUID.randomUUID());
    e.setDescription("Choose a site");
    e.setContact("Office");
    e.setContactPhone("9030000000");
    e.setContactEmail("office@example.invalid");
    e.setStartsAt(LocalDateTime.of(2027, 4, 10, 9, 0));
    e.setEndsAt(LocalDateTime.of(2027, 4, 10, 12, 0));
    e.setRegistrationMode(mode);
    return events.save(e);
  }

  private EventSite site(Event e, int order) {
    EventSite s = new EventSite();
    s.setEvent(e);
    s.setName("Nonprofit " + order);
    s.setLocation("Example location " + order);
    s.setDisplayOrder(order);
    return sites.save(s);
  }

  private MockHttpServletRequestBuilder edit(Event e) {
    return post("/admin/events/" + e.getId())
        .param("name", e.getName())
        .param("description", e.getDescription())
        .param("contact", e.getContact())
        .param("contactPhone", e.getContactPhone())
        .param("contactEmail", e.getContactEmail());
  }

  @Test
  @WithMockUser(username = "student", roles = "STUDENT")
  void studentBrowsesThirtyOneSitesWithInheritedAndOverriddenTimesWithoutSigningUp()
      throws Exception {
    Event e = event(EventRegistrationMode.SITE_FORMS);
    for (int i = 30; i >= 0; i--) site(e, i);
    EventSite s = sites.findByEventIdOrderByDisplayOrderAscNameAsc(e.getId()).getFirst();
    s.setStartsAt(LocalDateTime.of(2027, 4, 10, 10, 30));
    s.setEndsAt(LocalDateTime.of(2027, 4, 10, 13, 30));
    s.setRegistrationUrl("https://forms.gle/example");
    s.setRequirements("Closed-toe shoes");
    sites.save(s);
    long count = signups.count();
    mvc.perform(get("/student/events/" + e.getId()))
        .andExpect(status().isOk())
        .andExpect(xpath("//article").nodeCount(31))
        .andExpect(xpath("(//article)[1]//h2").string("Nonprofit 0"))
        .andExpect(
            xpath(
                    "//a[@href='https://forms.gle/example'][@target='_blank'][@rel='noopener noreferrer']")
                .exists())
        .andExpect(xpath("//article//button[@disabled]").nodeCount(30))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("10:30 AM")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("9:00 AM")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Closed-toe shoes")))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Availability unavailable")));
    mvc.perform(post("/student/events/" + e.getId() + "/signup").with(csrf()))
        .andExpect(redirectedUrl("/student/events/" + e.getId()));
    assertThat(signups.count()).isEqualTo(count);
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void eventListOffersSitesInsteadOfLegacySignupAndHandlesAnEmptySiteList() throws Exception {
    Event e = event(EventRegistrationMode.SITE_FORMS);
    mvc.perform(get("/student/events"))
        .andExpect(
            xpath("//a[@href='/student/events/" + e.getId() + "'][text()='View sites']").exists())
        .andExpect(
            xpath("//form[@action='/student/events/" + e.getId() + "/signup']").doesNotExist());
    mvc.perform(get("/student/events/" + e.getId()))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Sites are being prepared")));
    Event ordinary = event(EventRegistrationMode.DIRECT);
    mvc.perform(get("/student/events/" + ordinary.getId()))
        .andExpect(redirectedUrl("/student/events"));
    mvc.perform(get("/student/events/999999999")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminSwitchesModeWithCsrf() throws Exception {
    Event e = event(EventRegistrationMode.DIRECT);
    String path = "/admin/events/" + e.getId() + "/sites/registration";
    mvc.perform(post(path).param("registrationMode", "SITE_FORMS"))
        .andExpect(status().isForbidden());
    mvc.perform(post(path).param("registrationMode", "SITE_FORMS").with(csrf()))
        .andExpect(status().is3xxRedirection());
    assertThat(events.findById(e.getId()).orElseThrow().isSiteRegistration()).isTrue();
    mvc.perform(post(path).param("registrationMode", "DIRECT").with(csrf()))
        .andExpect(status().is3xxRedirection());
    assertThat(events.findById(e.getId()).orElseThrow().isSiteRegistration()).isFalse();
  }

  @Test
  @WithMockUser(roles = "MODERATOR")
  void moderatorCannotSwitchModeOrForgeItThroughOrdinaryEventEditing() throws Exception {
    Event e = event(EventRegistrationMode.DIRECT);
    mvc.perform(
            post("/admin/events/" + e.getId() + "/sites/registration")
                .param("registrationMode", "SITE_FORMS")
                .with(csrf()))
        .andExpect(status().isForbidden());
    mvc.perform(edit(e).param("registrationMode", "SITE_FORMS").with(csrf()))
        .andExpect(status().is3xxRedirection());
    assertThat(events.findById(e.getId()).orElseThrow().isSiteRegistration()).isFalse();
    mvc.perform(get("/student/events/" + e.getId())).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void studentCannotEnableSiteRegistration() throws Exception {
    Event e = event(EventRegistrationMode.DIRECT);
    mvc.perform(
            post("/admin/events/" + e.getId() + "/sites/registration")
                .param("registrationMode", "SITE_FORMS")
                .with(csrf()))
        .andExpect(status().isForbidden());
  }
}
