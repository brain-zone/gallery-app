package net.matrix.gallery.service;

import java.util.Comparator;
import java.util.List;
import net.matrix.gallery.domain.model.ArtEntity;
import net.matrix.gallery.domain.value.ArtworkDetail;
import net.matrix.gallery.domain.value.CategoryReference;
import net.matrix.gallery.domain.value.RenditionDetail;
import net.matrix.gallery.repository.ArtworkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Provides read-only artwork browsing operations. */
@Service
@Transactional(readOnly = true)
public class ArtworkService {

  private final ArtworkRepository artworkRepository;

  public ArtworkService(ArtworkRepository artworkRepository) {
    this.artworkRepository = artworkRepository;
  }

  public ArtworkDetail getArtwork(long id) {
    ArtEntity artwork =
        artworkRepository
            .findDetailById(id)
            .orElseThrow(
                () -> new GalleryResourceNotFoundException("Artwork " + id + " was not found"));

    List<CategoryReference> categories =
        artwork.getCategories().stream()
            .sorted(Comparator.comparing(category -> category.getCategoryName().toLowerCase()))
            .map(category -> new CategoryReference(category.getId(), category.getCategoryName()))
            .toList();
    List<RenditionDetail> renditions =
        artwork.getImageRendition().entrySet().stream()
            .sorted(java.util.Map.Entry.comparingByKey())
            .map(
                entry ->
                    new RenditionDetail(
                        entry.getKey(),
                        entry.getValue().objectKey(),
                        entry.getValue().contentType(),
                        entry.getValue().sizeBytes(),
                        entry.getValue().width(),
                        entry.getValue().height(),
                        entry.getValue().checksum()))
            .toList();

    return new ArtworkDetail(
        artwork.getId(),
        artwork.getTitle(),
        artwork.getSubTitle(),
        artwork.getUploadedDate(),
        artwork.getDisplayDate(),
        artwork.getWidth(),
        artwork.getHeight(),
        artwork.getMedia(),
        artwork.getDescription(),
        artwork.getCaption(),
        artwork.isGeneralViewable(),
        artwork.isPrivilegeViewable(),
        categories,
        renditions);
  }
}
