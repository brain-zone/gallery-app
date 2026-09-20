package net.matrix.gallery.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import net.matrix.gallery.repository.ArtworkRepository;
import net.matrix.gallery.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class PublicBrowsingIntegrationTest {

  private static final String LANDSCAPE_IMAGE_PATH =
      "/artworks/images/3F1ED738-72F7-4AFA-B6B2-969A545AC8DD.jpeg";

  @Autowired private MockMvc mockMvc;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private ArtworkRepository artworkRepository;

  @LocalServerPort private int port;

  private long categoryId;
  private long artworkId;

  @BeforeEach
  void findSeedData() {
    categoryId = categoryRepository.findByCategoryNameIgnoreCase("Landscape").orElseThrow().getId();
    artworkId =
        artworkRepository
            .findByCatalogKeyIgnoreCase("sunrise_mountain_photograph.jpg")
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
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Sunrise over the Mountain")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("/artworks/" + artworkId)))
        .andExpect(content().string(org.hamcrest.Matchers.containsString(LANDSCAPE_IMAGE_PATH)));

    mockMvc
        .perform(get("/artworks/{id}", artworkId).with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Sunrise over the Mountain")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Maya Vasquez")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString(">Genre</dt>")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Nature")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(">Media</dt>"))))
        .andExpect(content().string(org.hamcrest.Matchers.containsString(LANDSCAPE_IMAGE_PATH)));
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
        .andExpect(jsonPath("$.artworks[0].title").value("Sunrise over the Mountain"));

    mockMvc
        .perform(get("/api/artworks/{id}", artworkId).with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Sunrise over the Mountain"))
        .andExpect(jsonPath("$.artist").value("Maya Vasquez"))
        .andExpect(jsonPath("$.genre").value("Nature"))
        .andExpect(jsonPath("$.media").doesNotExist())
        .andExpect(jsonPath("$.primaryImageUrl").value(LANDSCAPE_IMAGE_PATH))
        .andExpect(jsonPath("$.renditions[0].url").value(LANDSCAPE_IMAGE_PATH))
        .andExpect(jsonPath("$.categories[0].categoryName").value("Landscape"));
  }

  @Test
  void anonymousVisitorCanLoadCuratedImageBytes() throws Exception {
    mockMvc
        .perform(get(LANDSCAPE_IMAGE_PATH).with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));
    mockMvc
        .perform(get("/artworks/images/missing.jpeg").with(anonymous()))
        .andExpect(status().isNotFound());
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

  @Test
  void realHttpErrorDispatchPreservesPublicNotFoundResponses() throws Exception {
    assertThat(httpStatus("/categories/" + Long.MAX_VALUE)).isEqualTo(404);
    assertThat(httpStatus("/api/categories/" + Long.MAX_VALUE)).isEqualTo(404);
    assertThat(httpStatus("/artworks/" + Long.MAX_VALUE)).isEqualTo(404);
    assertThat(httpStatus("/api/artworks/" + Long.MAX_VALUE)).isEqualTo(404);
    assertThat(httpStatus("/actuator/health")).isEqualTo(401);
  }

  private int httpStatus(String path) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
    return HttpClient.newHttpClient()
        .send(request, HttpResponse.BodyHandlers.discarding())
        .statusCode();
  }
}
