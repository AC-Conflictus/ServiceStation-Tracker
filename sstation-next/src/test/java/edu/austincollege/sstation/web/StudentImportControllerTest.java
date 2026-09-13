package edu.austincollege.sstation.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import edu.austincollege.sstation.repository.StudentRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Web layer for the student CSV import (TC-123). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class StudentImportControllerTest {

  private static final String CSV =
      "acid,ignored,firstname,lastname,status,acbox,classification,ignored2,email\n"
          + "AC92001,x,Web,Upload,A,77,FR,y,webupload@austincollege.edu\n";

  @Autowired private MockMvc mvc;
  @Autowired private StudentRepository students;

  private MockMultipartFile csvFile(String body) {
    return new MockMultipartFile(
        "file", "students.csv", "text/csv", body.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminSeesTheImportForm() throws Exception {
    mvc.perform(get("/admin/students/import"))
        .andExpect(status().isOk())
        .andExpect(view().name("students/import"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void uploadingACsvImportsTheStudents() throws Exception {
    mvc.perform(multipart("/admin/students/import").file(csvFile(CSV)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("students/import-result"))
        .andExpect(model().attributeExists("result"));

    assertThat(students.findByAcid("AC92001")).isPresent();
  }

  @Test
  @WithMockUser(roles = "MODERATOR")
  void moderatorsCanImportToo() throws Exception {
    // Consistent with StudentAdminController: moderators already create and edit students
    // one at a time, so bulk import is the same capability rather than a new privilege.
    mvc.perform(get("/admin/students/import")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void studentsCannotReachTheImportAtAll() throws Exception {
    // The Grails original allowed this: AcStudentController's class-level @Secured listed
    // ROLE_STUDENT, so any signed-in student could overwrite the whole roster. Deliberately
    // not ported — this test is the guard against someone "restoring parity" by loosening it.
    mvc.perform(get("/admin/students/import")).andExpect(status().isForbidden());
    mvc.perform(multipart("/admin/students/import").file(csvFile(CSV)).with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void submittingWithNoFileReRendersTheFormWithAMessage() throws Exception {
    mvc.perform(multipart("/admin/students/import").file(csvFile("")).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("students/import"))
        .andExpect(model().attributeExists("error"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void theUploadIsCsrfProtected() throws Exception {
    mvc.perform(multipart("/admin/students/import").file(csvFile(CSV)))
        .andExpect(status().isForbidden());
  }
}
