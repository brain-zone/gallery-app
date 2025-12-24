package net.matrix.gallery.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.function.Function;
import net.matrix.gallery.domain.model.ArtEntity;
import net.matrix.gallery.domain.model.Category;
import net.matrix.gallery.domain.value.CategorySummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
public class CategoryRepositoryTest {
  @Autowired CategoryRepository categoryRepository;
  @Autowired EntityManager entityManager;
  private Category cat1;
  private Category cat2;
  private Category cat3;
  private Category cat4;
  private Category cat5;

  @BeforeEach
  void setup() {
    cat1 = new Category();
    cat1.setCategoryName("Category1");
    entityManager.persist(cat1);

    cat2 = new Category();
    cat2.setCategoryName("Category2");
    entityManager.persist(cat2);

    cat3 = new Category();
    cat3.setCategoryName("Category3");
    entityManager.persist(cat3);

    cat4 = new Category();
    cat4.setCategoryName("Category4");
    entityManager.persist(cat4);

    cat5 = new Category();
    cat5.setCategoryName("Category5");
    entityManager.persist(cat5);

    ArtEntity art1 = new ArtEntity();
    art1.setTitle("Sunset");
    art1.setDescription("A beautiful sunset.");
    art1.addCategory(cat2);
    entityManager.persist(art1);

    ArtEntity art2 = new ArtEntity();
    art2.setTitle("Mountain");
    art2.setDescription("A majestic mountain.");
    art2.addCategory(cat2);
    entityManager.persist(art2);

    ArtEntity art3 = new ArtEntity();
    art3.setTitle("Cityscape");
    art3.setDescription("A bustling cityscape.");
    art3.addCategory(cat3);
    entityManager.persist(art3);

    ArtEntity art4 = new ArtEntity();
    art4.setTitle("Abstract Thoughts");
    art4.setDescription("An abstract piece.");
    art4.addCategory(cat1);
    entityManager.persist(art4);

    entityManager.flush();
  }

  @Test
  void testExistsByCategoryNameIgnoreCase() {
    assertTrue(categoryRepository.existsByCategoryNameIgnoreCase("Category1"));
    assertTrue(categoryRepository.existsByCategoryNameIgnoreCase("Category2"));
    assertTrue(categoryRepository.existsByCategoryNameIgnoreCase("Category3"));
    assertTrue(categoryRepository.existsByCategoryNameIgnoreCase("Category4"));
    assertTrue(categoryRepository.existsByCategoryNameIgnoreCase("Category5"));
    assertFalse(categoryRepository.existsByCategoryNameIgnoreCase("Nonexistent"));
    assertFalse(categoryRepository.existsByCategoryNameIgnoreCase(""));
    assertFalse(categoryRepository.existsByCategoryNameIgnoreCase(null));
  }

  @Test
  void testFindByCategoryNameIgnoreCase() {
    Optional<Category> category = categoryRepository.findByCategoryNameIgnoreCase("category3");

    assertTrue(category.isPresent());
    assertEquals("Category3", category.get().getCategoryName());

    category = categoryRepository.findByCategoryNameIgnoreCase("cAtEgOrY1");
    assertTrue(category.isPresent());
    assertEquals("Category1", category.get().getCategoryName());

    category = categoryRepository.findByCategoryNameIgnoreCase("nonexistent");
    assertTrue(category.isEmpty());

    category = categoryRepository.findByCategoryNameIgnoreCase("");
    assertTrue(category.isEmpty());

    category = categoryRepository.findByCategoryNameIgnoreCase(null);
    assertTrue(category.isEmpty());
  }

  @Test
  void testListCategorySummaries() {
    var summaries = categoryRepository.listCategorySummaries();
    assertEquals(5, summaries.stream().map(s -> s.categoryName()).distinct().count());

    var summaryByName =
        summaries.stream()
            .collect(java.util.stream.Collectors.toMap(s -> s.categoryName(), s -> s));

    assertEquals(1, summaryByName.get("Category1").artEntityCount());
    assertEquals(2, summaryByName.get("Category2").artEntityCount());
    assertEquals(1, summaryByName.get("Category3").artEntityCount());
    assertEquals(0, summaryByName.get("Category4").artEntityCount());
    assertEquals(0, summaryByName.get("Category5").artEntityCount());
  }

  @Test
  void testListCategorySummariesCountsMultipleCategoriesPerArtwork() {
    ArtEntity mixed = new ArtEntity();
    mixed.setTitle("Crossroads");
    mixed.setDescription("Blends urban and landscape elements.");
    mixed.addCategory(cat2);
    mixed.addCategory(cat3);
    entityManager.persist(mixed);
    entityManager.flush();

    var summaryByName =
        categoryRepository.listCategorySummaries().stream()
            .collect(java.util.stream.Collectors.toMap(s -> s.categoryName(), s -> s));

    assertEquals(3, summaryByName.get("Category2").artEntityCount());
    assertEquals(2, summaryByName.get("Category3").artEntityCount());
  }

  @Test
  void testListCategorySummariesCountsSharedCategoriesAcrossArtworks() {
    ArtEntity first = new ArtEntity();
    first.setTitle("Duality");
    first.setDescription("Abstract landscape.");
    first.addCategory(cat1);
    first.addCategory(cat2);
    entityManager.persist(first);

    ArtEntity second = new ArtEntity();
    second.setTitle("Two Worlds");
    second.setDescription("Another abstract landscape.");
    second.addCategory(cat1);
    second.addCategory(cat2);
    entityManager.persist(second);

    entityManager.flush();

    var summaryByName =
        categoryRepository.listCategorySummaries().stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    CategorySummary::categoryName, Function.identity()));

    assertEquals(3, summaryByName.get("Category1").artEntityCount());
    assertEquals(4, summaryByName.get("Category2").artEntityCount());
  }
}
