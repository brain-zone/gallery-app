package net.matrix.gallery.service;

import java.util.Comparator;
import java.util.List;
import net.matrix.gallery.domain.model.ArtEntity;
import net.matrix.gallery.domain.model.Category;
import net.matrix.gallery.domain.value.ArtworkImageUrl;
import net.matrix.gallery.domain.value.ArtworkSummary;
import net.matrix.gallery.domain.value.CategoryDetail;
import net.matrix.gallery.domain.value.CategorySummary;
import net.matrix.gallery.domain.value.CategoryViewer;
import net.matrix.gallery.domain.value.ImageRendition;
import net.matrix.gallery.domain.value.LightboxArtwork;
import net.matrix.gallery.domain.value.RenditionType;
import net.matrix.gallery.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Provides read-only category browsing operations. */
@Service
@Transactional(readOnly = true)
public class CategoryService {

  private final CategoryRepository categoryRepository;

  public CategoryService(CategoryRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
  }

  public List<CategorySummary> listCategories() {
    return categoryRepository.listCategorySummaries();
  }

  public CategoryDetail getCategory(long id) {
    Category category = findCategory(id);
    return toDetail(category, orderedArtworks(category));
  }

  public CategoryViewer getCategoryViewer(long categoryId) {
    Category category = findCategory(categoryId);
    List<ArtEntity> artworks = orderedArtworks(category);
    return toViewer(category, artworks, artworks.isEmpty() ? -1 : 0);
  }

  public CategoryViewer getCategoryViewer(long categoryId, long artworkId) {
    Category category = findCategory(categoryId);
    List<ArtEntity> artworks = orderedArtworks(category);
    int selectedIndex = -1;
    for (int index = 0; index < artworks.size(); index++) {
      if (artworks.get(index).getId() == artworkId) {
        selectedIndex = index;
        break;
      }
    }
    if (selectedIndex < 0) {
      throw new GalleryResourceNotFoundException(
          "Artwork " + artworkId + " was not found in category " + categoryId);
    }
    return toViewer(category, artworks, selectedIndex);
  }

  private Category findCategory(long id) {
    return categoryRepository
        .findWithArtEntitiesById(id)
        .orElseThrow(
            () -> new GalleryResourceNotFoundException("Category " + id + " was not found"));
  }

  private List<ArtEntity> orderedArtworks(Category category) {
    return category.getArtEntities().stream()
        .sorted(
            Comparator.comparing(
                    ArtEntity::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(ArtEntity::getId))
        .toList();
  }

  private CategoryDetail toDetail(Category category, List<ArtEntity> artworks) {
    return new CategoryDetail(
        category.getId(),
        category.getCategoryName(),
        category.getCategoryDescription(),
        artworks.stream().map(this::toSummary).toList());
  }

  private ArtworkSummary toSummary(ArtEntity artwork) {
    return new ArtworkSummary(
        artwork.getId(),
        artwork.getTitle(),
        artwork.getSubTitle(),
        imageUrl(artwork, RenditionType.THUMBNAIL, RenditionType.GALLERY, RenditionType.ORIGINAL));
  }

  private CategoryViewer toViewer(Category category, List<ArtEntity> artworks, int selectedIndex) {
    CategoryDetail detail = toDetail(category, artworks);
    ArtworkSummary selectedArtwork =
        selectedIndex >= 0 ? detail.artworks().get(selectedIndex) : null;
    ArtworkSummary previousArtwork =
        selectedIndex > 0 ? detail.artworks().get(selectedIndex - 1) : null;
    ArtworkSummary nextArtwork =
        selectedIndex >= 0 && selectedIndex < detail.artworks().size() - 1
            ? detail.artworks().get(selectedIndex + 1)
            : null;
    ArtEntity selectedEntity = selectedIndex >= 0 ? artworks.get(selectedIndex) : null;

    return new CategoryViewer(
        detail.id(),
        detail.categoryName(),
        detail.categoryDescription(),
        detail.artworks(),
        artworks.stream().map(this::toLightboxArtwork).toList(),
        selectedArtwork,
        previousArtwork,
        nextArtwork,
        selectedEntity == null ? null : selectedEntity.getArtist(),
        selectedEntity == null ? null : selectedEntity.getDescription(),
        selectedEntity == null
            ? null
            : imageUrl(
                selectedEntity,
                RenditionType.GALLERY,
                RenditionType.ORIGINAL,
                RenditionType.THUMBNAIL),
        selectedEntity == null
            ? null
            : imageUrl(
                selectedEntity,
                RenditionType.ORIGINAL,
                RenditionType.GALLERY,
                RenditionType.THUMBNAIL));
  }

  private LightboxArtwork toLightboxArtwork(ArtEntity artwork) {
    return new LightboxArtwork(
        artwork.getId(),
        artwork.getTitle(),
        artwork.getArtist(),
        imageUrl(artwork, RenditionType.ORIGINAL, RenditionType.GALLERY, RenditionType.THUMBNAIL));
  }

  private String imageUrl(ArtEntity artwork, RenditionType... priorities) {
    for (RenditionType priority : priorities) {
      ImageRendition rendition = artwork.getImageRendition().get(priority);
      if (rendition != null) {
        String url = ArtworkImageUrl.fromObjectKey(rendition.objectKey());
        if (url != null) {
          return url;
        }
      }
    }
    return null;
  }
}
