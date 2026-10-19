package net.matrix.gallery.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.List;
import net.matrix.gallery.domain.value.ArtworkDetail;
import net.matrix.gallery.domain.value.CategoryReference;
import net.matrix.gallery.domain.value.RenditionDetail;
import net.matrix.gallery.domain.value.RenditionType;
import net.matrix.gallery.service.ArtworkService;
import net.matrix.gallery.service.GalleryResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

class ArtworkControllerTest {

  @Mock private ArtworkService artworkService;

  private MockMvc mockMvc;

  @BeforeEach
  void configureMockMvc() {
    MockitoAnnotations.openMocks(this);
    var viewResolver = new InternalResourceViewResolver("/WEB-INF/templates/", ".html");
    mockMvc =
        MockMvcBuilders.standaloneSetup(new ArtworkController(artworkService))
            .setViewResolvers(viewResolver)
            .build();
  }

  @Test
  void rendersArtworkDetailViewAndModel() throws Exception {
    ArtworkDetail artwork = artworkDetail();
    when(artworkService.getArtwork(9L)).thenReturn(artwork);

    mockMvc
        .perform(get("/artworks/9"))
        .andExpect(status().isOk())
        .andExpect(view().name("artwork-detail"))
        .andExpect(model().attribute("artwork", artwork))
        .andExpect(model().attribute("displayImageUrl", "/gallery/evening-sky.jpg"));
  }

  @Test
  void selectsOriginalForHtmlDisplayWithoutChangingRestPrimaryImage() throws Exception {
    ArtworkDetail artwork =
        artworkDetail(
            List.of(
                rendition(RenditionType.GALLERY, "/gallery/evening-sky.jpg"),
                rendition(RenditionType.ORIGINAL, "/original/evening-sky.jpg"),
                rendition(RenditionType.THUMBNAIL, "/thumbnail/evening-sky.jpg")));
    when(artworkService.getArtwork(9L)).thenReturn(artwork);

    mockMvc
        .perform(get("/artworks/9"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("displayImageUrl", "/original/evening-sky.jpg"))
        .andExpect(model().attribute("artwork", artwork));
  }

  @Test
  void fallsBackToGalleryThenThumbnailForHtmlDisplay() throws Exception {
    ArtworkDetail galleryArtwork =
        artworkDetail(
            List.of(
                rendition(RenditionType.GALLERY, "/gallery/evening-sky.jpg"),
                rendition(RenditionType.THUMBNAIL, "/thumbnail/evening-sky.jpg")));
    ArtworkDetail thumbnailArtwork =
        artworkDetail(List.of(rendition(RenditionType.THUMBNAIL, "/thumbnail/evening-sky.jpg")));
    when(artworkService.getArtwork(9L)).thenReturn(galleryArtwork);
    when(artworkService.getArtwork(10L)).thenReturn(thumbnailArtwork);

    mockMvc
        .perform(get("/artworks/9"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("displayImageUrl", "/gallery/evening-sky.jpg"));
    mockMvc
        .perform(get("/artworks/10"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("displayImageUrl", "/thumbnail/evening-sky.jpg"));
  }

  @Test
  void returnsNotFoundForMissingArtwork() throws Exception {
    when(artworkService.getArtwork(404L))
        .thenThrow(new GalleryResourceNotFoundException("Artwork 404 was not found"));

    mockMvc.perform(get("/artworks/404")).andExpect(status().isNotFound());
  }

  static ArtworkDetail artworkDetail() {
    return artworkDetail(
        List.of(
            new RenditionDetail(
                RenditionType.GALLERY,
                "gallery/evening-sky.jpg",
                "/gallery/evening-sky.jpg",
                "image/jpeg",
                245760L,
                1200,
                800,
                "checksum")));
  }

  static ArtworkDetail artworkDetail(List<RenditionDetail> renditions) {
    return new ArtworkDetail(
        9L,
        "Evening Sky",
        "Sunset series",
        "Legacy Artist",
        "Landscape",
        null,
        LocalDate.of(2026, 1, 15),
        1200,
        800,
        "Oil on canvas",
        "A warm-toned landscape at dusk.",
        "Serenity at dusk",
        true,
        false,
        "/artworks/images/evening-sky.jpg",
        List.of(new CategoryReference(5L, "Landscapes")),
        renditions);
  }

  private static RenditionDetail rendition(RenditionType type, String url) {
    return new RenditionDetail(
        type,
        type.name().toLowerCase() + "/evening-sky.jpg",
        url,
        "image/jpeg",
        245760L,
        1200,
        800,
        "checksum");
  }
}
