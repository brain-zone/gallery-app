package net.matrix.gallery.domain.value;

/** Converts a stored static-resource object key into a browser-usable URL. */
public final class ArtworkImageUrl {

  private ArtworkImageUrl() {}

  public static String fromObjectKey(String objectKey) {
    if (objectKey == null || objectKey.isBlank()) {
      return null;
    }
    return objectKey.startsWith("/") ? objectKey : "/" + objectKey;
  }
}
