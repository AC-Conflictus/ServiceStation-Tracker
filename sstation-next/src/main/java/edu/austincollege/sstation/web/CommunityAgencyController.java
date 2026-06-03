package edu.austincollege.sstation.web;

import edu.austincollege.sstation.domain.CommunityAgency;
import edu.austincollege.sstation.repository.CommunityAgencyRepository;
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

/** CRUD for {@link CommunityAgency}s (TC-106). Port of the Grails {@code CommOrgController}. */
@Controller
@RequestMapping("/admin/agencies")
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class CommunityAgencyController {

  private final CommunityAgencyRepository agencies;
  private final ReferenceCrudService crud;

  public CommunityAgencyController(CommunityAgencyRepository agencies, ReferenceCrudService crud) {
    this.agencies = agencies;
    this.crud = crud;
  }

  @GetMapping
  public String list(Model model) {
    model.addAttribute("agencies", agencies.findAll());
    return "agencies/list";
  }

  @GetMapping("/new")
  public String newForm(Model model) {
    model.addAttribute("agency", new CommunityAgency());
    model.addAttribute("heading", "New Community Agency");
    return "agencies/form";
  }

  @PostMapping
  public String create(
      @Valid @ModelAttribute("agency") CommunityAgency agency,
      BindingResult binding,
      Model model,
      RedirectAttributes flash) {
    if (binding.hasErrors()) {
      model.addAttribute("heading", "New Community Agency");
      return "agencies/form";
    }
    agencies.save(agency);
    flash.addFlashAttribute("message", "Community agency \"" + agency.getName() + "\" created.");
    return "redirect:/admin/agencies";
  }

  @GetMapping("/{id}/edit")
  public String editForm(@PathVariable Long id, Model model) {
    CommunityAgency agency = agencies.findById(id).orElseThrow();
    model.addAttribute("agency", agency);
    model.addAttribute("heading", "Edit " + agency.getName());
    return "agencies/form";
  }

  @PostMapping("/{id}")
  public String update(
      @PathVariable Long id,
      @Valid @ModelAttribute("agency") CommunityAgency agency,
      BindingResult binding,
      Model model,
      RedirectAttributes flash) {
    if (binding.hasErrors()) {
      model.addAttribute("heading", "Edit community agency");
      return "agencies/form";
    }
    CommunityAgency existing = agencies.findById(id).orElseThrow();
    existing.setName(agency.getName());
    existing.setAddress(agency.getAddress());
    existing.setDescription(agency.getDescription());
    existing.setContact(agency.getContact());
    existing.setContactPhone(agency.getContactPhone());
    existing.setContactEmail(agency.getContactEmail());
    agencies.save(existing);
    flash.addFlashAttribute("message", "Community agency \"" + existing.getName() + "\" updated.");
    return "redirect:/admin/agencies";
  }

  @PostMapping("/{id}/delete")
  public String delete(@PathVariable Long id, RedirectAttributes flash) {
    crud.deleteCommunityAgency(id);
    flash.addFlashAttribute("message", "Community agency deleted (service hours kept).");
    return "redirect:/admin/agencies";
  }
}
