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
 * The student dashboard resolves the logged-in student via the User->Student FK and is gated to
 * students (TC-105c / TC-104). Dev profile so the seeded `student` -> Sam Student link exists.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class StudentControllerTest {

  @Autowired private MockMvc mvc;

  @Test
  @WithMockUser(
      username = "student",
      roles = {"STUDENT"})
  void seededStudentSeesOwnDashboardResolvedByFk() throws Exception {
    mvc.perform(get("/student"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Sam Student")));
  }

  @Test
  @WithMockUser(
      username = "student",
      roles = {"STUDENT"})
  void seededStudentSeesOwnReport() throws Exception {
    mvc.perform(get("/student/report")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(
      username = "student",
      roles = {"STUDENT"})
  void seededStudentCanDownloadReportCsv() throws Exception {
    mvc.perform(get("/student/report.csv"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/csv"))
        .andExpect(
            header().string("Content-Disposition", containsString("student_AC50000_hours.csv")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminIsForbiddenFromStudentDashboard() throws Exception {
    mvc.perform(get("/student")).andExpect(status().isForbidden());
  }
}
