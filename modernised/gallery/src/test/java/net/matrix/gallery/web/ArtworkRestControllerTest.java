package net.matrix.gallery.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.matrix.gallery.service.ArtworkService;
import net.matrix.gallery.service.GalleryResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ArtworkRestControllerTest {

  @Mock private ArtworkService artworkService;

  private MockMvc mockMvc;

  @BeforeEach
  void configureMockMvc() {
    MockitoAnnotations.openMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(new ArtworkRestController(artworkService)).build();
  }

  @Test
  void returnsArtworkDetailAsJson() throws Exception {
    when(artworkService.getArtwork(9L)).thenReturn(ArtworkControllerTest.artworkDetail());

    mockMvc
        .perform(get("/api/artworks/9"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(9))
        .andExpect(jsonPath("$.title").value("Evening Sky"))
        .andExpect(jsonPath("$.categories[0].categoryName").value("Landscapes"))
        .andExpect(jsonPath("$.renditions[0].type").value("GALLERY"))
        .andExpect(jsonPath("$.renditions[0].objectKey").value("gallery/evening-sky.jpg"));
  }

  @Test
  void returnsNotFoundForMissingArtwork() throws Exception {
    when(artworkService.getArtwork(404L))
        .thenThrow(new GalleryResourceNotFoundException("Artwork 404 was not found"));

    mockMvc.perform(get("/api/artworks/404")).andExpect(status().isNotFound());
  }
}
