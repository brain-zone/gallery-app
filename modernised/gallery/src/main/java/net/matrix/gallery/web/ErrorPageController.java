package net.matrix.gallery.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Renders branded responses for HTTP errors that do not use form-login redirection. */
@Controller
public class ErrorPageController {

  @RequestMapping("/authentication-required")
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public String authenticationRequired() {
    return "error/401";
  }
}
