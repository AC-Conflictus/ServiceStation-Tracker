package edu.austincollege.sstation.web;

import edu.austincollege.sstation.security.SstationUserDetails;
import edu.austincollege.sstation.service.PasswordChangeService;
import edu.austincollege.sstation.service.PasswordChangeService.Result;
import edu.austincollege.sstation.service.PasswordPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
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
  public String form(Model model, Authentication auth) {
    model.addAttribute("minLength", PasswordPolicy.MIN_LENGTH);
    model.addAttribute(
        "mustChange",
        auth != null
            && auth.getPrincipal() instanceof SstationUserDetails user
            && user.isMustChangePassword());
    return "change-password";
  }

  @PostMapping("/change-password")
  public String change(
      @RequestParam String currentPassword,
      @RequestParam String newPassword,
      Principal principal,
      HttpServletRequest request,
      HttpServletResponse response,
      RedirectAttributes flash) {

    Result result = service.change(principal.getName(), currentPassword, newPassword);
    if (result == Result.OK) {
      // End the session rather than patch the principal in place. SstationUserDetails snapshots
      // mustChangePassword at login, so a still-flagged principal would keep the filter bouncing
      // this user back here forever. Re-authenticating is also the conventional thing to do after
      // a credential change.
      new SecurityContextLogoutHandler()
          .logout(request, response, SecurityContextHolder.getContext().getAuthentication());
      return "redirect:/login?changed";
    }

    flash.addFlashAttribute(
        "error",
        result == Result.TOO_WEAK ? service.weaknessMessage(newPassword) : result.message());
    return "redirect:/change-password";
  }
}
