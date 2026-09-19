package net.matrix.gallery.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.matrix.gallery.repository.ArtworkRepository;
import net.matrix.gallery.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PublicBrowsingIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private ArtworkRepository artworkRepository;

  private long categoryId;
  private long artworkId;

  @BeforeEach
  void findSeedData() {
    categoryId = categoryRepository.findByCategoryNameIgnoreCase("Landscape").orElseThrow().getId();
    artworkId =
        artworkRepository.findAll().stream()
            .filter(artwork -> "Evening Sky".equals(artwork.getTitle()))
            .findFirst()
            .orElseThrow()
            .getId();
  }

  @Test
  void anonymousVisitorCanBrowseRenderedCategoryAndArtworkPages() throws Exception {
    String categoryHtml =
        mockMvc
            .perform(get("/categories").with(anonymous()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Gallery categories")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(categoryHtml).contains("/categories/" + categoryId, "Landscape");

    mockMvc
        .perform(get("/categories/{id}", categoryId).with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Evening Sky")))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("/artworks/" + artworkId)));

    mockMvc
        .perform(get("/artworks/{id}", artworkId).with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Evening Sky")))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("gallery/evening-sky.jpg")));
  }

  @Test
  void anonymousVisitorCanBrowseCategoryAndArtworkApis() throws Exception {
    mockMvc
        .perform(get("/api/categories").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[?(@.categoryName == 'Landscape')]").exists());

    mockMvc
        .perform(get("/api/categories/{id}", categoryId).with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categoryName").value("Landscape"))
        .andExpect(jsonPath("$.artworks[0].title").value("Evening Sky"));

    mockMvc
        .perform(get("/api/artworks/{id}", artworkId).with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Evening Sky"))
        .andExpect(jsonPath("$.categories[0].categoryName").value("Landscape"));
  }

  @Test
  void anonymousVisitorGetsNotFoundForMissingPublicResources() throws Exception {
    mockMvc
        .perform(get("/categories/{id}", Long.MAX_VALUE).with(anonymous()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/artworks/{id}", Long.MAX_VALUE).with(anonymous()))
        .andExpect(status().isNotFound());
  }

  @Test
  void unrelatedActuatorEndpointRemainsProtected() throws Exception {
    mockMvc.perform(get("/actuator/health").with(anonymous())).andExpect(status().isUnauthorized());
  }
}
