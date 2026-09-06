package edu.austincollege.sstation.web;

import edu.austincollege.sstation.service.PasswordChangeService;
import edu.austincollege.sstation.service.PasswordChangeService.Result;
import edu.austincollege.sstation.service.PasswordPolicy;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * In-app password change for the signed-in user (TC-121). Any authenticated account may use it;
 * accounts flagged {@code mustChangePassword} are forced here by {@link
 * edu.austincollege.sstation.security.MustChangePasswordFilter}.
 */
@Controller
public class ChangePasswordController {

  private final PasswordChangeService service;

  public ChangePasswordController(PasswordChangeService service) {
    this.service = service;
  }

  @GetMapping("/change-password")
  public String form(Model model) {
    model.addAttribute("minLength", PasswordPolicy.MIN_LENGTH);
    return "change-password";
  }

  @PostMapping("/change-password")
  public String change(
      @RequestParam String currentPassword,
      @RequestParam String newPassword,
      Principal principal,
      RedirectAttributes flash) {

    Result result = service.change(principal.getName(), currentPassword, newPassword);
    if (result == Result.OK) {
      flash.addFlashAttribute("message", "Your password has been changed.");
      return "redirect:/";
    }

    flash.addFlashAttribute(
        "error",
        result == Result.TOO_WEAK ? service.weaknessMessage(newPassword) : result.message());
    return "redirect:/change-password";
  }
}
