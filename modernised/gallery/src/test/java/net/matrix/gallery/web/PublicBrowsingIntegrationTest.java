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

  private static final String LANDSCAPE_ORIGINAL_IMAGE_PATH =
      "/artworks/images/3F1ED738-72F7-4AFA-B6B2-969A545AC8DD.jpeg";
  private static final String LANDSCAPE_GALLERY_IMAGE_PATH =
      "/artworks/images/g-3F1ED738-72F7-4AFA-B6B2-969A545AC8DD.jpeg";
  private static final String LANDSCAPE_THUMBNAIL_IMAGE_PATH =
      "/artworks/images/t-3F1ED738-72F7-4AFA-B6B2-969A545AC8DD.jpeg";

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
  void anonymousVisitorCanOpenHomePage() throws Exception {
    mockMvc
        .perform(get("/").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("home-shell")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("/categories")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("/images/indeximage.jpg")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("/images/ind_exhib.gif")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("/virtualExhibitions"))));
  }

  @Test
  void anonymousVisitorCanBrowseRenderedCategoryAndArtworkPages() throws Exception {
    String categoryHtml =
        mockMvc
            .perform(get("/categories").with(anonymous()))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(
                content().string(org.hamcrest.Matchers.containsString("Select a gallery below:")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("site-header")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("category-grid")))
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
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "/categories/" + categoryId + "/artworks/" + artworkId)))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "href=\"" + LANDSCAPE_ORIGINAL_IMAGE_PATH + "\"")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "src=\"" + LANDSCAPE_GALLERY_IMAGE_PATH + "\"")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "src=\"" + LANDSCAPE_THUMBNAIL_IMAGE_PATH + "\"")));

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
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString(LANDSCAPE_GALLERY_IMAGE_PATH)));
  }

  @Test
  void categoryViewerSelectsWithinCategoryContextAndRejectsOtherCategoryArtwork() throws Exception {
    long abstractCategoryId =
        categoryRepository.findByCategoryNameIgnoreCase("Abstract").orElseThrow().getId();
    long cosmicDanceId =
        artworkRepository
            .findByCatalogKeyIgnoreCase("cosmic_dance_painting.jpg")
            .orElseThrow()
            .getId();
    long gardenOfLightId =
        artworkRepository
            .findByCatalogKeyIgnoreCase("garden_of_light_painting.jpg")
            .orElseThrow()
            .getId();
    long abstractDreamId =
        artworkRepository
            .findByCatalogKeyIgnoreCase("abstract_dream_painting.jpg")
            .orElseThrow()
            .getId();

    mockMvc
        .perform(get("/categories/{id}", abstractCategoryId).with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "<h2 class=\"gallery-image-title\">Chromatic Vortex</h2>")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "/categories/" + abstractCategoryId + "/artworks/" + cosmicDanceId)))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "aria-label=\"Next artwork: Cosmic Dance\"")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("aria-label=\"Previous artwork:"))));

    mockMvc
        .perform(
            get(
                    "/categories/{categoryId}/artworks/{artworkId}",
                    abstractCategoryId,
                    gardenOfLightId)
                .with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString(">Abstract</span>")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "<h2 class=\"gallery-image-title\">In the Garden of Light</h2>")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "/categories/" + abstractCategoryId + "/artworks/" + cosmicDanceId)))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "/categories/" + abstractCategoryId + "/artworks/" + gardenOfLightId)))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "/categories/" + abstractCategoryId + "/artworks/" + abstractDreamId)))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "aria-label=\"Previous artwork: Cosmic Dance\"")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "aria-label=\"Next artwork: The Abstract Dream\"")));

    mockMvc
        .perform(
            get(
                    "/categories/{categoryId}/artworks/{artworkId}",
                    abstractCategoryId,
                    abstractDreamId)
                .with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "aria-label=\"Previous artwork: In the Garden of Light\"")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("aria-label=\"Next artwork:"))));

    mockMvc
        .perform(
            get("/categories/{categoryId}/artworks/{artworkId}", abstractCategoryId, artworkId)
                .with(anonymous()))
        .andExpect(status().isNotFound());
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
        .andExpect(jsonPath("$.primaryImageUrl").value(LANDSCAPE_GALLERY_IMAGE_PATH))
        .andExpect(jsonPath("$.renditions[0].url").value(LANDSCAPE_ORIGINAL_IMAGE_PATH))
        .andExpect(jsonPath("$.categories[0].categoryName").value("Landscape"));
  }

  @Test
  void anonymousVisitorCanLoadCuratedImageBytes() throws Exception {
    mockMvc
        .perform(get(LANDSCAPE_ORIGINAL_IMAGE_PATH).with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));
    mockMvc
        .perform(get("/artworks/images/missing.jpeg").with(anonymous()))
        .andExpect(status().isNotFound());
  }

  @Test
  void anonymousVisitorCanLoadSiteShellAssets() throws Exception {
    mockMvc
        .perform(get("/images/background.jpg").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));
    mockMvc
        .perform(get("/images/int_galleries.gif").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_GIF));
    mockMvc
        .perform(get("/images/indexlogo.gif").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_GIF));
    mockMvc
        .perform(get("/images/indeximage.jpg").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));
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

  @Test
  void realHttpErrorDispatchRendersBrandedNotFoundAndAuthenticationPages() throws Exception {
    HttpResponse<String> notFound = htmlResponse("/categories/" + Long.MAX_VALUE);
    assertThat(notFound.statusCode()).isEqualTo(404);
    assertThat(notFound.body())
        .contains("Page not found", "Return to the public gallery")
        .doesNotContain("GalleryResourceNotFoundException", "Category 9223372036854775807");

    HttpResponse<String> unauthorized = httpResponse("/actuator/health");
    assertThat(unauthorized.statusCode()).isEqualTo(401);
    assertThat(unauthorized.headers().firstValue("WWW-Authenticate"))
        .hasValue("Basic realm=\"Realm\"");
    assertThat(unauthorized.body())
        .contains("Authentication required", "return to the public gallery")
        .doesNotContain("Exception", "stack trace");
  }

  private int httpStatus(String path) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
    return HttpClient.newHttpClient()
        .send(request, HttpResponse.BodyHandlers.discarding())
        .statusCode();
  }

  private HttpResponse<String> httpResponse(String path) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> htmlResponse(String path) throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
            .header("Accept", MediaType.TEXT_HTML_VALUE)
            .GET()
            .build();
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
  }
}
