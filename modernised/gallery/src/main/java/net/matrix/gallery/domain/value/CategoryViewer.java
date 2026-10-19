package net.matrix.gallery.domain.value;

import java.util.List;

/** Category-scoped artwork viewer with an ordered artwork list and current selection. */
public record CategoryViewer(
    Long id,
    String categoryName,
    String categoryDescription,
    List<ArtworkSummary> artworks,
    List<LightboxArtwork> lightboxArtworks,
    ArtworkSummary selectedArtwork,
    ArtworkSummary previousArtwork,
    ArtworkSummary nextArtwork,
    String selectedArtworkArtist,
    String selectedArtworkDescription,
    String featuredImageUrl,
    String featuredImageLinkUrl) {

  public CategoryViewer {
    artworks = List.copyOf(artworks);
    lightboxArtworks = List.copyOf(lightboxArtworks);
  }

  public CategoryViewer(
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
    this(
        id,
        categoryName,
        categoryDescription,
        artworks,
        List.of(),
        selectedArtwork,
        previousArtwork,
        nextArtwork,
        selectedArtworkArtist,
        selectedArtworkDescription,
        featuredImageUrl,
        featuredImageLinkUrl);
  }
}
