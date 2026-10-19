package net.matrix.gallery.web;

import java.util.List;
import net.matrix.gallery.domain.value.RenditionDetail;
import net.matrix.gallery.domain.value.RenditionType;
import net.matrix.gallery.service.ArtworkService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** Renders public artwork detail pages. */
@Controller
public class ArtworkController {

  private static final List<RenditionType> DISPLAY_PRIORITY =
      List.of(RenditionType.ORIGINAL, RenditionType.GALLERY, RenditionType.THUMBNAIL);

  private final ArtworkService artworkService;

  public ArtworkController(ArtworkService artworkService) {
    this.artworkService = artworkService;
  }

  @GetMapping("/artworks/{id}")
  public String showArtwork(@PathVariable long id, Model model) {
    var artwork = artworkService.getArtwork(id);
    model.addAttribute("artwork", artwork);
    model.addAttribute("displayImageUrl", displayImageUrl(artwork.renditions()));
    return "artwork-detail";
  }

  private String displayImageUrl(List<RenditionDetail> renditions) {
    for (RenditionType type : DISPLAY_PRIORITY) {
      var matchingRendition =
          renditions.stream().filter(rendition -> rendition.type() == type).findFirst();
      if (matchingRendition.isPresent()) {
        return matchingRendition.get().url();
      }
    }
    return null;
  }
}
