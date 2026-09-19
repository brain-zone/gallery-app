package net.matrix.gallery.service;

import java.util.List;
import net.matrix.gallery.domain.value.CategorySummary;
import net.matrix.gallery.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Provides read-only category browsing operations. */
@Service
@Transactional(readOnly = true)
public class CategoryService {

  private final CategoryRepository categoryRepository;

  public CategoryService(CategoryRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
  }

  public List<CategorySummary> listCategories() {
    return categoryRepository.listCategorySummaries();
  }
}
