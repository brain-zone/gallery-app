package net.matrix.gallery.service;

/** Raised when a bundled artwork catalog cannot be validated or imported safely. */
public class ArtworkCatalogImportException extends RuntimeException {

  public ArtworkCatalogImportException(String message) {
    super(message);
  }

  public ArtworkCatalogImportException(String message, Throwable cause) {
    super(message, cause);
  }
}
