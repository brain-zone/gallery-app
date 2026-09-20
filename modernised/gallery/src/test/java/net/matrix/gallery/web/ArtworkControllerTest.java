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
        .andExpect(model().attribute("artwork", artwork));
  }

  @Test
  void returnsNotFoundForMissingArtwork() throws Exception {
    when(artworkService.getArtwork(404L))
        .thenThrow(new GalleryResourceNotFoundException("Artwork 404 was not found"));

    mockMvc.perform(get("/artworks/404")).andExpect(status().isNotFound());
  }

  static ArtworkDetail artworkDetail() {
    return new ArtworkDetail(
        9L,
        "Evening Sky",
        "Sunset series",
        null,
        LocalDate.of(2026, 1, 15),
        1200,
        800,
        "Oil on canvas",
        "A warm-toned landscape at dusk.",
        "Serenity at dusk",
        true,
        false,
        List.of(new CategoryReference(5L, "Landscapes")),
        List.of(
            new RenditionDetail(
                RenditionType.GALLERY,
                "gallery/evening-sky.jpg",
                "image/jpeg",
                245760L,
                1200,
                800,
                "checksum")));
  }
}
