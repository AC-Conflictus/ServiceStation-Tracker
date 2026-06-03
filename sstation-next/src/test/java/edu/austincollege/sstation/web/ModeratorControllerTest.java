package edu.austincollege.sstation.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Promote/demote flow + ADMIN-only gating for {@link ModeratorController} (TC-106b). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ModeratorControllerTest {

  @Autowired private MockMvc mvc;
  @Autowired private StudentRepository students;

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminCanViewModerators() throws Exception {
    mvc.perform(get("/admin/moderators")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void promoteThenDemoteTogglesTheFlag() throws Exception {
    Student s = students.save(student("AC70001"));

    mvc.perform(post("/admin/moderators/{id}/promote", s.getId()).with(csrf()))
        .andExpect(status().is3xxRedirection());
    assertThat(students.findById(s.getId()).orElseThrow().getIsModerator()).isTrue();

    mvc.perform(post("/admin/moderators/{id}/demote", s.getId()).with(csrf()))
        .andExpect(status().is3xxRedirection());
    assertThat(students.findById(s.getId()).orElseThrow().getIsModerator()).isFalse();
  }

  @Test
  @WithMockUser(roles = "MODERATOR")
  void moderatorRoleCannotManageModerators() throws Exception {
    // Promote/demote is ADMIN-only (matches the Grails ModeratorController @Secured).
    mvc.perform(get("/admin/moderators")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void studentCannotManageModerators() throws Exception {
    mvc.perform(get("/admin/moderators")).andExpect(status().isForbidden());
  }

  private static Student student(String acid) {
    Student s = new Student();
    s.setFirstname("Promote");
    s.setLastname("Me");
    s.setAcid(acid);
    s.setAcEmail("p@austincollege.edu");
    s.setStatus('A');
    s.setIsModerator(false);
    return s;
  }
}
