package net.matrix.gallery.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        new ImageRendition(
            RenditionType.THUMBNAIL, "objects/thumb.jpg", "image/jpeg", 128L, 48, 48, "md5-thumb");

    art.addImageRendition(RenditionType.THUMBNAIL, thumb);

    assertSame(thumb, art.getThumbnailPicture());
    assertSame(thumb, art.getRenditions().get(RenditionType.THUMBNAIL));
    assertEquals(1, art.getRenditions().size());
  }

  @Test
  void renditionCollectionIsUnmodifiable() {
    ArtEntity art = new ArtEntity();

    assertThrows(
        UnsupportedOperationException.class,
        () ->
            art.getRenditions()
                .put(
                    RenditionType.GALLERY,
                    new ImageRendition(
                        RenditionType.GALLERY,
                        "objects/gallery.jpg",
                        "image/jpeg",
                        256L,
                        357,
                        312,
                        "md5-gallery")));

    assertThrows(
        UnsupportedOperationException.class,
        () -> art.getRenditions().remove(RenditionType.GALLERY));
  }

  @Test
  void removeRenditionDropsEntry() {
    ArtEntity art = new ArtEntity();
    ImageRendition gallery =
        new ImageRendition(
            RenditionType.GALLERY,
            "objects/gallery.jpg",
            "image/jpeg",
            256L,
            357,
            312,
            "md5-gallery");

    art.addImageRendition(RenditionType.GALLERY, gallery);
    assertEquals(1, art.getRenditions().size());

    art.removeImageRendition(RenditionType.GALLERY);
    assertEquals(0, art.getRenditions().size());
  }

  @Test
  void galleryAndOriginalRenditionsAreAccessible() {
    ArtEntity art = new ArtEntity();
    ImageRendition gallery =
        new ImageRendition(
            RenditionType.GALLERY,
            "objects/gallery.jpg",
            "image/jpeg",
            256L,
            357,
            312,
            "md5-gallery");
    ImageRendition original =
        new ImageRendition(
            RenditionType.ORIGINAL,
            "objects/original.jpg",
            "image/jpeg",
            1024L,
            1200,
            800,
            "md5-original");

    art.addImageRendition(RenditionType.GALLERY, gallery);
    art.addImageRendition(RenditionType.ORIGINAL, original);

    assertSame(gallery, art.getGalleryPicture());
    assertSame(original, art.getStoragePicture());
    assertSame(gallery, art.getRenditions().get(RenditionType.GALLERY));
    assertSame(original, art.getRenditions().get(RenditionType.ORIGINAL));
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

    art.removeCategory(category);
    art.removeComment(comment);
    assertEquals(0, art.getCategories().size());
    assertEquals(0, art.getComments().size());
  }

  @Test
  void equalityWhenIdsAreNull() {
    ArtEntity one = new ArtEntity();
    ArtEntity two = new ArtEntity();

    assertNotEquals(one, two);
    assertEquals(one, one); // self check
  }

  @Test
  void equalityWhenIdPresent() {
    ArtEntity one = new ArtEntity();
    ArtEntity two = new ArtEntity();
    ArtEntity three = new ArtEntity();

    one.id = 1L;
    two.id = 1L;
    three.id = 2L;

    assertEquals(one, two);
    assertNotEquals(one, three);
    // id set vs null id should be false
    ArtEntity nullId = new ArtEntity();
    assertNotEquals(one, nullId);
  }

  @Test
  void equalityWhenDifferentType() {
    ArtEntity art = new ArtEntity();
    art.id = 1L;

    assertNotEquals(art, "not-an-art-entity");
  }

  @Test
  void viewableFlagsReflectState() {
    ArtEntity art = new ArtEntity();

    // defaults should be false
    assertEquals(false, art.isGeneralViewable());
    assertEquals(false, art.isPrivilegeViewable());

    art.setGeneralViewable(true);
    art.setPrivilegeViewable(true);

    assertEquals(true, art.isGeneralViewable());
    assertEquals(true, art.isPrivilegeViewable());
  }
}
