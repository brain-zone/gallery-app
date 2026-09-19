package net.matrix.gallery.web;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import net.matrix.gallery.domain.value.ArtworkSummary;
import net.matrix.gallery.domain.value.CategoryDetail;
import net.matrix.gallery.domain.value.CategorySummary;
import net.matrix.gallery.service.CategoryService;
import net.matrix.gallery.service.GalleryResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class CategoryControllerTest {

  @Mock private CategoryService categoryService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    var viewResolver = new InternalResourceViewResolver("/WEB-INF/templates/", ".html");
    mockMvc =
        MockMvcBuilders.standaloneSetup(new CategoryController(categoryService))
            .setViewResolvers(viewResolver)
            .build();
  }

  @Test
  void rendersCategoriesViewWithCategorySummaries() throws Exception {
    var summary = new CategorySummary(7L, "Landscapes", 3L);
    when(categoryService.listCategories()).thenReturn(List.of(summary));

    mockMvc
        .perform(get("/categories"))
        .andExpect(status().isOk())
        .andExpect(view().name("categories"))
        .andExpect(model().attribute("categories", contains(summary)));
  }

  @Test
  void rendersCategoryDetailWithArtworkSummary() throws Exception {
    var detail =
        new CategoryDetail(
            5L,
            "Landscapes",
            "Landscape works",
            List.of(new ArtworkSummary(9L, "Evening Sky", "Sunset series")));
    when(categoryService.getCategory(5L)).thenReturn(detail);

    mockMvc
        .perform(get("/categories/5"))
        .andExpect(status().isOk())
        .andExpect(view().name("category-detail"))
        .andExpect(model().attribute("category", detail));
  }

  @Test
  void returnsNotFoundForMissingCategory() throws Exception {
    when(categoryService.getCategory(404L))
        .thenThrow(new GalleryResourceNotFoundException("Category 404 was not found"));

    mockMvc.perform(get("/categories/404")).andExpect(status().isNotFound());
  }
}
