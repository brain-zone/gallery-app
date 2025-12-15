package net.matrix.gallery.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for ArtEntity domain behavior.
 *
 * @author Anand Hemadri
 */
class ArtEntityTest {

  @Test
  void addsRenditionAndFindsViaHelpers() {
    ArtEntity art = new ArtEntity();
    ImageRendition thumb =
        new ImageRendition("objects/thumb.jpg", "image/jpeg", 128L, 48, 48, "md5-thumb");

    art.addImageRendition(RenditionType.THUMBNAIL, thumb);

    assertSame(thumb, art.getThumbnailPicture());
    assertSame(thumb, art.getImageRendition().get(RenditionType.THUMBNAIL));
    assertEquals(1, art.getImageRendition().size());
  }

  @Test
  void renditionCollectionIsUnmodifiable() {
    ArtEntity art = new ArtEntity();
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            art.getImageRendition()
                .put(
                    RenditionType.GALLERY,
                    new ImageRendition(
                        "objects/gallery.jpg", "image/jpeg", 256L, 357, 312, "md5-gallery")));

    assertThrows(
        UnsupportedOperationException.class,
        () -> art.getImageRendition().remove(RenditionType.GALLERY));
  }

  @Test
  void removeRenditionDropsEntry() {
    ArtEntity art = new ArtEntity();
    ImageRendition gallery =
        new ImageRendition("objects/gallery.jpg", "image/jpeg", 256L, 357, 312, "md5-gallery");

    art.addImageRendition(RenditionType.GALLERY, gallery);
    assertEquals(1, art.getImageRendition().size());

    art.removeImageRendition(RenditionType.GALLERY);
    assertEquals(0, art.getImageRendition().size());
  }

  @Test
  void galleryAndOriginalRenditionsAreAccessible() {
    ArtEntity art = new ArtEntity();
    ImageRendition gallery =
        new ImageRendition("objects/gallery.jpg", "image/jpeg", 256L, 357, 312, "md5-gallery");
    ImageRendition original =
        new ImageRendition("objects/original.jpg", "image/jpeg", 1024L, 1200, 800, "md5-original");

    art.addImageRendition(RenditionType.GALLERY, gallery);
    art.addImageRendition(RenditionType.ORIGINAL, original);

    assertSame(gallery, art.getGalleryPicture());
    assertSame(original, art.getStoragePicture());
    assertSame(gallery, art.getImageRendition().get(RenditionType.GALLERY));
    assertSame(original, art.getImageRendition().get(RenditionType.ORIGINAL));
  }

  @Test
  void categoriesAreUnmodifiableAfterAccess() {
    ArtEntity art = new ArtEntity();
    Category category = new Category();
    category.setCategoryName("Landscapes");

    art.addCategory(category);

    assertEquals(1, art.getCategories().size());
    assertSame(category, art.getCategories().iterator().next());
    assertThrows(
        UnsupportedOperationException.class, () -> art.getCategories().add(new Category()));
    assertEquals(1, category.getArtEntities().size());
    assertSame(art, category.getArtEntities().iterator().next());
    assertThrows(
        UnsupportedOperationException.class, () -> category.getArtEntities().add(new ArtEntity()));
  }

  @Test
  void commentsAreUnmodifiableAfterAccess() {
    ArtEntity art = new ArtEntity();
    Comment comment = new Comment();
    comment.setComment("Nice piece");

    art.addComment(comment);

    assertEquals(1, art.getComments().size());
    assertSame(comment, art.getComments().iterator().next());
    assertThrows(UnsupportedOperationException.class, () -> art.getComments().add(new Comment()));
  }

  @Test
  void removingCategoryAndCommentWorks() {
    ArtEntity art = new ArtEntity();
    Category category = new Category();
    Comment comment = new Comment();

    art.addCategory(category);
    art.addComment(comment);
    assertEquals(1, art.getCategories().size());
    assertEquals(1, art.getComments().size());
    assertEquals(1, category.getArtEntities().size());

    art.removeCategory(category);
    art.removeComment(comment);
    assertEquals(0, art.getCategories().size());
    assertEquals(0, art.getComments().size());
    assertEquals(0, category.getArtEntities().size());
    assertNull(comment.getCommentedArt());
  }

  @Test
  void viewableFlagsReflectState() {
    ArtEntity art = new ArtEntity();

    // defaults should be false
    assertFalse(art.isGeneralViewable());
    assertFalse(art.isPrivilegeViewable());

    art.setGeneralViewable(true);
    art.setPrivilegeViewable(true);

    assertTrue(art.isGeneralViewable());
    assertTrue(art.isPrivilegeViewable());
  }

  @Test
  void equalityUsesIdOnly() {
    ArtEntity one = new ArtEntity();
    one.setId(1L);
    one.setTitle("One");

    ArtEntity two = new ArtEntity();
    two.setId(2L);
    two.setTitle("Two");

    ArtEntity three = new ArtEntity();
    three.setId(1L);
    three.setTitle("Three");

    assertNotEquals(one, two);
    assertEquals(one, three);
  }
}
