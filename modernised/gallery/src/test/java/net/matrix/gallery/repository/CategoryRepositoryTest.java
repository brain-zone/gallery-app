package net.matrix.gallery.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import net.matrix.gallery.domain.model.ArtEntity;
import net.matrix.gallery.domain.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
public class CategoryRepositoryTest {
  @Autowired CategoryRepository categoryRepository;
  @Autowired EntityManager entityManager;
  private Category abstractCategory;
  private Category landscapeCategory;
  private Category urbanCategory;
  private Category portraitCategory;
  private Category sculptureCategory;

  @BeforeEach
  void setup() {
    abstractCategory = new Category();
    abstractCategory.setCategoryName("Abstract");
    entityManager.persist(abstractCategory);

    landscapeCategory = new Category();
    landscapeCategory.setCategoryName("Landscape");
    entityManager.persist(landscapeCategory);

    urbanCategory = new Category();
    urbanCategory.setCategoryName("Urban");
    entityManager.persist(urbanCategory);

    portraitCategory = new Category();
    portraitCategory.setCategoryName("Portrait");
    entityManager.persist(portraitCategory);

    sculptureCategory = new Category();
    sculptureCategory.setCategoryName("Sculpture");
    entityManager.persist(sculptureCategory);

    ArtEntity art1 = new ArtEntity();
    art1.setTitle("Sunset");
    art1.setDescription("A beautiful sunset.");
    art1.addCategory(landscapeCategory);
    entityManager.persist(art1);

    ArtEntity art2 = new ArtEntity();
    art2.setTitle("Mountain");
    art2.setDescription("A majestic mountain.");
    art2.addCategory(landscapeCategory);
    entityManager.persist(art2);

    ArtEntity art3 = new ArtEntity();
    art3.setTitle("Cityscape");
    art3.setDescription("A bustling cityscape.");
    art3.addCategory(urbanCategory);
    entityManager.persist(art3);

    ArtEntity art4 = new ArtEntity();
    art4.setTitle("Abstract Thoughts");
    art4.setDescription("An abstract piece.");
    art4.addCategory(abstractCategory);
    entityManager.persist(art4);

    entityManager.flush();
  }

  @Test
  void testExistsByCategoryNameIgnoreCase() {
    assertTrue(categoryRepository.existsByCategoryNameIgnoreCase("Abstract"));
    assertTrue(categoryRepository.existsByCategoryNameIgnoreCase("Portrait"));
    assertTrue(categoryRepository.existsByCategoryNameIgnoreCase("Landscape"));
    assertTrue(categoryRepository.existsByCategoryNameIgnoreCase("Sculpture"));
    assertTrue(categoryRepository.existsByCategoryNameIgnoreCase("Urban"));
    assertFalse(categoryRepository.existsByCategoryNameIgnoreCase("Nonexistent"));
    assertFalse(categoryRepository.existsByCategoryNameIgnoreCase(""));
    assertFalse(categoryRepository.existsByCategoryNameIgnoreCase(null));
  }

  @Test
  void testFindByCategoryNameIgnoreCase() {
    Optional<Category> category = categoryRepository.findByCategoryNameIgnoreCase("urban");

    assertTrue(category.isPresent());
    assertEquals("Urban", category.get().getCategoryName());

    category = categoryRepository.findByCategoryNameIgnoreCase("aBsTrAcT");
    assertTrue(category.isPresent());
    assertEquals("Abstract", category.get().getCategoryName());

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

    assertEquals(1, summaryByName.get("Abstract").artEntityCount());
    assertEquals(2, summaryByName.get("Landscape").artEntityCount());
    assertEquals(1, summaryByName.get("Urban").artEntityCount());
    assertEquals(0, summaryByName.get("Portrait").artEntityCount());
    assertEquals(0, summaryByName.get("Sculpture").artEntityCount());
  }

  @Test
  void testListCategorySummariesCountsMultipleCategoriesPerArtwork() {
    ArtEntity mixed = new ArtEntity();
    mixed.setTitle("Crossroads");
    mixed.setDescription("Blends urban and landscape elements.");
    mixed.addCategory(landscapeCategory);
    mixed.addCategory(urbanCategory);
    entityManager.persist(mixed);
    entityManager.flush();

    var summaryByName =
        categoryRepository.listCategorySummaries().stream()
            .collect(java.util.stream.Collectors.toMap(s -> s.categoryName(), s -> s));

    assertEquals(3, summaryByName.get("Landscape").artEntityCount());
    assertEquals(2, summaryByName.get("Urban").artEntityCount());
  }

  @Test
  void testListCategorySummariesCountsSharedCategoriesAcrossArtworks() {
    ArtEntity first = new ArtEntity();
    first.setTitle("Duality");
    first.setDescription("Abstract landscape.");
    first.addCategory(abstractCategory);
    first.addCategory(landscapeCategory);
    entityManager.persist(first);

    ArtEntity second = new ArtEntity();
    second.setTitle("Two Worlds");
    second.setDescription("Another abstract landscape.");
    second.addCategory(abstractCategory);
    second.addCategory(landscapeCategory);
    entityManager.persist(second);

    entityManager.flush();

    var summaryByName =
        categoryRepository.listCategorySummaries().stream()
            .collect(java.util.stream.Collectors.toMap(s -> s.categoryName(), s -> s));

    assertEquals(3, summaryByName.get("Abstract").artEntityCount());
    assertEquals(4, summaryByName.get("Landscape").artEntityCount());
  }
}
