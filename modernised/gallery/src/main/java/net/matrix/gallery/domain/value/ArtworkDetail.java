package net.matrix.gallery.domain.value;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Complete read model for the public artwork detail routes. */
public record ArtworkDetail(
    Long id,
    String title,
    String subTitle,
    Instant uploadedDate,
    LocalDate displayDate,
    int width,
    int height,
    String media,
    String description,
    String caption,
    boolean generalViewable,
    boolean privilegeViewable,
    List<CategoryReference> categories,
    List<RenditionDetail> renditions) {

  public ArtworkDetail {
    categories = List.copyOf(categories);
    renditions = List.copyOf(renditions);
  }
}
