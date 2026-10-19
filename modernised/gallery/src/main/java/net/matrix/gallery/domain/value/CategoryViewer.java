package net.matrix.gallery.domain.value;

import java.util.List;

/** Category-scoped artwork viewer with an ordered artwork list and current selection. */
public record CategoryViewer(
    Long id,
    String categoryName,
    String categoryDescription,
    List<ArtworkSummary> artworks,
    ArtworkSummary selectedArtwork,
    ArtworkSummary previousArtwork,
    ArtworkSummary nextArtwork,
    String selectedArtworkArtist,
    String selectedArtworkDescription,
    String featuredImageUrl,
    String featuredImageLinkUrl) {

  public CategoryViewer {
    artworks = List.copyOf(artworks);
  }
}
