package edu.austincollege.sstation.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.austincollege.sstation.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer coverage for {@link EventController} CRUD, validation, CSRF and role gating (TC-106a).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class EventControllerTest {

  @Autowired private MockMvc mvc;
  @Autowired private EventRepository events;

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminCanListEvents() throws Exception {
    mvc.perform(get("/admin/events")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void validCreateSavesAndRedirects() throws Exception {
    long before = events.count();
    mvc.perform(
            post("/admin/events")
                .with(csrf())
                .param("name", "Test Event")
                .param("description", "A test event")
                .param("contact", "Jane Doe")
                .param("contactPhone", "9038132000")
                .param("contactEmail", "jdoe@austincollege.edu"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/events"));
    assertThat(events.count()).isEqualTo(before + 1);
    assertThat(events.findAll()).anyMatch(e -> "Test Event".equals(e.getName()));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void invalidCreateReRendersFormWithErrors() throws Exception {
    long before = events.count();
    mvc.perform(
            post("/admin/events")
                .with(csrf())
                .param("name", "") // @NotBlank violated
                .param("description", "desc")
                .param("contact", "Jane")
                .param("contactPhone", "903")
                .param("contactEmail", "not-an-email")) // @Email violated
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("event", "name", "contactEmail"));
    assertThat(events.count()).isEqualTo(before); // nothing saved
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void studentIsForbiddenFromEventCrud() throws Exception {
    mvc.perform(get("/admin/events")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createWithoutCsrfIsForbidden() throws Exception {
    mvc.perform(post("/admin/events").param("name", "X")).andExpect(status().isForbidden());
  }
}
