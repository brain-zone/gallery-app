package net.matrix.gallery.domain.value;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.web.util.UriUtils;

/** Converts a logical artwork object key into an application-origin media URL. */
public final class ArtworkImageUrl {

  private ArtworkImageUrl() {}

  public static String fromObjectKey(String objectKey) {
    if (objectKey == null || objectKey.isBlank()) {
      return null;
    }
    String normalized = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
    String encoded =
        Arrays.stream(normalized.split("/", -1))
            .map(segment -> UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8))
            .collect(Collectors.joining("/"));
    return "/media/" + encoded;
  }
}
