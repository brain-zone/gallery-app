package net.matrix.gallery.repository;

import java.util.List;
import java.util.Optional;
import net.matrix.gallery.domain.model.Category;
import net.matrix.gallery.domain.value.CategorySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  @Query(
      """
        SELECT DISTINCT c FROM Category c
        LEFT JOIN FETCH c.artEntities artwork
        LEFT JOIN FETCH artwork.imageRendition
        WHERE c.id = :id
        """)
  Optional<Category> findWithArtEntitiesById(@Param("id") Long id);
}
