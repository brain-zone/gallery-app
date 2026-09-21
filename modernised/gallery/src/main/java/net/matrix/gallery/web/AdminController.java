package net.matrix.gallery.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Renders the authenticated administration landing page. */
@Controller
public class AdminController {

  @GetMapping("/admin")
  public String admin() {
    return "admin";
  }
}
