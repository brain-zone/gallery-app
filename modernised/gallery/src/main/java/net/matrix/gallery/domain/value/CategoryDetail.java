package net.matrix.gallery.domain.value;

import java.util.List;

/** Category information and its artwork browsing entries. */
public record CategoryDetail(
    Long id, String categoryName, String categoryDescription, List<ArtworkSummary> artworks) {

  public CategoryDetail {
    artworks = List.copyOf(artworks);
  }
}
