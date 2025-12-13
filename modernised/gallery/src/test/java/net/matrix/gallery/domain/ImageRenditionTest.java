package net.matrix.gallery.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for ImageRendition record behavior.
 *
 * @author Anand Hemadri
 */
class ImageRenditionTest {

  @Test
  void recordHoldsProvidedValues() {
    ImageRendition rendition =
        new ImageRendition(
            RenditionType.GALLERY,
            "objects/gallery.jpg",
            "image/jpeg",
            512L,
            357,
            312,
            "md5-gallery");

    assertEquals(RenditionType.GALLERY, rendition.type());
    assertEquals("objects/gallery.jpg", rendition.objectKey());
    assertEquals("image/jpeg", rendition.contentType());
    assertEquals(512L, rendition.sizeBytes());
    assertEquals(357, rendition.width());
    assertEquals(312, rendition.height());
    assertEquals("md5-gallery", rendition.checksum());
  }
}
