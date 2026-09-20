package net.matrix.gallery.web;

import net.matrix.gallery.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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

  @GetMapping("/categories/{id}")
  public String showCategory(@PathVariable long id, Model model) {
    model.addAttribute("category", categoryService.getCategory(id));
    return "category-detail";
  }
}
