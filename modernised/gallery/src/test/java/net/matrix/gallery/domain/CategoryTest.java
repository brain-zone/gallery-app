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
  void addArtAddsToCollectionAndCollectionIsUnmodifiable() {
    Category category = new Category();
    category.setCategoryName("Portraits");

    ArtEntity art = new ArtEntity();
    category.addArtToCategory(art);

    assertEquals(1, category.getArtEntities().size());
    assertSame(art, category.getArtEntities().iterator().next());
    assertThrows(
        UnsupportedOperationException.class, () -> category.getArtEntities().add(new ArtEntity()));
    assertThrows(UnsupportedOperationException.class, () -> category.getArtEntities().remove(art));
  }

  @Test
  void equalityHandlesNullsAndSelf() {
    Category category = new Category();
    category.setCategoryName(null);

    // self
    assertEquals(category, category);
    // null
    assertNotEquals(null, category);

    Category other = new Category();
    other.setId(1L);
    other.setCategoryName(null);
    assertNotEquals(category, other);
  }

  @Test
  void equalityWhenIdPresent() {
    Category one = new Category();
    Category two = new Category();
    Category three = new Category();

    one.setCategoryName("Abstract");
    two.setCategoryName("Abstract");
    three.setCategoryName("Abstract");

    one.setId(1L);
    two.setId(1L);
    three.setId(2L);

    assertEquals(one, two);
    assertNotEquals(one, three);
    // id vs null id should not be equal
    Category nullId = new Category();
    nullId.setCategoryName("Abstract");
    assertNotEquals(one, nullId);
  }

  @Test
  void equalityWhenIdSameIgnoresNameDifferences() {
    Category one = new Category();
    Category two = new Category();

    one.setCategoryName("Landscape");
    two.setCategoryName("Portrait");

    one.setId(99L);
    two.setId(99L);

    assertEquals(one, two);
  }

  @Test
  void equalityWhenNameIsNullAndIdMatches() {
    Category one = new Category();
    Category two = new Category();

    one.setId(5L);
    two.setId(5L);
    one.setCategoryName(null);
    two.setCategoryName("Something");

    assertEquals(one, two);
  }

  @Test
  void hashCodeHandlesNullNameAndNonNullId() {
    Category category = new Category();
    category.setId(7L);
    category.setCategoryName(null);

    // should not throw and should be deterministic
    int hash1 = category.hashCode();
    int hash2 = category.hashCode();

    assertEquals(hash1, hash2);
  }
}
