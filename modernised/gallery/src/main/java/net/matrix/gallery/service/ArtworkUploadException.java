package net.matrix.gallery.service;

/** A safe validation or processing failure for an administrator artwork upload. */
public class ArtworkUploadException extends RuntimeException {
  public ArtworkUploadException(String message) {
    super(message);
  }

  public ArtworkUploadException(String message, Throwable cause) {
    super(message, cause);
  }
}
