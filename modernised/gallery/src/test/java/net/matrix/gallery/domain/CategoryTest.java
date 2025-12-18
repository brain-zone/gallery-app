package net.matrix.gallery.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests for Category domain behavior.
 *
 * @author Anand Hemadri
 */
class CategoryTest {

  @Test
  void addAndRemoveArt() {
    Category category = new Category();
    category.setCategoryName("Portraits");

    ArtEntity art = new ArtEntity();
    category.addArtToCategory(art);

    assertEquals(1, category.getArtEntities().size());
    assertSame(art, category.getArtEntities().iterator().next());

    category.removeArtFromCategory(art);
    assertEquals(0, category.getArtEntities().size());
  }

  @Test
  void artEntitiesCollectionIsUnmodifiable() {
    Category category = new Category();
    category.setCategoryName("Portraits");

    ArtEntity art = new ArtEntity();
    category.addArtToCategory(art);

    assertThrows(
        UnsupportedOperationException.class, () -> category.getArtEntities().add(new ArtEntity()));
    assertThrows(UnsupportedOperationException.class, () -> category.getArtEntities().remove(art));
  }

  @Test
  void equalsAndHashCodeContract() {
    Category one = new Category();
    one.setId(1L);
    one.setCategoryName("One");

    Category two = new Category();
    two.setId(2L);
    two.setCategoryName("Two");

    Category oneAgain = new Category();
    oneAgain.setId(1L);
    oneAgain.setCategoryName("One Again");

    // Not equal with different IDs
    assertNotEquals(one, two);

    // Equal with same IDs
    assertEquals(one, oneAgain);

    // Not equal to null
    assertNotEquals(one, null);

    // Not equal to different class
    assertNotEquals(one, new Object());

    // New entities are not equal to each other
    assertNotEquals(new Category(), new Category());

    // New entity not equal to persisted one
    assertNotEquals(new Category(), one);

    // Hashcode contract: equal objects must have equal hashcodes
    assertEquals(one.hashCode(), oneAgain.hashCode());
  }
}
