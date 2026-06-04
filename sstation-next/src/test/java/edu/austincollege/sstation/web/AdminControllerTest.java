package edu.austincollege.sstation.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Admin dashboard rendering + the date-range filter (TC-105 / TC-108e). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AdminControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  @WithMockUser(roles = "ADMIN")
  void dashboardRendersWithoutRange() throws Exception {
    mvc.perform(get("/admin"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("ranged", false));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void dashboardAcceptsShareableDateRange() throws Exception {
    mvc.perform(get("/admin").param("from", "2026-01-01").param("to", "2026-05-25"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("ranged", true))
        .andExpect(model().attributeExists("from", "to", "dashboard"));
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void studentIsForbiddenFromDashboard() throws Exception {
    mvc.perform(get("/admin")).andExpect(status().isForbidden());
  }
}
