package net.matrix.gallery.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import net.matrix.gallery.domain.model.ArtEntity;
import net.matrix.gallery.domain.model.Category;
import net.matrix.gallery.domain.value.ImageRendition;
import net.matrix.gallery.domain.value.RenditionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(
    properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
class ArtworkRepositoryTest {

  @Autowired private ArtworkRepository artworkRepository;
  @Autowired private EntityManager entityManager;

  @Test
  void findDetailLoadsCategoriesAndRenditions() {
    Category category = new Category();
    category.setCategoryName("Landscapes");
    entityManager.persist(category);

    ArtEntity artwork = new ArtEntity();
    artwork.setTitle("Evening Sky");
    artwork.addCategory(category);
    artwork.addImageRendition(
        RenditionType.GALLERY,
        new ImageRendition("gallery/sky.jpg", "image/jpeg", 100L, 1200, 800, "checksum"));
    entityManager.persist(artwork);
    entityManager.flush();
    Long artworkId = artwork.getId();
    entityManager.clear();

    var detail = artworkRepository.findDetailById(artworkId).orElseThrow();

    assertThat(detail.getCategories())
        .extracting(Category::getCategoryName)
        .containsExactly("Landscapes");
    assertThat(detail.getImageRendition()).containsKey(RenditionType.GALLERY);
  }
}
