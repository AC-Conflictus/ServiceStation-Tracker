package edu.austincollege.sstation.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Landing dispatcher. Mirrors the Grails {@code HomeController.index}: admins go to the KPI
 * dashboard, students to their own dashboard. Anyone else (e.g. moderators) gets the simple landing
 * page with role-appropriate links.
 */
@Controller
public class HomeController {

  @GetMapping("/")
  public String index(Authentication authentication, Model model) {
    if (hasRole(authentication, "ROLE_ADMIN")) {
      return "redirect:/admin";
    }
    if (hasRole(authentication, "ROLE_STUDENT")) {
      return "redirect:/student";
    }
    model.addAttribute("appName", "Service Station Hours");
    return "index";
  }

  private boolean hasRole(Authentication authentication, String role) {
    if (authentication == null) {
      return false;
    }
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      if (role.equals(authority.getAuthority())) {
        return true;
      }
    }
    return false;
  }
}
