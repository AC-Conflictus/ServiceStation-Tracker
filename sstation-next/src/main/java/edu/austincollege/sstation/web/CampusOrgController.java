package edu.austincollege.sstation.web;

import edu.austincollege.sstation.domain.CampusOrg;
import edu.austincollege.sstation.repository.CampusOrgRepository;
import edu.austincollege.sstation.service.ReferenceCrudService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** CRUD for {@link CampusOrg}s (TC-106). Port of the Grails {@code ACGroupController}. */
@Controller
@RequestMapping("/admin/campus-orgs")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class CampusOrgController {

  private final CampusOrgRepository campusOrgs;
  private final ReferenceCrudService crud;

  public CampusOrgController(CampusOrgRepository campusOrgs, ReferenceCrudService crud) {
    this.campusOrgs = campusOrgs;
    this.crud = crud;
  }

  @GetMapping
  public String list(Model model) {
    model.addAttribute("orgs", campusOrgs.findAll());
    return "campus-orgs/list";
  }

  @GetMapping("/new")
  public String newForm(Model model) {
    model.addAttribute("org", new CampusOrg());
    model.addAttribute("heading", "New Campus Organization");
    return "campus-orgs/form";
  }

  @PostMapping
  public String create(
      @Valid @ModelAttribute("org") CampusOrg org,
      BindingResult binding,
      Model model,
      RedirectAttributes flash) {
    if (binding.hasErrors()) {
      model.addAttribute("heading", "New Campus Organization");
      return "campus-orgs/form";
    }
    campusOrgs.save(org);
    flash.addFlashAttribute("message", "Campus organization \"" + org.getName() + "\" created.");
    return "redirect:/admin/campus-orgs";
  }

  @GetMapping("/{id}/edit")
  public String editForm(@PathVariable Long id, Model model) {
    CampusOrg org = campusOrgs.findById(id).orElseThrow();
    model.addAttribute("org", org);
    model.addAttribute("heading", "Edit " + org.getName());
    return "campus-orgs/form";
  }

  @PostMapping("/{id}")
  public String update(
      @PathVariable Long id,
      @Valid @ModelAttribute("org") CampusOrg org,
      BindingResult binding,
      Model model,
      RedirectAttributes flash) {
    if (binding.hasErrors()) {
      model.addAttribute("heading", "Edit campus organization");
      return "campus-orgs/form";
    }
    CampusOrg existing = campusOrgs.findById(id).orElseThrow();
    existing.setName(org.getName());
    existing.setDescription(org.getDescription());
    existing.setContact(org.getContact());
    existing.setContactPhone(org.getContactPhone());
    existing.setContactEmail(org.getContactEmail());
    campusOrgs.save(existing);
    flash.addFlashAttribute(
        "message", "Campus organization \"" + existing.getName() + "\" updated.");
    return "redirect:/admin/campus-orgs";
  }

  @PostMapping("/{id}/delete")
  public String delete(@PathVariable Long id, RedirectAttributes flash) {
    crud.deleteCampusOrg(id);
    flash.addFlashAttribute("message", "Campus organization deleted (service hours kept).");
    return "redirect:/admin/campus-orgs";
  }
}
