package net.matrix.gallery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import net.matrix.gallery.domain.value.CategorySummary;
import net.matrix.gallery.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
