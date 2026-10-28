package net.matrix.gallery.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileSystemArtworkStorageTest {

  @TempDir Path temporaryDirectory;

  @Test
  void readsExactBytesAndContentTypeFromConfiguredRoot() throws Exception {
    Path image = temporaryDirectory.resolve("artworks/images/example.jpeg");
    Files.createDirectories(Objects.requireNonNull(image.getParent()));
    byte[] expected = {1, 2, 3, 4};
    Files.write(image, expected);

    StoredArtwork stored =
        new FileSystemArtworkStorage(temporaryDirectory.toString())
            .read("artworks/images/example.jpeg")
            .orElseThrow();

    assertThat(stored.resource().getInputStream().readAllBytes()).isEqualTo(expected);
    assertThat(stored.contentType()).isEqualTo("image/jpeg");
    assertThat(stored.contentLength()).isEqualTo(expected.length);
  }

  @Test
  void returnsEmptyForMissingSafeObject() throws Exception {
    assertThat(
            new FileSystemArtworkStorage(temporaryDirectory.toString())
                .read("artworks/images/missing.png"))
        .isEmpty();
  }

  @Test
  void rejectsTraversalAbsoluteBackslashSchemeAndNonNormalizedKeys() {
    FileSystemArtworkStorage storage = new FileSystemArtworkStorage(temporaryDirectory.toString());

    assertThatThrownBy(() -> storage.read("../secret.png"))
        .isInstanceOf(InvalidArtworkObjectKeyException.class);
    assertThatThrownBy(() -> storage.read("/absolute.png"))
        .isInstanceOf(InvalidArtworkObjectKeyException.class);
    assertThatThrownBy(() -> storage.read("artworks\\images\\image.png"))
        .isInstanceOf(InvalidArtworkObjectKeyException.class);
    assertThatThrownBy(() -> storage.read("file:secret.png"))
        .isInstanceOf(InvalidArtworkObjectKeyException.class);
    assertThatThrownBy(() -> storage.read("artworks/../secret.png"))
        .isInstanceOf(InvalidArtworkObjectKeyException.class);
  }

  @Test
  void rejectsSymlinkThatEscapesStorageRoot() throws Exception {
    Path outside = Files.createTempFile("gallery-outside-", ".png");
    Path link = temporaryDirectory.resolve("artworks/images/link.png");
    Files.createDirectories(Objects.requireNonNull(link.getParent()));
    Files.createSymbolicLink(link, outside);

    assertThatThrownBy(
            () ->
                new FileSystemArtworkStorage(temporaryDirectory.toString())
                    .read("artworks/images/link.png"))
        .isInstanceOf(InvalidArtworkObjectKeyException.class);
  }

  @Test
  void writesAndDeletesOneCompleteArtworkDirectory() throws Exception {
    Path original = Files.write(temporaryDirectory.resolve("source.jpeg"), new byte[] {1, 2, 3});
    Path gallery = Files.write(temporaryDirectory.resolve("gallery.jpeg"), new byte[] {4, 5});
    FileSystemArtworkStorage storage = new FileSystemArtworkStorage(temporaryDirectory.toString());

    storage.writeDirectory(
        "artworks/images/identity", Map.of("original.jpeg", original, "gallery.jpeg", gallery));

    assertThat(storage.read("artworks/images/identity/original.jpeg").orElseThrow().contentLength())
        .isEqualTo(3);
    assertThat(storage.read("artworks/images/identity/gallery.jpeg").orElseThrow().contentLength())
        .isEqualTo(2);

    storage.deleteDirectory("artworks/images/identity");
    assertThat(temporaryDirectory.resolve("artworks/images/identity")).doesNotExist();
  }

  @Test
  void refusesToOverwriteAnExistingArtworkDirectory() throws Exception {
    Path source = Files.write(temporaryDirectory.resolve("source.png"), new byte[] {1});
    FileSystemArtworkStorage storage = new FileSystemArtworkStorage(temporaryDirectory.toString());
    storage.writeDirectory("artworks/images/identity", Map.of("original.png", source));

    assertThatThrownBy(
            () ->
                storage.writeDirectory("artworks/images/identity", Map.of("original.png", source)))
        .isInstanceOf(InvalidArtworkObjectKeyException.class);
  }
}
