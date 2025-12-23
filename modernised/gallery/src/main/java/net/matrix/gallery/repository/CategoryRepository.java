package net.matrix.gallery.repository;

import java.util.List;
import java.util.Optional;
import net.matrix.gallery.domain.model.Category;
import net.matrix.gallery.domain.value.CategorySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {

  Optional<Category> findByCategoryNameIgnoreCase(String categoryName);

  boolean existsByCategoryNameIgnoreCase(String categoryName);

  @Query(
      """
        SELECT new net.matrix.gallery.domain.value.CategorySummary(c.id, c.categoryName, count(a.id)) FROM Category c
        LEFT JOIN  c.artEntities a
        GROUP BY c.id, c.categoryName  ORDER BY c.categoryName ASC
        """)
  public List<CategorySummary> listCategorySummaries();
}
