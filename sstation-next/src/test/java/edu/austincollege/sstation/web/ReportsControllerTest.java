package edu.austincollege.sstation.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Reports render for admins/moderators and are gated from students (TC-105b / TC-104). Runs under
 * the dev profile so the seeded demo data is present and the views actually render.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ReportsControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  @WithMockUser(roles = "ADMIN")
  void allReportsRenderForAdmin() throws Exception {
    for (String path :
        new String[] {
          "/reports",
          "/reports/year",
          "/reports/event",
          "/reports/community-org",
          "/reports/campus-org",
          "/reports/summary",
          "/reports/semester"
        }) {
      mvc.perform(get(path)).andExpect(status().isOk());
    }
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void summaryAcceptsYearParam() throws Exception {
    mvc.perform(get("/reports/summary").param("year", "2024")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void semesterAcceptsYearAndSemesterParams() throws Exception {
    mvc.perform(get("/reports/semester").param("year", "2024").param("semester", "Spring"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "MODERATOR")
  void moderatorCanReachReports() throws Exception {
    mvc.perform(get("/reports")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void studentIsForbiddenFromReports() throws Exception {
    mvc.perform(get("/reports")).andExpect(status().isForbidden());
    mvc.perform(get("/reports/year")).andExpect(status().isForbidden());
  }
}
