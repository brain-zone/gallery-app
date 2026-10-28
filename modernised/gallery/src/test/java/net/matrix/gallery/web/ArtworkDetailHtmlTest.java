package net.matrix.gallery.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import net.matrix.gallery.domain.value.ArtworkDetail;
import net.matrix.gallery.domain.value.CategoryReference;
import net.matrix.gallery.domain.value.RenditionDetail;
import net.matrix.gallery.domain.value.RenditionType;
import net.matrix.gallery.service.ArtworkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArtworkController.class)
@AutoConfigureMockMvc(addFilters = false)
class ArtworkDetailHtmlTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ArtworkService artworkService;

  @Test
  void rendersOriginalPrimaryImageAndLabeledRenditionsWithMetadata() throws Exception {
    var original = rendition(RenditionType.ORIGINAL, "/images/work.jpg", 1024, 768);
    var gallery = rendition(RenditionType.GALLERY, "/images/gallery/work.jpg", 600, 450);
    var thumbnail = rendition(RenditionType.THUMBNAIL, "/images/thumbnail/work.jpg", 96, 72);
    when(artworkService.getArtwork(9L))
        .thenReturn(ArtworkControllerTest.artworkDetail(List.of(original, gallery, thumbnail)));

    String html =
        mockMvc
            .perform(get("/artworks/9"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String normalizedHtml = html.replaceAll("\\s+", " ");

    assertThat(normalizedHtml)
        .contains("src=\"/js/artwork-detail-lightbox.js\"")
        .contains(
            "class=\"gallery-image-link\" href=\"/images/work.jpg\" data-artwork-detail-lightbox-trigger aria-label=\"Open original image of Evening Sky\" data-artwork-title=\"Evening Sky\"")
        .contains(
            "class=\"gallery-primary-image\" src=\"/images/work.jpg\" alt=\"Evening Sky ORIGINAL image\" data-artwork-detail-canvas-image")
        .contains("href=\"/images/work.jpg\"")
        .contains("href=\"/images/gallery/work.jpg\"")
        .contains("href=\"/images/thumbnail/work.jpg\"")
        .contains("data-rendition-label=\"ORIGINAL\"")
        .contains("data-rendition-label=\"GALLERY\"")
        .contains("data-rendition-label=\"THUMBNAIL\"")
        .contains("data-artwork-detail-rendition-selector aria-current=\"true\"")
        .contains("data-rendition-width=\"1024\" data-rendition-height=\"768\"")
        .contains(">ORIGINAL</span>", ">GALLERY</span>", ">THUMBNAIL</span>")
        .contains(">1024 × 768</span>", ">600 × 450</span>", ">96 × 72</span>")
        .contains("<dialog class=\"artwork-detail-lightbox\"")
        .contains(
            "<button class=\"artwork-lightbox__close\" type=\"submit\" autofocus>Close</button>")
        .contains("class=\"artwork-detail-lightbox__image\" src=\"/images/work.jpg\"")
        .doesNotContain("data-artwork-lightbox-previous", "data-artwork-lightbox-next")
        .contains("Evening Sky", "Legacy Artist", "A warm-toned landscape at dusk.")
        .contains("Landscape", "1200 × 800", "href=\"/categories/5/artworks/9\"");
  }

  @Test
  void categoryLinksRetainArtworkContextForEveryAssociation() throws Exception {
    when(artworkService.getArtwork(11L))
        .thenReturn(
            artworkDetail(
                11L,
                List.of(
                    new CategoryReference(5L, "Urban"),
                    new CategoryReference(8L, "Contemporary"))));
    when(artworkService.getArtwork(12L)).thenReturn(artworkDetail(12L, List.of()));

    String associatedHtml =
        mockMvc.perform(get("/artworks/11")).andReturn().getResponse().getContentAsString();
    String unassociatedHtml =
        mockMvc.perform(get("/artworks/12")).andReturn().getResponse().getContentAsString();

    assertThat(associatedHtml)
        .contains("href=\"/categories/5/artworks/11\"", ">Urban</a>")
        .contains("href=\"/categories/8/artworks/11\"", ">Contemporary</a>")
        .contains("href=\"/categories\">Galleries</a>")
        .contains("href=\"/categories/5\">Urban</a>")
        .doesNotContain("href=\"/categories/8\"");
    assertThat(unassociatedHtml)
        .contains("href=\"/categories\">Galleries</a>", ">Shattered Reflection</span>")
        .doesNotContain("class=\"gallery-category-links\"");
  }

  @Test
  void omitsDimensionsAndBrokenMarkupWhenRenditionMetadataOrRenditionsAreMissing()
      throws Exception {
    var gallery = rendition(RenditionType.GALLERY, "/images/gallery/work.png", null, null);
    when(artworkService.getArtwork(9L))
        .thenReturn(ArtworkControllerTest.artworkDetail(List.of(gallery)));
    when(artworkService.getArtwork(10L)).thenReturn(ArtworkControllerTest.artworkDetail(List.of()));

    String galleryHtml =
        mockMvc.perform(get("/artworks/9")).andReturn().getResponse().getContentAsString();
    String missingHtml =
        mockMvc.perform(get("/artworks/10")).andReturn().getResponse().getContentAsString();

    assertThat(galleryHtml)
        .contains("href=\"/images/gallery/work.png\"")
        .contains("class=\"artwork-detail-lightbox__image\"")
        .contains("src=\"/images/gallery/work.png\"")
        .contains(">GALLERY</span>")
        .doesNotContain("artwork-rendition__dimensions");
    assertThat(missingHtml)
        .contains("Image unavailable.")
        .doesNotContain(
            "gallery-image-link", "artwork-rendition", "<dialog class=\"artwork-detail-lightbox\"");
  }

  private RenditionDetail rendition(RenditionType type, String url, Integer width, Integer height) {
    return new RenditionDetail(
        type,
        type.name().toLowerCase(Locale.ROOT) + "/work.jpg",
        url,
        "image/jpeg",
        100L,
        width,
        height,
        "checksum");
  }

  private ArtworkDetail artworkDetail(long id, List<CategoryReference> categories) {
    return new ArtworkDetail(
        id,
        "Shattered Reflection",
        null,
        "Legacy Artist",
        "Urban",
        null,
        LocalDate.of(2026, 1, 15),
        1200,
        800,
        "Oil on canvas",
        "An urban reflection.",
        null,
        true,
        false,
        "/images/work.jpg",
        categories,
        List.of(rendition(RenditionType.ORIGINAL, "/images/work.jpg", 1200, 800)));
  }
}
