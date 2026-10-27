package net.matrix.gallery.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "GALLERY_ADMIN_BOOTSTRAP_PASSWORD=test-bootstrap-password")
@AutoConfigureMockMvc
class PublicBrowsingIntegrationTest {

  private static final String LANDSCAPE_ORIGINAL_IMAGE_PATH =
      "/media/artworks/images/3F1ED738-72F7-4AFA-B6B2-969A545AC8DD/original.jpeg";
  private static final String LANDSCAPE_GALLERY_IMAGE_PATH =
      "/media/artworks/images/3F1ED738-72F7-4AFA-B6B2-969A545AC8DD/gallery.jpeg";
  private static final String LANDSCAPE_THUMBNAIL_IMAGE_PATH =
      "/media/artworks/images/3F1ED738-72F7-4AFA-B6B2-969A545AC8DD/thumbnail.jpeg";

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
  void sharedPublicNavigationMapsEveryHtmlRoute() throws Exception {
    for (String path :
        java.util.List.of(
            "/",
            "/categories",
            "/categories/" + categoryId,
            "/categories/" + categoryId + "/artworks/" + artworkId,
            "/artworks/" + artworkId,
            "/bio")) {
      var response = htmlResponse(path);
      assertThat(response.statusCode()).isEqualTo(200);
      assertPublicNavigation(
          response.body(), path.equals("/") ? "/" : path.equals("/bio") ? "/bio" : "/categories");
    }
    for (String path :
        java.util.List.of("/categories/" + Long.MAX_VALUE, "/artworks/" + Long.MAX_VALUE)) {
      var response = htmlResponse(path);
      assertThat(response.statusCode()).isIn(401, 404);
      assertPublicNavigation(response.body(), null);
    }
    var unauthorized = httpResponse("/actuator/health");
    assertThat(unauthorized.statusCode()).isEqualTo(401);
    assertPublicNavigation(unauthorized.body(), null);
  }

  @Test
  void sharedNavigationPreservesAdministratorAccountActions() throws Exception {
    for (String path : java.util.List.of("/", "/categories", "/bio")) {
      var response =
          mockMvc
              .perform(
                  get(path)
                      .with(
                          org.springframework.security.test.web.servlet.request
                              .SecurityMockMvcRequestPostProcessors.user("admin")
                              .roles("ADMIN")))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse();
      var document = org.jsoup.Jsoup.parse(response.getContentAsString());
      assertThat(document.select("nav[aria-label=Account] a[href=/admin]")).hasSize(1);
      assertThat(document.select("nav[aria-label=Account] a[href=/login]")).isEmpty();
      assertThat(
              document.select(
                  "nav[aria-label=Account] form[action=/logout][method=post] button[type=submit]"))
          .hasSize(1);
      assertThat(document.select("nav[aria-label=Account] form input[name=_csrf]")).hasSize(1);
    }
  }

  private void assertPublicNavigation(String html, String currentHref) {
    var document = org.jsoup.Jsoup.parse(html);
    var navigation = document.select("nav[aria-label=Primary]");
    assertThat(navigation).hasSize(1);
    assertThat(navigation.select("a[href]").eachAttr("href"))
        .containsExactly("/", "/categories", "/bio");
    assertThat(navigation.select("a").eachText()).containsExactly("Home", "Galleries", "Bio");
    assertThat(navigation.select("img")).isEmpty();
    var disabled = navigation.select("span[role=link][aria-disabled=true]");
    assertThat(disabled).hasSize(1);
    assertThat(disabled.text()).isEqualTo("Virtual Exhibitions");
    assertThat(disabled.select("[href], [tabindex], [onclick], [aria-current]")).isEmpty();
    var current = document.select("[aria-current=page]");
    if (currentHref == null) {
      assertThat(current).isEmpty();
    } else {
      assertThat(current).hasSize(1);
      assertThat(current.attr("href")).isEqualTo(currentHref);
    }
    assertThat(document.select("nav[aria-label=Account] a[href=/login]")).hasSize(1);
    assertThat(document.select("nav[aria-label=Account] a[href=/admin]")).isEmpty();
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
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Virtual Exhibitions")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("/virtualExhibitions"))));
  }

  @Test
  void anonymousVisitorCanRetrieveExternallyStoredArtworkWithGetAndHead() throws Exception {
    byte[] expected =
        Files.readAllBytes(
            Path.of(
                "artwork-media/artworks/images/3F1ED738-72F7-4AFA-B6B2-969A545AC8DD/original.jpeg"));

    mockMvc
        .perform(get(LANDSCAPE_ORIGINAL_IMAGE_PATH).with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/jpeg"))
        .andExpect(content().bytes(expected));

    mockMvc
        .perform(head(LANDSCAPE_ORIGINAL_IMAGE_PATH).with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(content().contentType("image/jpeg"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                .longValue("Content-Length", expected.length));
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
                        "A serene photograph capturing the first light of dawn over a snow-covered mountain range.")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(
                            "Select an artwork to view its details."))))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "/categories/" + categoryId + "/artworks/" + artworkId)))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("Artwork by Maya Vasquez")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString("href=\"/artworks/" + artworkId + "\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("View artwork details")))
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
                        "A watercolor painting depicting an ethereal garden bathed in golden light, with flowers and trees shimmering as if glowing from within.")))
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
        .perform(get("/media/artworks/images/missing.jpeg").with(anonymous()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/media/artworks/../application.properties").with(anonymous()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void allCuratedArtworksArePublic() {
    assertThat(artworkRepository.findAll())
        .filteredOn(artwork -> artwork.getCatalogKey() != null)
        .hasSize(14)
        .allMatch(artwork -> artwork.isGeneralViewable());
  }

  @Test
  @Transactional
  void explicitlyHiddenArtworkIsAbsentFromPublicBrowsingDetailAndMedia() throws Exception {
    long originalCategoryCount =
        categoryRepository.listCategorySummaries().stream()
            .filter(category -> category.id() == categoryId)
            .findFirst()
            .orElseThrow()
            .artEntityCount();
    var artwork = artworkRepository.findById(artworkId).orElseThrow();
    artwork.setGeneralViewable(false);
    artworkRepository.saveAndFlush(artwork);

    mockMvc
        .perform(get("/api/categories").with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$[?(@.id == " + categoryId + ")].artEntityCount")
                .value((int) originalCategoryCount - 1));
    mockMvc
        .perform(get("/api/categories/{id}", categoryId).with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.artworks[?(@.title == 'Sunrise over the Mountain')]").doesNotExist());
    mockMvc
        .perform(get("/categories/{id}", categoryId).with(anonymous()))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Sunrise over the Mountain"))));
    mockMvc
        .perform(
            get("/categories/{id}/artworks/{artworkId}", categoryId, artworkId).with(anonymous()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/artworks/{id}", artworkId).with(anonymous()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/artworks/{id}", artworkId).with(anonymous()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get(LANDSCAPE_ORIGINAL_IMAGE_PATH).with(anonymous()))
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
        .contains("Page not found", "Home")
        .doesNotContain("GalleryResourceNotFoundException", "Category 9223372036854775807");

    HttpResponse<String> unauthorized = httpResponse("/actuator/health");
    assertThat(unauthorized.statusCode()).isEqualTo(401);
    assertThat(unauthorized.headers().firstValue("WWW-Authenticate"))
        .hasValue("Basic realm=\"Realm\"");
    assertThat(unauthorized.body())
        .contains("Authentication required", "Home")
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
