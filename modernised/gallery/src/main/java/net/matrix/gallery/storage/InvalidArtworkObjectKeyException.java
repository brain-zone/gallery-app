package net.matrix.gallery.storage;

/** Raised when an artwork object key is not a safe, normalized relative path. */
public class InvalidArtworkObjectKeyException extends IllegalArgumentException {

  public InvalidArtworkObjectKeyException(String message) {
    super(message);
  }

  public InvalidArtworkObjectKeyException(String message, Throwable cause) {
    super(message, cause);
  }
}
