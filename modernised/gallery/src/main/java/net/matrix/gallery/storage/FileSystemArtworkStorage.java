package net.matrix.gallery.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

/** Local-filesystem artwork storage rooted at a configurable directory. */
@Component
public class FileSystemArtworkStorage implements ArtworkStorage {

  private final Path root;

  public FileSystemArtworkStorage(
      @Value("${gallery.artwork.storage.root:./artwork-media}") String configuredRoot) {
    this.root = Path.of(configuredRoot).toAbsolutePath().normalize();
  }

  @Override
  public Optional<StoredArtwork> read(String objectKey) throws IOException {
    Path relativePath = validate(objectKey);
    Path candidate = root.resolve(relativePath).normalize();
    if (!candidate.startsWith(root) || !Files.isRegularFile(candidate)) {
      return Optional.empty();
    }

    Path realRoot = root.toRealPath();
    Path realCandidate = candidate.toRealPath();
    if (!realCandidate.startsWith(realRoot) || !Files.isRegularFile(realCandidate)) {
      throw new InvalidArtworkObjectKeyException("Artwork object key escapes storage root");
    }
    Path fileName = realCandidate.getFileName();
    if (fileName == null) {
      throw new InvalidArtworkObjectKeyException("Artwork object key has no filename");
    }

    return Optional.of(
        new StoredArtwork(
            new FileSystemResource(realCandidate),
            contentType(fileName.toString()),
            Files.size(realCandidate)));
  }

  private Path validate(String objectKey) {
    if (objectKey == null
        || objectKey.isBlank()
        || objectKey.startsWith("/")
        || objectKey.contains("\\")
        || objectKey.contains(":")) {
      throw new InvalidArtworkObjectKeyException("Artwork object key must be a relative path");
    }

    try {
      Path path = Path.of(objectKey);
      Path normalized = path.normalize();
      String canonical = normalized.toString().replace(path.getFileSystem().getSeparator(), "/");
      if (path.isAbsolute()
          || normalized.startsWith("..")
          || normalized.getNameCount() < 2
          || !canonical.equals(objectKey)) {
        throw new InvalidArtworkObjectKeyException(
            "Artwork object key must be normalized and contain no traversal");
      }
      return normalized;
    } catch (InvalidPathException exception) {
      throw new InvalidArtworkObjectKeyException("Artwork object key is invalid", exception);
    }
  }

  private String contentType(String fileName) {
    String lowerName = fileName.toLowerCase(Locale.ROOT);
    if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
      return "image/jpeg";
    }
    if (lowerName.endsWith(".png")) {
      return "image/png";
    }
    return "application/octet-stream";
  }
}
