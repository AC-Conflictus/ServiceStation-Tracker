package edu.austincollege.sstation.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.austincollege.sstation.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Web-layer coverage for {@link StudentAdminController} (TC-106b). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class StudentAdminControllerTest {

  @Autowired private MockMvc mvc;
  @Autowired private StudentRepository students;

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminCanListStudents() throws Exception {
    mvc.perform(get("/admin/students")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "MODERATOR")
  void moderatorCanListStudents() throws Exception {
    mvc.perform(get("/admin/students")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void validCreateSavesAndRedirects() throws Exception {
    long before = students.count();
    mvc.perform(
            post("/admin/students")
                .with(csrf())
                .param("firstname", "New")
                .param("lastname", "Student")
                .param("acid", "AC99999")
                .param("acEmail", "nstudent@austincollege.edu")
                .param("status", "A")
                .param("classification", "FR"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/students"));
    assertThat(students.count()).isEqualTo(before + 1);
    assertThat(students.findByAcid("AC99999")).isPresent();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void invalidCreateReRendersFormWithErrors() throws Exception {
    long before = students.count();
    mvc.perform(
            post("/admin/students")
                .with(csrf())
                .param("firstname", "") // @NotBlank
                .param("lastname", "Student")
                .param("acid", "AC88888")
                .param("acEmail", "bad-email") // @Email
                .param("status", "")) // @NotNull
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("student", "firstname", "acEmail", "status"));
    assertThat(students.count()).isEqualTo(before);
  }

  @Test
  @WithMockUser(
      username = "student",
      roles = {"STUDENT"})
  void studentRoleIsForbiddenFromStudentAdmin() throws Exception {
    mvc.perform(get("/admin/students")).andExpect(status().isForbidden());
  }
}
