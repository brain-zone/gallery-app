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
  void addArtAddsToCollectionAndViewIsUnmodifiable() {
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
  void equalityWhenIdNullUsesName() {
    Category one = new Category();
    Category two = new Category();

    one.setCategoryName("Abstract");
    two.setCategoryName("Abstract");

    assertNotEquals(one, two);
    assertEquals(one.hashCode(), two.hashCode());
  }

  @Test
  void equalityHandlesNullsAndSelf() {
    Category category = new Category();
    category.setCategoryName(null);

    // self
    assertEquals(category, category);
    // null
    assertNotEquals(category, null);

    Category other = new Category();
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

    one.id = 1L;
    two.id = 1L;
    three.id = 2L;

    assertEquals(one, two);
    assertNotEquals(one, three);
    // id vs null id should not be equal
    Category nullId = new Category();
    nullId.setCategoryName("Abstract");
    assertNotEquals(one, nullId);
  }

  @Test
  void equalityWhenIdSameButNameDiffers() {
    Category one = new Category();
    Category two = new Category();

    one.setCategoryName("Landscape");
    two.setCategoryName("Portrait");

    one.id = 99L;
    two.id = 99L;

    assertNotEquals(one, two);
  }

  @Test
  void equalityWhenNameIsNullAndIdMatches() {
    Category one = new Category();
    Category two = new Category();

    one.id = 5L;
    two.id = 5L;
    one.setCategoryName(null);
    two.setCategoryName("Something");

    assertNotEquals(one, two);
  }

  @Test
  void equalityWhenNamesDiffer() {
    Category one = new Category();
    Category two = new Category();

    one.setCategoryName("Landscape");
    two.setCategoryName("Portrait");

    assertNotEquals(one, two);
  }

  @Test
  void equalityWhenDifferentType() {
    Category category = new Category();
    category.id = 42L;
    category.setCategoryName("Abstract");

    assertNotEquals(category, "not-a-category");
  }

  @Test
  void hashCodeHandlesNullNameAndNonNullId() {
    Category category = new Category();
    category.id = 7L;
    category.setCategoryName(null);

    // should not throw and should be deterministic
    int hash1 = category.hashCode();
    int hash2 = category.hashCode();

    assertEquals(hash1, hash2);
  }
}
