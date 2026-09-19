package net.matrix.gallery.web;

import java.util.List;
import net.matrix.gallery.domain.value.CategoryDetail;
import net.matrix.gallery.domain.value.CategorySummary;
import net.matrix.gallery.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes category browsing data as JSON. */
@RestController
@RequestMapping("/api/categories")
public class CategoryRestController {

  private final CategoryService categoryService;

  public CategoryRestController(CategoryService categoryService) {
    this.categoryService = categoryService;
  }

  @GetMapping
  public List<CategorySummary> listCategories() {
    return categoryService.listCategories();
  }

  @GetMapping("/{id}")
  public CategoryDetail showCategory(@PathVariable long id) {
    return categoryService.getCategory(id);
  }
}
