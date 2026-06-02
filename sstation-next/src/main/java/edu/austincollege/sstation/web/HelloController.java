package edu.austincollege.sstation.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Placeholder landing page proving the scaffold boots (TC-101). Replaced by the real dashboards in
 * TC-105.
 */
@Controller
public class HelloController {

  @GetMapping("/")
  public String index(Model model) {
    model.addAttribute("appName", "Service Station Hours (rewrite-in-progress)");
    return "index";
  }
}
