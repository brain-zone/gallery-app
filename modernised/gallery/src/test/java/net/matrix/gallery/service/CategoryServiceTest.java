package net.matrix.gallery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import net.matrix.gallery.domain.model.ArtEntity;
import net.matrix.gallery.domain.model.Category;
import net.matrix.gallery.domain.value.CategorySummary;
import net.matrix.gallery.domain.value.ImageRendition;
import net.matrix.gallery.domain.value.RenditionType;
import net.matrix.gallery.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

  @Mock private CategoryRepository categoryRepository;

  @InjectMocks private CategoryService categoryService;

  @Test
  void returnsPopulatedCategorySummariesFromRepository() {
    var expected = List.of(new CategorySummary(7L, "Landscapes", 3L));
    when(categoryRepository.listCategorySummaries()).thenReturn(expected);

    assertThat(categoryService.listCategories()).isEqualTo(expected);
    verify(categoryRepository).listCategorySummaries();
  }

  @Test
  void returnsEmptyResultFromRepository() {
    when(categoryRepository.listCategorySummaries()).thenReturn(List.of());

    assertThat(categoryService.listCategories()).isEmpty();
    verify(categoryRepository).listCategorySummaries();
  }

  @Test
  void mapsCategoryAndArtworksToDetail() {
    Category category = category(5L, "Landscapes", "Landscape works");
    ArtEntity artwork = artwork(9L, "Evening Sky", "Sunset series");
    addRendition(artwork, RenditionType.ORIGINAL, "original-evening-sky.jpg");
    addRendition(artwork, RenditionType.THUMBNAIL, "thumbnail-evening-sky.jpg");
    artwork.addCategory(category);
    when(categoryRepository.findWithArtEntitiesById(5L)).thenReturn(Optional.of(category));

    var detail = categoryService.getCategory(5L);

    assertThat(detail.id()).isEqualTo(5L);
    assertThat(detail.categoryName()).isEqualTo("Landscapes");
    assertThat(detail.categoryDescription()).isEqualTo("Landscape works");
    assertThat(detail.artworks()).hasSize(1);
    assertThat(detail.artworks().getFirst().id()).isEqualTo(9L);
    assertThat(detail.artworks().getFirst().title()).isEqualTo("Evening Sky");
    assertThat(detail.artworks().getFirst().subTitle()).isEqualTo("Sunset series");
    assertThat(detail.artworks().getFirst().imageUrl())
        .isEqualTo("/artworks/images/thumbnail-evening-sky.jpg");
    verify(categoryRepository).findWithArtEntitiesById(5L);
  }

  @Test
  void mapsCategoryWithoutArtworks() {
    Category category = category(5L, "Landscapes", null);
    when(categoryRepository.findWithArtEntitiesById(5L)).thenReturn(Optional.of(category));

    assertThat(categoryService.getCategory(5L).artworks()).isEmpty();
  }

  @Test
  void categoryViewerSelectsFirstDeterministicallyOrderedArtwork() {
    Category category = category(5L, "Landscapes", "Landscape works");
    artwork(10L, "Zulu Sky", null).addCategory(category);
    artwork(9L, "alpha Field", null).addCategory(category);
    when(categoryRepository.findWithArtEntitiesById(5L)).thenReturn(Optional.of(category));

    var viewer = categoryService.getCategoryViewer(5L);

    assertThat(viewer.artworks()).extracting(artwork -> artwork.id()).containsExactly(9L, 10L);
    assertThat(viewer.selectedArtwork().id()).isEqualTo(9L);
  }

  @Test
  void categoryViewerSelectsRequestedArtworkAndRetainsCategoryContext() {
    Category category = category(5L, "Landscapes", "Landscape works");
    ArtEntity alpha = artwork(9L, "Alpha Field", null);
    alpha.setDescription("A quiet field at dawn.");
    alpha.addCategory(category);
    ArtEntity zulu = artwork(10L, "Zulu Sky", null);
    zulu.setDescription("A dramatic evening sky.");
    zulu.addCategory(category);
    when(categoryRepository.findWithArtEntitiesById(5L)).thenReturn(Optional.of(category));

    var initialViewer = categoryService.getCategoryViewer(5L);
    var changedViewer = categoryService.getCategoryViewer(5L, 10L);

    assertThat(initialViewer.selectedArtwork().title()).isEqualTo("Alpha Field");
    assertThat(initialViewer.selectedArtworkDescription()).isEqualTo("A quiet field at dawn.");
    assertThat(changedViewer.id()).isEqualTo(5L);
    assertThat(changedViewer.categoryName()).isEqualTo("Landscapes");
    assertThat(changedViewer.artworks())
        .extracting(artwork -> artwork.id())
        .containsExactly(9L, 10L);
    assertThat(changedViewer.selectedArtwork().id()).isEqualTo(10L);
    assertThat(changedViewer.selectedArtwork().title()).isEqualTo("Zulu Sky");
    assertThat(changedViewer.selectedArtworkDescription()).isEqualTo("A dramatic evening sky.");
    verify(categoryRepository, org.mockito.Mockito.times(2)).findWithArtEntitiesById(5L);
  }

  @Test
  void categoryViewerUsesDistinctRenditionPrioritiesForThumbnailDisplayAndLink() {
    Category category = category(5L, "Landscapes", "Landscape works");
    ArtEntity artwork = artwork(9L, "Evening Sky", null);
    artwork.setArtist("Legacy Artist");
    addRendition(artwork, RenditionType.ORIGINAL, "original.jpg");
    addRendition(artwork, RenditionType.THUMBNAIL, "thumbnail.jpg");
    artwork.addCategory(category);
    when(categoryRepository.findWithArtEntitiesById(5L)).thenReturn(Optional.of(category));

    var viewer = categoryService.getCategoryViewer(5L);

    assertThat(viewer.selectedArtwork().imageUrl()).isEqualTo("/artworks/images/thumbnail.jpg");
    assertThat(viewer.selectedArtworkArtist()).isEqualTo("Legacy Artist");
    assertThat(viewer.featuredImageUrl()).isEqualTo("/artworks/images/evening-sky.jpg");
    assertThat(viewer.featuredImageLinkUrl()).isEqualTo("/artworks/images/original.jpg");
    verify(categoryRepository).findWithArtEntitiesById(5L);
  }

  @Test
  void categoryViewerFallsBackAcrossAvailableRenditions() {
    Category category = category(5L, "Landscapes", "Landscape works");
    ArtEntity originalOnly = artworkWithoutRendition(9L, "Alpha", null);
    addRendition(originalOnly, RenditionType.ORIGINAL, "alpha-original.jpg");
    originalOnly.addCategory(category);
    ArtEntity thumbnailOnly = artworkWithoutRendition(10L, "Beta", null);
    addRendition(thumbnailOnly, RenditionType.THUMBNAIL, "beta-thumbnail.jpg");
    thumbnailOnly.addCategory(category);
    ArtEntity noImage = artworkWithoutRendition(11L, "Gamma", null);
    noImage.addCategory(category);
    ArtEntity blankThumbnail = artworkWithoutRendition(12L, "Omega", null);
    blankThumbnail.addImageRendition(
        RenditionType.THUMBNAIL, new ImageRendition(" ", "image/jpeg", 10L, 1200, 800, "checksum"));
    addRendition(blankThumbnail, RenditionType.GALLERY, "omega-gallery.jpg");
    blankThumbnail.addCategory(category);
    when(categoryRepository.findWithArtEntitiesById(5L)).thenReturn(Optional.of(category));

    var originalViewer = categoryService.getCategoryViewer(5L, 9L);
    var thumbnailViewer = categoryService.getCategoryViewer(5L, 10L);
    var noImageViewer = categoryService.getCategoryViewer(5L, 11L);
    var blankThumbnailViewer = categoryService.getCategoryViewer(5L, 12L);

    assertThat(originalViewer.selectedArtwork().imageUrl())
        .isEqualTo("/artworks/images/alpha-original.jpg");
    assertThat(originalViewer.featuredImageUrl()).isEqualTo("/artworks/images/alpha-original.jpg");
    assertThat(originalViewer.featuredImageLinkUrl())
        .isEqualTo("/artworks/images/alpha-original.jpg");
    assertThat(thumbnailViewer.selectedArtwork().imageUrl())
        .isEqualTo("/artworks/images/beta-thumbnail.jpg");
    assertThat(thumbnailViewer.featuredImageUrl()).isEqualTo("/artworks/images/beta-thumbnail.jpg");
    assertThat(thumbnailViewer.featuredImageLinkUrl())
        .isEqualTo("/artworks/images/beta-thumbnail.jpg");
    assertThat(noImageViewer.selectedArtwork().imageUrl()).isNull();
    assertThat(noImageViewer.featuredImageUrl()).isNull();
    assertThat(noImageViewer.featuredImageLinkUrl()).isNull();
    assertThat(blankThumbnailViewer.selectedArtwork().imageUrl())
        .isEqualTo("/artworks/images/omega-gallery.jpg");
  }

  @Test
  void categoryViewerProvidesPreviousAndNextWithoutWrapping() {
    Category category = category(5L, "Landscapes", "Landscape works");
    artwork(9L, "Alpha Field", null).addCategory(category);
    artwork(10L, "Middle Meadow", null).addCategory(category);
    artwork(11L, "Zulu Sky", null).addCategory(category);
    when(categoryRepository.findWithArtEntitiesById(5L)).thenReturn(Optional.of(category));

    var first = categoryService.getCategoryViewer(5L, 9L);
    var middle = categoryService.getCategoryViewer(5L, 10L);
    var last = categoryService.getCategoryViewer(5L, 11L);

    assertThat(first.previousArtwork()).isNull();
    assertThat(first.nextArtwork().id()).isEqualTo(10L);
    assertThat(middle.previousArtwork().id()).isEqualTo(9L);
    assertThat(middle.nextArtwork().id()).isEqualTo(11L);
    assertThat(last.previousArtwork().id()).isEqualTo(10L);
    assertThat(last.nextArtwork()).isNull();
  }

  @Test
  void singleArtworkCategoryHasNoPreviousOrNext() {
    Category category = category(5L, "Landscapes", "Landscape works");
    artwork(9L, "Only Artwork", null).addCategory(category);
    when(categoryRepository.findWithArtEntitiesById(5L)).thenReturn(Optional.of(category));

    var viewer = categoryService.getCategoryViewer(5L);

    assertThat(viewer.previousArtwork()).isNull();
    assertThat(viewer.nextArtwork()).isNull();
  }

  @Test
  void categoryViewerRejectsArtworkOutsideRequestedCategory() {
    Category category = category(5L, "Landscapes", "Landscape works");
    artwork(9L, "Alpha Field", null).addCategory(category);
    when(categoryRepository.findWithArtEntitiesById(5L)).thenReturn(Optional.of(category));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> categoryService.getCategoryViewer(5L, 99L))
        .isInstanceOf(GalleryResourceNotFoundException.class)
        .hasMessage("Artwork 99 was not found in category 5");
  }

  @Test
  void missingCategoryRaisesNotFoundException() {
    when(categoryRepository.findWithArtEntitiesById(404L)).thenReturn(Optional.empty());

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> categoryService.getCategory(404L))
        .isInstanceOf(GalleryResourceNotFoundException.class)
        .hasMessage("Category 404 was not found");
  }

  private Category category(long id, String name, String description) {
    Category category = new Category();
    ReflectionTestUtils.setField(category, "id", id);
    category.setCategoryName(name);
    category.setCategoryDescription(description);
    return category;
  }

  private ArtEntity artwork(long id, String title, String subTitle) {
    ArtEntity artwork = artworkWithoutRendition(id, title, subTitle);
    addRendition(artwork, RenditionType.GALLERY, "evening-sky.jpg");
    return artwork;
  }

  private ArtEntity artworkWithoutRendition(long id, String title, String subTitle) {
    ArtEntity artwork = new ArtEntity();
    ReflectionTestUtils.setField(artwork, "id", id);
    artwork.setTitle(title);
    artwork.setSubTitle(subTitle);
    return artwork;
  }

  private void addRendition(ArtEntity artwork, RenditionType type, String fileName) {
    artwork.addImageRendition(
        type,
        new ImageRendition(
            "artworks/images/" + fileName, "image/jpeg", 10L, 1200, 800, "checksum"));
  }
}
