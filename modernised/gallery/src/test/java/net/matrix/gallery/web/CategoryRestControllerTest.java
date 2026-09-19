package net.matrix.gallery.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import net.matrix.gallery.domain.value.CategorySummary;
import net.matrix.gallery.service.CategoryService;
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
  void setUp() {
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
}
