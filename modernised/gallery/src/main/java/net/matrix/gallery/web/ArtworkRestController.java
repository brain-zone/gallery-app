package net.matrix.gallery.web;

import net.matrix.gallery.domain.value.ArtworkDetail;
import net.matrix.gallery.service.ArtworkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes artwork browsing data as JSON. */
@RestController
@RequestMapping("/api/artworks")
public class ArtworkRestController {

  private final ArtworkService artworkService;

  public ArtworkRestController(ArtworkService artworkService) {
    this.artworkService = artworkService;
  }

  @GetMapping("/{id}")
  public ArtworkDetail showArtwork(@PathVariable long id) {
    return artworkService.getArtwork(id);
  }
}
