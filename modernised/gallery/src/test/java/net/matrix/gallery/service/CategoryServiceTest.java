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
        .isEqualTo("/artworks/images/evening-sky.jpg");
    verify(categoryRepository).findWithArtEntitiesById(5L);
  }

  @Test
  void mapsCategoryWithoutArtworks() {
    Category category = category(5L, "Landscapes", null);
    when(categoryRepository.findWithArtEntitiesById(5L)).thenReturn(Optional.of(category));

    assertThat(categoryService.getCategory(5L).artworks()).isEmpty();
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
    ArtEntity artwork = new ArtEntity();
    ReflectionTestUtils.setField(artwork, "id", id);
    artwork.setTitle(title);
    artwork.setSubTitle(subTitle);
    artwork.addImageRendition(
        RenditionType.GALLERY,
        new ImageRendition(
            "artworks/images/evening-sky.jpg", "image/jpeg", 10L, 1200, 800, "checksum"));
    return artwork;
  }
}
