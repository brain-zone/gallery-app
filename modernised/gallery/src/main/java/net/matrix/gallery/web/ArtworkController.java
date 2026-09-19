package net.matrix.gallery.web;

import net.matrix.gallery.service.ArtworkService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Renders public artwork detail pages. */
@Controller
public class ArtworkController {

  private final ArtworkService artworkService;

  public ArtworkController(ArtworkService artworkService) {
    this.artworkService = artworkService;
  }

  @GetMapping("/artworks/{id}")
  public String showArtwork(@PathVariable long id, Model model) {
    model.addAttribute("artwork", artworkService.getArtwork(id));
    return "artwork-detail";
  }
}
