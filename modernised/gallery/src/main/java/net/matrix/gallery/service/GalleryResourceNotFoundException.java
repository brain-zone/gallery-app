package net.matrix.gallery.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Indicates that a requested public gallery resource does not exist. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class GalleryResourceNotFoundException extends RuntimeException {

  public GalleryResourceNotFoundException(String message) {
    super(message);
  }
}
