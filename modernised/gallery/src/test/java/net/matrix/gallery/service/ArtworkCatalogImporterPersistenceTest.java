package net.matrix.gallery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.matrix.gallery.domain.model.ArtEntity;
import net.matrix.gallery.repository.ArtworkRepository;
import net.matrix.gallery.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest(
    properties = "spring.datasource.url=jdbc:h2:mem:catalog-import-persistence;DB_CLOSE_DELAY=-1")
class ArtworkCatalogImporterPersistenceTest {

  @Autowired private ArtworkCatalogImporter importer;
  @Autowired private ArtworkRepository artworkRepository;
  @Autowired private CategoryRepository categoryRepository;

  @TempDir Path temporaryDirectory;

  @Test
  void startupImportPersistsTheCatalogAndRepeatImportIsIdempotent() {
    long artworkCount = artworkRepository.count();
    long categoryCount = categoryRepository.count();

    var result = importer.importBundledCatalog();

    assertThat(result.catalogSize()).isEqualTo(10);
    assertThat(result.artworksCreated()).isZero();
    assertThat(result.artworksUpdated()).isEqualTo(10);
    assertThat(result.categoriesCreated()).isZero();
    assertThat(artworkRepository.count()).isEqualTo(artworkCount);
    assertThat(categoryRepository.count()).isEqualTo(categoryCount);
    ArtEntity imported =
        artworkRepository
            .findByCatalogKeyIgnoreCase("ABSTRACT_DREAM_PAINTING.JPG")
            .flatMap(artwork -> artworkRepository.findDetailById(artwork.getId()))
            .orElseThrow();
    assertThat(imported)
        .satisfies(
            artwork -> {
              assertThat(artwork.getCatalogKey()).isEqualTo("abstract_dream_painting.jpg");
              assertThat(artwork.getArtist()).isEqualTo("Liana Corvus");
              assertThat(artwork.getGenre()).isEqualTo("Surrealism");
              assertThat(artwork.getMedia()).isNull();
              assertThat(artwork.getSubTitle()).isNull();
              assertThat(artwork.getCategories()).hasSize(1);
              assertThat(artwork.getImageRendition()).hasSize(1);
            });
  }

  @Test
  void missingImageValidationLeavesNoPartialDatabaseState() throws IOException {
    long artworkCount = artworkRepository.count();
    long categoryCount = categoryRepository.count();
    Files.createDirectories(temporaryDirectory.resolve("images"));
    BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
    ImageIO.write(image, "png", temporaryDirectory.resolve("images/present.png").toFile());
    Path catalog = temporaryDirectory.resolve("artworks.json");
    Files.writeString(
        catalog,
        "{\"artworks\":["
            + artwork("First", "first.jpg", "images/present.png")
            + ","
            + artwork("Second", "second.jpg", "images/missing.png")
            + "]}");

    assertThatThrownBy(() -> importer.importCatalog(new FileSystemResource(catalog)))
        .isInstanceOf(ArtworkCatalogImportException.class)
        .hasMessageContaining("refers to missing image images/missing.png");
    assertThat(artworkRepository.count()).isEqualTo(artworkCount);
    assertThat(categoryRepository.count()).isEqualTo(categoryCount);
    assertThat(artworkRepository.findByCatalogKeyIgnoreCase("first.jpg")).isEmpty();
  }

  @Test
  void normalizedCatalogKeyIsProtectedByTheV4UniqueIndex() {
    ArtEntity first = new ArtEntity();
    first.setTitle("Unique index first");
    first.setCatalogKey(" Unique-Index.JPG ");
    artworkRepository.saveAndFlush(first);

    try {
      ArtEntity duplicate = new ArtEntity();
      duplicate.setTitle("Unique index duplicate");
      duplicate.setCatalogKey("unique-index.jpg");

      assertThatThrownBy(() -> artworkRepository.saveAndFlush(duplicate))
          .isInstanceOf(DataIntegrityViolationException.class);
    } finally {
      artworkRepository.deleteById(first.getId());
    }
  }

  @Test
  void v4RemovesTheSyntheticV3ArtworkOnly() {
    assertThat(artworkRepository.findAll())
        .noneMatch(artwork -> "Evening Sky".equals(artwork.getTitle()));
    assertThat(artworkRepository.count()).isGreaterThanOrEqualTo(10);
  }

  private String artwork(String title, String key, String fileName) {
    return "{"
        + "\"title\":\""
        + title
        + "\",\"category\":\"Atomic Category\",\"artist\":\"Atomic Artist\","
        + "\"description\":\"Atomic validation fixture\",\"fileName\":\""
        + fileName
        + "\",\"logicalFileName\":\""
        + key
        + "\",\"genre\":\"Test\","
        + "\"prices\":[{\"type\":\"SELL\",\"amount\":1.00,\"currency\":\"USD\"}]}";
  }
}
