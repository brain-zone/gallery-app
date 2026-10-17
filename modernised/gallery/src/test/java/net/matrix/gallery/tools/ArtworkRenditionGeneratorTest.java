package net.matrix.gallery.tools;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArtworkRenditionGeneratorTest {

  @TempDir Path temporaryDirectory;

  @Test
  void generatesExpectedNamesAndAspectRatioConstrainedDimensions() throws IOException {
    Path source = writeImage("artwork.jpeg", 1600, 900, BufferedImage.TYPE_INT_RGB, "jpeg");

    List<ArtworkRenditionGenerator.GeneratedRendition> generated =
        ArtworkRenditionGenerator.generate(source);

    assertEquals("g-artwork.jpeg", generated.get(0).path().getFileName().toString());
    assertEquals("t-artwork.jpeg", generated.get(1).path().getFileName().toString());
    assertDimensions(generated.get(0).path(), 800, 450);
    assertDimensions(generated.get(1).path(), 96, 54);
  }

  @Test
  void doesNotUpscaleAndDoesNotChangeTheSource() throws IOException {
    Path source = writeImage("small.jpg", 50, 40, BufferedImage.TYPE_INT_RGB, "jpeg");
    byte[] originalBytes = Files.readAllBytes(source);

    ArtworkRenditionGenerator.generate(source);

    assertDimensions(temporaryDirectory.resolve("g-small.jpg"), 50, 40);
    assertDimensions(temporaryDirectory.resolve("t-small.jpg"), 50, 40);
    assertArrayEquals(originalBytes, Files.readAllBytes(source));
  }

  @Test
  void preservesPngTransparency() throws IOException {
    BufferedImage transparentImage = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = transparentImage.createGraphics();
    try {
      graphics.setColor(new Color(10, 20, 30, 255));
      graphics.fillRect(100, 0, 100, 100);
    } finally {
      graphics.dispose();
    }
    Path source = temporaryDirectory.resolve("transparent.png");
    assertTrue(ImageIO.write(transparentImage, "png", source.toFile()));

    ArtworkRenditionGenerator.generate(source);

    BufferedImage thumbnail =
        ImageIO.read(temporaryDirectory.resolve("t-transparent.png").toFile());
    assertTrue(thumbnail.getColorModel().hasAlpha());
    assertEquals(0, (thumbnail.getRGB(10, 10) >>> 24) & 0xff);
    assertEquals(255, (thumbnail.getRGB(80, 10) >>> 24) & 0xff);
  }

  @Test
  void safelyReplacesExistingRenditions() throws IOException {
    Path source = writeImage("replace.png", 200, 100, BufferedImage.TYPE_INT_RGB, "png");
    Files.writeString(temporaryDirectory.resolve("g-replace.png"), "old gallery rendition");
    Files.writeString(temporaryDirectory.resolve("t-replace.png"), "old thumbnail rendition");

    ArtworkRenditionGenerator.generate(source);

    assertDimensions(temporaryDirectory.resolve("g-replace.png"), 200, 100);
    assertDimensions(temporaryDirectory.resolve("t-replace.png"), 96, 48);
  }

  @Test
  void generatesForImmediateDirectorySourcesOnly() throws IOException {
    writeImage("first.jpg", 200, 100, BufferedImage.TYPE_INT_RGB, "jpeg");
    writeImage("second.png", 100, 200, BufferedImage.TYPE_INT_RGB, "png");
    writeImage("g-first.jpg", 10, 10, BufferedImage.TYPE_INT_RGB, "jpeg");
    Path nestedDirectory = Files.createDirectory(temporaryDirectory.resolve("nested"));
    writeImage(nestedDirectory, "nested.jpg", 200, 100, BufferedImage.TYPE_INT_RGB, "jpeg");

    List<ArtworkRenditionGenerator.GeneratedRendition> generated =
        ArtworkRenditionGenerator.generateAll(temporaryDirectory);

    assertEquals(4, generated.size());
    assertDimensions(temporaryDirectory.resolve("g-first.jpg"), 200, 100);
    assertDimensions(temporaryDirectory.resolve("t-first.jpg"), 96, 48);
    assertDimensions(temporaryDirectory.resolve("g-second.png"), 100, 200);
    assertDimensions(temporaryDirectory.resolve("t-second.png"), 48, 96);
    assertTrue(Files.notExists(nestedDirectory.resolve("g-nested.jpg")));
    assertTrue(Files.notExists(nestedDirectory.resolve("t-nested.jpg")));
  }

  @Test
  void rejectsInvalidUnsupportedAndRenditionInputs() throws IOException {
    Path invalidImage = temporaryDirectory.resolve("invalid.jpg");
    Files.writeString(invalidImage, "not an image");
    Path unsupportedImage = temporaryDirectory.resolve("artwork.webp");
    Files.writeString(unsupportedImage, "not a supported image");
    Path renditionSource = writeImage("g-artwork.png", 10, 10, BufferedImage.TYPE_INT_RGB, "png");

    assertThrows(
        IllegalArgumentException.class, () -> ArtworkRenditionGenerator.generate(invalidImage));
    assertThrows(
        IllegalArgumentException.class, () -> ArtworkRenditionGenerator.generate(unsupportedImage));
    assertThrows(
        IllegalArgumentException.class, () -> ArtworkRenditionGenerator.generate(renditionSource));
  }

  private Path writeImage(String name, int width, int height, int imageType, String format)
      throws IOException {
    return writeImage(temporaryDirectory, name, width, height, imageType, format);
  }

  private Path writeImage(
      Path directory, String name, int width, int height, int imageType, String format)
      throws IOException {
    BufferedImage image = new BufferedImage(width, height, imageType);
    Graphics2D graphics = image.createGraphics();
    try {
      graphics.setColor(new Color(50, 100, 150));
      graphics.fillRect(0, 0, width, height);
    } finally {
      graphics.dispose();
    }
    Path path = directory.resolve(name);
    assertTrue(ImageIO.write(image, format, path.toFile()));
    return path;
  }

  private static void assertDimensions(Path imagePath, int expectedWidth, int expectedHeight)
      throws IOException {
    BufferedImage image = ImageIO.read(imagePath.toFile());
    assertEquals(expectedWidth, image.getWidth());
    assertEquals(expectedHeight, image.getHeight());
  }
}
