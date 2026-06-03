package edu.austincollege.sstation.web;

import edu.austincollege.sstation.domain.Classification;
import edu.austincollege.sstation.domain.Student;
import edu.austincollege.sstation.repository.StudentRepository;
import edu.austincollege.sstation.service.StudentCrudService;
import jakarta.validation.Valid;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Admin/moderator management of {@link Student} profiles (TC-106b). Port of the Grails {@code
 * AcStudentController} CRUD actions. Distinct from {@code /student}, which is the logged-in
 * student's own dashboard.
 */
@Controller
@RequestMapping("/admin/students")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class StudentAdminController {

  private final StudentRepository students;
  private final StudentCrudService studentCrud;

  public StudentAdminController(StudentRepository students, StudentCrudService studentCrud) {
    this.students = students;
    this.studentCrud = studentCrud;
  }

  /**
   * Treat empty form fields as null (so optional fields like acYear/classification clear cleanly).
   */
  @InitBinder
  void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
  }

  @ModelAttribute("classifications")
  Classification[] classifications() {
    return Classification.values();
  }

  @GetMapping
  public String list(Model model) {
    model.addAttribute("students", students.findAll());
    return "students/list";
  }

  @GetMapping("/new")
  public String newForm(Model model) {
    Student student = new Student();
    student.setStatus('A');
    model.addAttribute("student", student);
    model.addAttribute("heading", "New Student");
    return "students/form";
  }

  @PostMapping
  public String create(
      @Valid @ModelAttribute("student") Student student,
      BindingResult binding,
      Model model,
      RedirectAttributes flash) {
    if (binding.hasErrors()) {
      model.addAttribute("heading", "New Student");
      return "students/form";
    }
    students.save(student);
    flash.addFlashAttribute("message", "Student \"" + student.getFullName() + "\" created.");
    return "redirect:/admin/students";
  }

  @GetMapping("/{id}/edit")
  public String editForm(@PathVariable Long id, Model model) {
    Student student = students.findById(id).orElseThrow();
    model.addAttribute("student", student);
    model.addAttribute("heading", "Edit " + student.getFullName());
    return "students/form";
  }

  @PostMapping("/{id}")
  public String update(
      @PathVariable Long id,
      @Valid @ModelAttribute("student") Student student,
      BindingResult binding,
      Model model,
      RedirectAttributes flash) {
    if (binding.hasErrors()) {
      model.addAttribute("heading", "Edit student");
      return "students/form";
    }
    Student existing = students.findById(id).orElseThrow();
    existing.setFirstname(student.getFirstname());
    existing.setLastname(student.getLastname());
    existing.setAcid(student.getAcid());
    existing.setAcEmail(student.getAcEmail());
    existing.setAcBox(student.getAcBox());
    existing.setPhone(student.getPhone());
    existing.setAcYear(student.getAcYear());
    existing.setStatus(student.getStatus());
    existing.setClassification(student.getClassification());
    existing.setIsModerator(student.getIsModerator());
    students.save(existing);
    flash.addFlashAttribute("message", "Student \"" + existing.getFullName() + "\" updated.");
    return "redirect:/admin/students";
  }

  @PostMapping("/{id}/delete")
  public String delete(@PathVariable Long id, RedirectAttributes flash) {
    studentCrud.deleteStudent(id);
    flash.addFlashAttribute("message", "Student deleted (their service hours were removed).");
    return "redirect:/admin/students";
  }
}
