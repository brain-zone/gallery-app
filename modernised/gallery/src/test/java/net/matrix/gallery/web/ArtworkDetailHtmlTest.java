package net.matrix.gallery.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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
        .contains("class=\"gallery-image-link\" href=\"/images/work.jpg\"")
        .contains("class=\"gallery-primary-image\" src=\"/images/work.jpg\"")
        .contains("href=\"/images/work.jpg\"")
        .contains("href=\"/images/gallery/work.jpg\"")
        .contains("href=\"/images/thumbnail/work.jpg\"")
        .contains(">ORIGINAL</span>", ">GALLERY</span>", ">THUMBNAIL</span>")
        .contains(">1024 × 768</span>", ">600 × 450</span>", ">96 × 72</span>")
        .contains("Evening Sky", "Legacy Artist", "A warm-toned landscape at dusk.")
        .contains("Landscape", "1200 × 800", "href=\"/categories/5\"");
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
        .contains(">GALLERY</span>")
        .doesNotContain("artwork-rendition__dimensions");
    assertThat(missingHtml)
        .contains("Image unavailable.")
        .doesNotContain("gallery-image-link", "artwork-rendition");
  }

  private RenditionDetail rendition(RenditionType type, String url, Integer width, Integer height) {
    return new RenditionDetail(
        type,
        type.name().toLowerCase() + "/work.jpg",
        url,
        "image/jpeg",
        100L,
        width,
        height,
        "checksum");
  }
}
