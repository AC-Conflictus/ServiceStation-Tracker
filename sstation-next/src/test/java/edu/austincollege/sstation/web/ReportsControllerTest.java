package edu.austincollege.sstation.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
  @WithMockUser(roles = "ADMIN")
  void everyReportOffersCsvDownload() throws Exception {
    String[][] csvPaths = {
      {"/reports/year.csv", "year_kpis.csv"},
      {"/reports/event.csv", "event_hours.csv"},
      {"/reports/community-org.csv", "community_org_hours.csv"},
      {"/reports/campus-org.csv", "campus_org_hours.csv"},
    };
    for (String[] pair : csvPaths) {
      mvc.perform(get(pair[0]))
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith("text/csv"))
          .andExpect(header().string("Content-Disposition", containsString(pair[1])));
    }
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void summaryAndSemesterCsvFilenamesReflectParams() throws Exception {
    mvc.perform(get("/reports/summary.csv").param("year", "2024"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", containsString("summary_2024.csv")));
    mvc.perform(get("/reports/semester.csv").param("year", "2024").param("semester", "Spring"))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Content-Disposition", containsString("semester_2024_spring.csv")));
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void studentIsForbiddenFromReportCsv() throws Exception {
    mvc.perform(get("/reports/year.csv")).andExpect(status().isForbidden());
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
