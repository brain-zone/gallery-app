package net.matrix.gallery.tools;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/** Generates gallery and thumbnail renditions for JPEG and PNG artwork images. */
public final class ArtworkRenditionGenerator {

  private static final RenditionSpec GALLERY = new RenditionSpec("g-", 800, 600);
  private static final RenditionSpec THUMBNAIL = new RenditionSpec("t-", 96, 96);

  private ArtworkRenditionGenerator() {}

  public static void main(String[] arguments) {
    if (arguments.length != 1) {
      System.err.println("Usage: ArtworkRenditionGenerator <source-image-path-or-directory>");
      System.exit(2);
    }

    try {
      for (GeneratedRendition rendition : generateAll(Path.of(arguments[0]))) {
        System.out.printf(
            "Generated %s (%d x %d)%n",
            rendition.path().getFileName(), rendition.width(), rendition.height());
      }
    } catch (IllegalArgumentException | IOException exception) {
      System.err.println("Unable to generate artwork renditions: " + exception.getMessage());
      System.exit(1);
    }
  }

  /** Generates renditions for one image or for each immediate source file in one directory. */
  public static List<GeneratedRendition> generateAll(Path sourceOrDirectory) throws IOException {
    if (sourceOrDirectory == null) {
      throw new IllegalArgumentException("Source path must not be null");
    }

    Path path = sourceOrDirectory.toAbsolutePath().normalize();
    if (!Files.isDirectory(path)) {
      return generate(path);
    }

    List<Path> sources;
    try (var entries = Files.list(path)) {
      sources =
          entries
              .filter(Files::isRegularFile)
              .filter(entry -> !isRenditionFileName(requireFileName(entry).toString()))
              .sorted(Comparator.comparing(entry -> requireFileName(entry).toString()))
              .toList();
    }

    List<GeneratedRendition> generated = new ArrayList<>(sources.size() * 2);
    for (Path source : sources) {
      generated.addAll(generate(source));
    }
    return List.copyOf(generated);
  }

  /** Generates both renditions beside {@code sourcePath}, replacing existing renditions safely. */
  public static List<GeneratedRendition> generate(Path sourcePath) throws IOException {
    Path source = validateSourcePath(sourcePath);
    ImageFormat imageFormat = ImageFormat.fromFileName(requireFileName(source).toString());
    BufferedImage sourceImage = readAndValidateImage(source, imageFormat);

    return List.of(
        generateRendition(source, sourceImage, imageFormat, GALLERY),
        generateRendition(source, sourceImage, imageFormat, THUMBNAIL));
  }

  private static Path validateSourcePath(Path sourcePath) {
    if (sourcePath == null) {
      throw new IllegalArgumentException("Source image path must not be null");
    }

    Path source = sourcePath.toAbsolutePath().normalize();
    if (!Files.isRegularFile(source) || !Files.isReadable(source)) {
      throw new IllegalArgumentException("Source image is not a readable file: " + source);
    }

    String fileName = requireFileName(source).toString();
    if (isRenditionFileName(fileName)) {
      throw new IllegalArgumentException("A rendition cannot be used as the source: " + fileName);
    }
    return source;
  }

  private static boolean isRenditionFileName(String fileName) {
    return fileName.startsWith("g-") || fileName.startsWith("t-");
  }

  private static BufferedImage readAndValidateImage(Path source, ImageFormat expectedFormat)
      throws IOException {
    try (ImageInputStream imageInput = ImageIO.createImageInputStream(source.toFile())) {
      if (imageInput == null) {
        throw new IllegalArgumentException("Unable to read source image: " + source);
      }

      Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
      if (!readers.hasNext()) {
        throw new IllegalArgumentException("Invalid or unsupported source image: " + source);
      }

      ImageReader reader = readers.next();
      try {
        String detectedFormat = reader.getFormatName();
        if (!expectedFormat.matches(detectedFormat)) {
          throw new IllegalArgumentException(
              "Source image content does not match its extension: " + source.getFileName());
        }

        reader.setInput(imageInput, true, true);
        BufferedImage image = reader.read(0);
        if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
          throw new IllegalArgumentException("Invalid source image: " + source);
        }
        return image;
      } catch (IOException exception) {
        throw new IllegalArgumentException(
            "Invalid or unreadable source image: " + source, exception);
      } finally {
        reader.dispose();
      }
    }
  }

  private static GeneratedRendition generateRendition(
      Path source, BufferedImage sourceImage, ImageFormat imageFormat, RenditionSpec specification)
      throws IOException {
    int sourceWidth = sourceImage.getWidth();
    int sourceHeight = sourceImage.getHeight();
    double scale =
        Math.min(
            1.0,
            Math.min(
                specification.maximumWidth() / (double) sourceWidth,
                specification.maximumHeight() / (double) sourceHeight));
    int targetWidth =
        Math.min(specification.maximumWidth(), Math.max(1, (int) Math.round(sourceWidth * scale)));
    int targetHeight =
        Math.min(
            specification.maximumHeight(), Math.max(1, (int) Math.round(sourceHeight * scale)));

    BufferedImage rendition = createRendition(sourceImage, targetWidth, targetHeight, imageFormat);
    Path target = source.resolveSibling(specification.prefix() + source.getFileName());
    writeSafely(rendition, imageFormat, target);
    return new GeneratedRendition(target, targetWidth, targetHeight);
  }

  private static BufferedImage createRendition(
      BufferedImage source, int width, int height, ImageFormat imageFormat) {
    int imageType =
        imageFormat == ImageFormat.PNG && source.getColorModel().hasAlpha()
            ? BufferedImage.TYPE_INT_ARGB
            : BufferedImage.TYPE_INT_RGB;
    BufferedImage rendition = new BufferedImage(width, height, imageType);
    Graphics2D graphics = rendition.createGraphics();
    try {
      graphics.setComposite(AlphaComposite.Src);
      graphics.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.setRenderingHint(
          RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
      graphics.setRenderingHint(
          RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
      graphics.drawImage(source, 0, 0, width, height, null);
    } finally {
      graphics.dispose();
    }
    return rendition;
  }

  private static void writeSafely(BufferedImage image, ImageFormat imageFormat, Path target)
      throws IOException {
    Path temporaryFile =
        Files.createTempFile(requireParent(target), "." + requireFileName(target) + "-", ".tmp");
    try {
      if (!ImageIO.write(image, imageFormat.writerName(), temporaryFile.toFile())) {
        throw new IOException("No ImageIO writer is available for " + imageFormat.writerName());
      }
      try {
        Files.move(
            temporaryFile,
            target,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException exception) {
        Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporaryFile);
    }
  }

  public record GeneratedRendition(Path path, int width, int height) {}

  private record RenditionSpec(String prefix, int maximumWidth, int maximumHeight) {}

  private enum ImageFormat {
    JPEG("JPEG"),
    PNG("PNG");

    private final String writerName;

    ImageFormat(String writerName) {
      this.writerName = writerName;
    }

    private static ImageFormat fromFileName(String fileName) {
      String lowerCaseName = fileName.toLowerCase(Locale.ROOT);
      if (lowerCaseName.endsWith(".jpeg") || lowerCaseName.endsWith(".jpg")) {
        return JPEG;
      }
      if (lowerCaseName.endsWith(".png")) {
        return PNG;
      }
      throw new IllegalArgumentException(
          "Unsupported source image format (expected .jpeg, .jpg, or .png): " + fileName);
    }

    private boolean matches(String detectedFormat) {
      return this == PNG
          ? "png".equalsIgnoreCase(detectedFormat)
          : "jpeg".equalsIgnoreCase(detectedFormat) || "jpg".equalsIgnoreCase(detectedFormat);
    }

    private String writerName() {
      return writerName;
    }
  }

  private static Path requireFileName(Path path) {
    return Objects.requireNonNull(path.getFileName(), "Path must have a file name: " + path);
  }

  private static Path requireParent(Path path) {
    return Objects.requireNonNull(path.getParent(), "Path must have a parent: " + path);
  }
}
