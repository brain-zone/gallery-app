package net.matrix.gallery.service;

import java.util.Comparator;
import java.util.List;
import net.matrix.gallery.domain.model.ArtEntity;
import net.matrix.gallery.domain.model.Category;
import net.matrix.gallery.domain.value.ArtworkImageUrl;
import net.matrix.gallery.domain.value.ArtworkSummary;
import net.matrix.gallery.domain.value.CategoryDetail;
import net.matrix.gallery.domain.value.CategorySummary;
import net.matrix.gallery.domain.value.ImageRendition;
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
    Category category =
        categoryRepository
            .findWithArtEntitiesById(id)
            .orElseThrow(
                () -> new GalleryResourceNotFoundException("Category " + id + " was not found"));

    List<ArtworkSummary> artworks =
        category.getArtEntities().stream()
            .sorted(
                Comparator.comparing(
                        ArtEntity::getTitle, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                    .thenComparing(ArtEntity::getId))
            .map(
                artwork ->
                    new ArtworkSummary(
                        artwork.getId(),
                        artwork.getTitle(),
                        artwork.getSubTitle(),
                        imageUrl(artwork)))
            .toList();

    return new CategoryDetail(
        category.getId(), category.getCategoryName(), category.getCategoryDescription(), artworks);
  }

  private String imageUrl(ArtEntity artwork) {
    ImageRendition rendition = artwork.getImageRendition().get(RenditionType.THUMBNAIL);
    if (rendition == null) {
      rendition = artwork.getImageRendition().get(RenditionType.GALLERY);
    }
    if (rendition == null) {
      rendition = artwork.getImageRendition().get(RenditionType.ORIGINAL);
    }
    return rendition == null ? null : ArtworkImageUrl.fromObjectKey(rendition.objectKey());
  }
}
