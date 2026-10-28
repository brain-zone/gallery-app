package net.matrix.gallery.web;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import net.matrix.gallery.repository.CategoryRepository;
import net.matrix.gallery.service.ArtworkUploadException;
import net.matrix.gallery.service.ArtworkUploadService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Renders the authenticated administration landing page. */
@Controller
public class AdminController {

  private final CategoryRepository categoryRepository;
  private final ArtworkUploadService artworkUploadService;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring-managed collaborators are intentionally retained by this controller")
  public AdminController(
      CategoryRepository categoryRepository, ArtworkUploadService artworkUploadService) {
    this.categoryRepository = categoryRepository;
    this.artworkUploadService = artworkUploadService;
  }

  @GetMapping("/admin")
  public String admin() {
    return "admin";
  }

  @GetMapping("/admin/artworks/upload")
  public String uploadForm(Model model) {
    if (!model.containsAttribute("artworkUploadForm")) {
      model.addAttribute("artworkUploadForm", new ArtworkUploadForm());
    }
    addCategories(model);
    return "admin-artwork-upload";
  }

  @PostMapping("/admin/artworks/upload")
  public String upload(
      @Valid @ModelAttribute ArtworkUploadForm artworkUploadForm,
      BindingResult bindingResult,
      Model model,
      RedirectAttributes redirectAttributes) {
    if (bindingResult.hasErrors()) {
      addCategories(model);
      return "admin-artwork-upload";
    }
    try {
      Long artworkId = artworkUploadService.upload(artworkUploadForm);
      redirectAttributes.addFlashAttribute("uploadSuccess", "Artwork uploaded and published.");
      return "redirect:/artworks/" + artworkId;
    } catch (ArtworkUploadException exception) {
      bindingResult.reject("upload.failed", exception.getMessage());
      addCategories(model);
      return "admin-artwork-upload";
    }
  }

  private void addCategories(Model model) {
    model.addAttribute(
        "categories", categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "categoryName")));
  }
}
