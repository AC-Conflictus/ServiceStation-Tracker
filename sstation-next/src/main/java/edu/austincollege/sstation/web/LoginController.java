package edu.austincollege.sstation.web;

import edu.austincollege.sstation.security.AuthProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the sign-in page. Spring Security handles the POST to {@code /login}.
 *
 * <p>The page follows AC Self-Service's sign-in flow (TC-124) — one card, user name and password,
 * one button — so signing in here feels like signing in anywhere else at Austin College. Which copy
 * it shows depends on {@link AuthProperties#mode()}: in directory mode the password belongs to AC
 * IT, so the page says so and points "forgot password" at them instead of at our own reset flow.
 */
@Controller
public class LoginController {

  private final AuthProperties auth;

  public LoginController(AuthProperties auth) {
    this.auth = auth;
  }

  @GetMapping("/login")
  public String login(Model model) {
    model.addAttribute("directoryMode", auth.directoryMode());
    model.addAttribute("passwordHelpUrl", auth.passwordHelpUrl());
    return "login";
  }
}
