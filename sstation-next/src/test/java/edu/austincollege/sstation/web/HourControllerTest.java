package edu.austincollege.sstation.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.austincollege.sstation.domain.ServiceHour;
import edu.austincollege.sstation.domain.Status;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.repository.ServiceHourAuditLogRepository;
import edu.austincollege.sstation.repository.ServiceHourRepository;
import edu.austincollege.sstation.repository.StudentRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer coverage for {@link HourController}: CRUD, quick approve/reject, audit, gating
 * (TC-106c).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class HourControllerTest {

  @Autowired private MockMvc mvc;
  @Autowired private ServiceHourRepository serviceHours;
  @Autowired private StudentRepository students;
  @Autowired private ServiceHourAuditLogRepository auditLogs;

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminCanListNewAndPending() throws Exception {
    mvc.perform(get("/admin/hours")).andExpect(status().isOk());
    mvc.perform(get("/admin/hours/pending")).andExpect(status().isOk());
    mvc.perform(get("/admin/hours/new")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void createWritesHourAndAuditEntry() throws Exception {
    Student student = students.findAll().get(0);
    long auditBefore = auditLogs.count();

    mvc.perform(
            post("/admin/hours")
                .with(csrf())
                .param("studentId", student.getId().toString())
                .param("description", "Tutoring")
                .param("duration", "3.0")
                .param("startTime", "2026-03-10T10:00")
                .param("status", "PENDING"))
        .andExpect(status().is3xxRedirection());

    assertThat(auditLogs.count()).isEqualTo(auditBefore + 1); // create logged null -> PENDING
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void invalidCreateReRendersFormWithErrors() throws Exception {
    mvc.perform(
            post("/admin/hours")
                .with(csrf())
                .param("studentId", "") // @NotNull
                .param("description", "x")
                .param("duration", "-1") // @Positive
                .param("status", "PENDING"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("form", "studentId", "duration", "startTime"));
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminQuickStatusChangeUpdatesAndAudits() throws Exception {
    ServiceHour hour = serviceHours.save(pendingHour());
    long auditBefore = auditLogs.count();

    mvc.perform(
            post("/admin/hours/{id}/status", hour.getId()).with(csrf()).param("status", "APPROVED"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.status").value("APPROVED"));

    assertThat(serviceHours.findById(hour.getId()).orElseThrow().getStatus())
        .isEqualTo(Status.APPROVED);
    assertThat(auditLogs.count()).isEqualTo(auditBefore + 1);
  }

  @Test
  @WithMockUser(
      username = "admin",
      roles = {"ADMIN"})
  void adminBulkStatusUpdatesAllSelectedAndAudits() throws Exception {
    ServiceHour a = serviceHours.save(pendingHour());
    ServiceHour b = serviceHours.save(pendingHour());
    long auditBefore = auditLogs.count();

    mvc.perform(
            post("/admin/hours/bulk-status")
                .with(csrf())
                .param("ids", a.getId().toString(), b.getId().toString())
                .param("status", "APPROVED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.updated").value(2))
        .andExpect(jsonPath("$.status").value("APPROVED"));

    assertThat(serviceHours.findById(a.getId()).orElseThrow().getStatus())
        .isEqualTo(Status.APPROVED);
    assertThat(serviceHours.findById(b.getId()).orElseThrow().getStatus())
        .isEqualTo(Status.APPROVED);
    assertThat(auditLogs.count()).isEqualTo(auditBefore + 2); // one audit row per hour
  }

  @Test
  @WithMockUser(roles = "MODERATOR")
  void moderatorCannotBulkChangeStatus() throws Exception {
    ServiceHour hour = serviceHours.save(pendingHour());
    mvc.perform(
            post("/admin/hours/bulk-status")
                .with(csrf())
                .param("ids", hour.getId().toString())
                .param("status", "APPROVED"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "MODERATOR")
  void moderatorCanViewHoursButNotChangeStatus() throws Exception {
    ServiceHour hour = serviceHours.save(pendingHour());
    mvc.perform(get("/admin/hours")).andExpect(status().isOk());
    // Status change is ADMIN-only.
    mvc.perform(
            post("/admin/hours/{id}/status", hour.getId()).with(csrf()).param("status", "APPROVED"))
        .andExpect(status().isForbidden());
    // Audit view is ADMIN-only.
    mvc.perform(get("/admin/hours/{id}/audit", hour.getId())).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void studentIsForbiddenFromHourAdmin() throws Exception {
    mvc.perform(get("/admin/hours")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void statusChangeWithoutCsrfIsForbidden() throws Exception {
    ServiceHour hour = serviceHours.save(pendingHour());
    mvc.perform(post("/admin/hours/{id}/status", hour.getId()).param("status", "APPROVED"))
        .andExpect(status().isForbidden());
  }

  private ServiceHour pendingHour() {
    ServiceHour h = new ServiceHour();
    h.setDescription("service");
    h.setStatus(Status.PENDING);
    h.setDuration(2.0);
    h.setStartTime(LocalDateTime.now());
    h.setLastModified(LocalDateTime.now());
    h.setStudent(students.findAll().get(0));
    return h;
  }
}
