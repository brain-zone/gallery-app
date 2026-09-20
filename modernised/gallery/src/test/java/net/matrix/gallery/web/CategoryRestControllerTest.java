package net.matrix.gallery.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CategoryRestControllerTest {

  @Mock private CategoryService categoryService;

  private MockMvc mockMvc;

  @BeforeEach
  void configureMockMvc() {
    MockitoAnnotations.openMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(new CategoryRestController(categoryService)).build();
  }

  @Test
  void returnsCategorySummariesAsJson() throws Exception {
    when(categoryService.listCategories())
        .thenReturn(List.of(new CategorySummary(7L, "Landscapes", 3L)));

    mockMvc
        .perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].id").value(7))
        .andExpect(jsonPath("$[0].categoryName").value("Landscapes"))
        .andExpect(jsonPath("$[0].artEntityCount").value(3));
  }

  @Test
  void returnsCategoryDetailAndArtworksAsJson() throws Exception {
    when(categoryService.getCategory(5L))
        .thenReturn(
            new CategoryDetail(
                5L,
                "Landscapes",
                "Landscape works",
                List.of(new ArtworkSummary(9L, "Evening Sky", "Sunset series"))));

    mockMvc
        .perform(get("/api/categories/5"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(5))
        .andExpect(jsonPath("$.categoryName").value("Landscapes"))
        .andExpect(jsonPath("$.categoryDescription").value("Landscape works"))
        .andExpect(jsonPath("$.artworks[0].id").value(9))
        .andExpect(jsonPath("$.artworks[0].title").value("Evening Sky"));
  }

  @Test
  void returnsNotFoundForMissingCategory() throws Exception {
    when(categoryService.getCategory(404L))
        .thenThrow(new GalleryResourceNotFoundException("Category 404 was not found"));

    mockMvc.perform(get("/api/categories/404")).andExpect(status().isNotFound());
  }
}
