package net.matrix.gallery.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.matrix.gallery.domain.model.ArtEntity;
import net.matrix.gallery.domain.model.Category;
import net.matrix.gallery.domain.value.ImageRendition;
import net.matrix.gallery.domain.value.RenditionType;
import net.matrix.gallery.repository.ArtworkRepository;
import net.matrix.gallery.repository.CategoryRepository;
import net.matrix.gallery.storage.ArtworkStorage;
import net.matrix.gallery.tools.ArtworkRenditionGenerator;
import net.matrix.gallery.web.ArtworkUploadForm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Validates, renders, stores, and persists one immediately public artwork. */
@Service
public class ArtworkUploadService {

  /** Caps one decoded source at about 48 MB for four-byte pixels, before decoder overhead. */
  private static final long MAXIMUM_DECODED_PIXELS = 12_000_000L;

  private final ArtworkRepository artworkRepository;
  private final CategoryRepository categoryRepository;
  private final ArtworkStorage artworkStorage;
  private final TransactionTemplate transactionTemplate;
  private final long maximumBytes;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "Spring-managed collaborators are intentionally retained by this service")
  public ArtworkUploadService(
      ArtworkRepository artworkRepository,
      CategoryRepository categoryRepository,
      ArtworkStorage artworkStorage,
      TransactionTemplate transactionTemplate,
      @Value("${gallery.artwork.upload.max-bytes:10485760}") long maximumBytes) {
    this.artworkRepository = artworkRepository;
    this.categoryRepository = categoryRepository;
    this.artworkStorage = artworkStorage;
    this.transactionTemplate = transactionTemplate;
    this.maximumBytes = maximumBytes;
  }

  public Long upload(ArtworkUploadForm form) {
    validateFile(form);
    if (!categoryRepository.existsById(form.getCategoryId())) {
      throw new ArtworkUploadException("Select an existing category.");
    }
    String identity = UUID.randomUUID().toString();
    String extension = extension(form.getImage().getOriginalFilename());
    String contentType = "png".equals(extension) ? "image/png" : "image/jpeg";
    String directoryKey = "artworks/images/" + identity;
    Path temporaryDirectory = null;
    boolean stored = false;
    try {
      temporaryDirectory = Files.createTempDirectory("gallery-artwork-upload-");
      Path original = temporaryDirectory.resolve("original." + extension);
      form.getImage().transferTo(original);
      int[] dimensions = inspectDimensions(original);

      var generated = ArtworkRenditionGenerator.generate(original);
      Path gallery = generated.get(0).path();
      Path thumbnail = generated.get(1).path();
      artworkStorage.writeDirectory(
          directoryKey,
          Map.of(
              "original." + extension, original,
              "gallery." + extension, gallery,
              "thumbnail." + extension, thumbnail));
      stored = true;

      Map<RenditionType, ImageRendition> renditions = new EnumMap<>(RenditionType.class);
      renditions.put(
          RenditionType.ORIGINAL,
          metadata(directoryKey, original, contentType, dimensions[0], dimensions[1]));
      renditions.put(
          RenditionType.GALLERY,
          metadata(
              directoryKey,
              gallery,
              contentType,
              generated.get(0).width(),
              generated.get(0).height()));
      renditions.put(
          RenditionType.THUMBNAIL,
          metadata(
              directoryKey,
              thumbnail,
              contentType,
              generated.get(1).width(),
              generated.get(1).height()));

      Long savedId =
          transactionTemplate.execute(
              status -> persist(form, identity, dimensions[0], dimensions[1], renditions));
      if (savedId == null) {
        throw new ArtworkUploadException("Artwork metadata could not be saved.");
      }
      return savedId;
    } catch (ArtworkUploadException exception) {
      cleanupStored(stored, directoryKey, exception);
      throw exception;
    } catch (IllegalArgumentException | IOException exception) {
      ArtworkUploadException uploadException =
          new ArtworkUploadException("The image could not be processed safely.", exception);
      cleanupStored(stored, directoryKey, uploadException);
      throw uploadException;
    } catch (RuntimeException exception) {
      ArtworkUploadException uploadException =
          new ArtworkUploadException("The artwork could not be saved.", exception);
      cleanupStored(stored, directoryKey, uploadException);
      throw uploadException;
    } finally {
      deleteTemporaryDirectory(temporaryDirectory);
    }
  }

  private int[] inspectDimensions(Path original) throws IOException {
    try (ImageInputStream input = ImageIO.createImageInputStream(original.toFile())) {
      if (input == null) {
        throw new ArtworkUploadException("Select a valid JPEG or PNG image.");
      }
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw new ArtworkUploadException("Select a valid JPEG or PNG image.");
      }
      ImageReader reader = readers.next();
      try {
        reader.setInput(input, true, true);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        if (width < 1 || height < 1) {
          throw new ArtworkUploadException("Select a valid JPEG or PNG image.");
        }
        if ((long) width * height > MAXIMUM_DECODED_PIXELS) {
          throw new ArtworkUploadException("The image exceeds the 12 million pixel limit.");
        }
        return new int[] {width, height};
      } finally {
        reader.dispose();
      }
    }
  }

  private Long persist(
      ArtworkUploadForm form,
      String identity,
      int width,
      int height,
      Map<RenditionType, ImageRendition> renditions) {
    Category category =
        categoryRepository
            .findById(form.getCategoryId())
            .orElseThrow(() -> new ArtworkUploadException("Select an existing category."));
    ArtEntity artwork = new ArtEntity();
    artwork.setCatalogKey("upload-" + identity);
    artwork.setTitle(form.getTitle().strip());
    artwork.setArtist(trimToNull(form.getArtist()));
    artwork.setGenre(trimToNull(form.getGenre()));
    artwork.setSubTitle(trimToNull(form.getSubTitle()));
    artwork.setMedia(trimToNull(form.getMedia()));
    artwork.setDescription(trimToNull(form.getDescription()));
    artwork.setCaption(trimToNull(form.getCaption()));
    artwork.setDisplayDate(form.getDisplayDate());
    artwork.setUploadedDate(Instant.now());
    artwork.setWidth(width);
    artwork.setHeight(height);
    artwork.setGeneralViewable(true);
    artwork.addCategory(category);
    renditions.forEach(artwork::addImageRendition);
    return artworkRepository.saveAndFlush(artwork).getId();
  }

  private void validateFile(ArtworkUploadForm form) {
    if (form.getImage() == null || form.getImage().isEmpty()) {
      throw new ArtworkUploadException("Select a JPEG or PNG image.");
    }
    if (form.getImage().getSize() > maximumBytes) {
      throw new ArtworkUploadException("The image exceeds the 10 MB upload limit.");
    }
    String extension = extension(form.getImage().getOriginalFilename());
    String suppliedType = form.getImage().getContentType();
    if (suppliedType != null
        && !(suppliedType.equalsIgnoreCase("image/jpeg")
            || suppliedType.equalsIgnoreCase("image/png"))) {
      throw new ArtworkUploadException("Select a JPEG or PNG image.");
    }
    if (!(extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png"))) {
      throw new ArtworkUploadException("Select a JPEG or PNG image.");
    }
  }

  private String extension(String fileName) {
    if (fileName == null) return "";
    int separator = fileName.lastIndexOf('.');
    return separator < 0 ? "" : fileName.substring(separator + 1).toLowerCase(Locale.ROOT);
  }

  private ImageRendition metadata(
      String directoryKey, Path path, String contentType, int width, int height)
      throws IOException {
    String storedName =
        java.util.Objects.requireNonNull(path.getFileName())
            .toString()
            .replace("g-original", "gallery")
            .replace("t-original", "thumbnail");
    return new ImageRendition(
        directoryKey + "/" + storedName,
        contentType,
        Files.size(path),
        width,
        height,
        checksum(path));
  }

  private String checksum(Path path) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (InputStream input = Files.newInputStream(path)) {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private void cleanupStored(boolean stored, String directoryKey, RuntimeException failure) {
    if (!stored) return;
    try {
      artworkStorage.deleteDirectory(directoryKey);
    } catch (IOException cleanupFailure) {
      failure.addSuppressed(cleanupFailure);
    }
  }

  private void deleteTemporaryDirectory(Path directory) {
    if (directory == null) return;
    try (var paths = Files.walk(directory)) {
      for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
        Files.deleteIfExists(path);
      }
    } catch (IOException ignored) {
      // Operating-system temporary storage will remove any exceptional residue.
    }
  }
}
