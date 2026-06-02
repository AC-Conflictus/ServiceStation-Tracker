package edu.austincollege.sstation.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the form-login page. Spring Security handles the POST to {@code /login}. */
@Controller
public class LoginController {

  @GetMapping("/login")
  public String login() {
    return "login";
  }
}
