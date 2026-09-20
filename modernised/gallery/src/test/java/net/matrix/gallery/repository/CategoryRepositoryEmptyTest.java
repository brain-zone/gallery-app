package net.matrix.gallery.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class CategoryRepositoryEmptyTest {
  @Autowired CategoryRepository categoryRepository;

  @Test
  void listCategorySummariesIsEmptyWhenNoCategoriesExist() {
    assertTrue(categoryRepository.listCategorySummaries().isEmpty());
  }

  @Test
  void existsAndFindReturnEmptyWhenNoCategoriesExist() {
    assertFalse(categoryRepository.existsByCategoryNameIgnoreCase("Landscape"));
    assertTrue(categoryRepository.findByCategoryNameIgnoreCase("Landscape").isEmpty());
  }
}
