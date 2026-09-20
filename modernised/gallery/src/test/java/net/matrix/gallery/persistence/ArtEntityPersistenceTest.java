package net.matrix.gallery.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import net.matrix.gallery.domain.model.ArtEntity;
import net.matrix.gallery.domain.model.Category;
import net.matrix.gallery.domain.model.Comment;
import net.matrix.gallery.domain.value.ImageRendition;
import net.matrix.gallery.domain.value.RenditionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Verifies JPA mappings for {@link ArtEntity} aggregate.
 *
 * @author Anand Hemadri
 */
@DataJpaTest
class ArtEntityPersistenceTest {

  @Autowired private EntityManager entityManager;

  @Test
  void persistsAggregateWithCategoriesCommentsAndRenditions() {
    ArtEntity artEntity = baseArtEntity();

    Category category = new Category();
    category.setCategoryName("Landscapes");
    entityManager.persist(category); // explicitly persist shared aggregate
    artEntity.addCategory(category);

    Comment comment = new Comment();
    comment.setComment("Beautiful piece");
    comment.setFirstName("Ada");
    comment.setLastName("Lovelace");
    comment.setEmailAddress("ada@example.com");
    artEntity.addComment(comment);

    artEntity.addImageRendition(
        RenditionType.THUMBNAIL, new ImageRendition("thumb.key", "image/jpeg", 128L, 48, 48, null));
    artEntity.addImageRendition(
        RenditionType.ORIGINAL,
        new ImageRendition("orig.key", "image/jpeg", 2048L, 1024, 768, null));

    entityManager.persist(artEntity);
    entityManager.flush();
    entityManager.clear();

    ArtEntity persisted = entityManager.find(ArtEntity.class, artEntity.getId());
    assertThat(persisted).isNotNull();
    assertThat(persisted.getCategories()).hasSize(1);
    assertThat(countCategoryLinks(artEntity.getId())).isEqualTo(1);
    assertThat(persisted.getComments()).hasSize(1);
    assertThat(persisted.getThumbnailPicture()).isNotNull();
    assertThat(persisted.getStoragePicture()).isNotNull();
  }

  @Test
  void removingCommentDeletesChildDueToOrphanRemoval() {
    ArtEntity artEntity = baseArtEntity();
    Comment comment = new Comment();
    comment.setComment("Temporary comment");
    comment.setFirstName("Ada");
    comment.setLastName("Tester");
    comment.setEmailAddress("ada@example.com");
    artEntity.addComment(comment);

    entityManager.persist(artEntity);
    entityManager.flush();

    Long commentId = comment.getId();
    artEntity.removeComment(comment);
    entityManager.flush();
    entityManager.clear();

    assertThat(entityManager.find(Comment.class, commentId)).isNull();
  }

  @Test
  void removingCategoryDoesNotDeleteCategory() {
    ArtEntity artEntity = baseArtEntity();
    Category category = new Category();
    category.setCategoryName("Still Life");
    entityManager.persist(category);
    artEntity.addCategory(category);

    entityManager.persist(artEntity);
    entityManager.flush();

    Long categoryId = category.getId();
    artEntity.removeCategory(category);
    entityManager.flush();
    entityManager.clear();

    assertThat(entityManager.find(Category.class, categoryId)).isNotNull();
    assertThat(countCategoryLinks(artEntity.getId())).isZero();
  }

  private ArtEntity baseArtEntity() {
    ArtEntity artEntity = new ArtEntity();
    artEntity.setTitle("Evening Sky");
    artEntity.setSubTitle("Sunset series");
    artEntity.setWidth(300);
    artEntity.setHeight(200);
    artEntity.setMedia("Oil on canvas");
    artEntity.setDescription("Warm toned sunset.");
    artEntity.setCaption("Serenity at dusk");
    return artEntity;
  }

  private long countCategoryLinks(Long artId) {
    Number count =
        (Number)
            entityManager
                .createNativeQuery(
                    "select count(*) from art_entity_category where art_entity_id = :artId")
                .setParameter("artId", artId)
                .getSingleResult();
    return count.longValue();
  }
}
