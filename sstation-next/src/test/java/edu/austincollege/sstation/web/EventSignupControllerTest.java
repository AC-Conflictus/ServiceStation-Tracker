package edu.austincollege.sstation.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.austincollege.sstation.domain.Event;
import edu.austincollege.sstation.repository.EventRepository;
import edu.austincollege.sstation.repository.EventSignupRepository;
import edu.austincollege.sstation.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Web-layer coverage for the event sign-up flow (TC-108f). Dev profile = seeded data. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class EventSignupControllerTest {

  @Autowired private MockMvc mvc;
  @Autowired private EventRepository events;
  @Autowired private EventSignupRepository signups;
  @Autowired private UserRepository users;

  @Test
  @WithMockUser(roles = "STUDENT")
  void studentCanSeeEventsList() throws Exception {
    mvc.perform(get("/student/events")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(
      username = "student",
      roles = {"STUDENT"})
  void seededStudentCanSignUpForAnEvent() throws Exception {
    Event event = events.findAll().get(0);
    var sam = users.findStudentByUsername("student").orElseThrow();
    boolean before = signups.existsByStudentAndEvent(sam, event);

    mvc.perform(post("/student/events/{id}/signup", event.getId()).with(csrf()))
        .andExpect(status().is3xxRedirection());

    assertThat(signups.existsByStudentAndEvent(sam, event)).isTrue();
    assertThat(before).isFalse();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminCanViewRoster() throws Exception {
    Event event = events.findAll().get(0);
    mvc.perform(get("/admin/events/{id}/roster", event.getId())).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminCanConvertAttendedHours() throws Exception {
    Event event = events.findAll().get(0);
    mvc.perform(post("/admin/events/{id}/convert", event.getId()).with(csrf()))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void studentCannotViewRoster() throws Exception {
    Event event = events.findAll().get(0);
    mvc.perform(get("/admin/events/{id}/roster", event.getId())).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminIsNotAllowedToUseStudentSignup() throws Exception {
    Event event = events.findAll().get(0);
    mvc.perform(post("/student/events/{id}/signup", event.getId()).with(csrf()))
        .andExpect(status().isForbidden());
  }
}
