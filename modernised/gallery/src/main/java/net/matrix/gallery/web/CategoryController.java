package net.matrix.gallery.web;

import net.matrix.gallery.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Renders the public category browsing pages. */
@Controller
public class CategoryController {

  private final CategoryService categoryService;

  public CategoryController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @GetMapping("/categories")
  public String listCategories(Model model) {
    model.addAttribute("categories", categoryService.listCategories());
    return "categories";
  }
}
