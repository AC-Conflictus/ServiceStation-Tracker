package edu.austincollege.sstation.web;

import edu.austincollege.sstation.service.PasswordPolicy;
import edu.austincollege.sstation.service.PasswordResetService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Self-service password reset (TC-028 / TC-108g). All four endpoints are public (see
 * SecurityConfig) — a logged-out user is exactly who needs them.
 */
@Controller
public class PasswordResetController {

  private final PasswordResetService service;

  public PasswordResetController(PasswordResetService service) {
    this.service = service;
  }

  @GetMapping("/forgot-password")
  public String forgotForm() {
    return "forgot-password";
  }

  @PostMapping("/forgot-password")
  public String requestReset(@RequestParam String email, RedirectAttributes flash) {
    String resetUrlBase =
        ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/reset-password")
            .build()
            .toUriString();
    service.requestReset(email, resetUrlBase);
    // Same response whether or not the email exists — no account enumeration.
    flash.addFlashAttribute(
        "message", "If an account with that email exists, a reset link has been sent.");
    return "redirect:/forgot-password";
  }

  @GetMapping("/reset-password")
  public String resetForm(@RequestParam(required = false) String token, Model model) {
    model.addAttribute("token", token);
    model.addAttribute("minLength", PasswordPolicy.MIN_LENGTH);
    return "reset-password";
  }

  @PostMapping("/reset-password")
  public String reset(
      @RequestParam String token, @RequestParam String password, RedirectAttributes flash) {
    if (service.reset(token, password)) {
      return "redirect:/login?reset";
    }
    flash.addFlashAttribute(
        "error",
        PasswordPolicy.validate(password).orElse("That reset link is invalid or has expired."));
    return "redirect:/reset-password?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
  }
}
