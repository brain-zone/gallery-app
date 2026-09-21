package net.matrix.gallery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import net.matrix.gallery.domain.model.ArtEntity;
import net.matrix.gallery.domain.model.Category;
import net.matrix.gallery.domain.value.RenditionType;
import net.matrix.gallery.repository.ArtworkRepository;
import net.matrix.gallery.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;

class ArtworkCatalogImporterTest {

  @TempDir Path temporaryDirectory;

  @Test
  void parsesAndImportsTheBundledCatalogAcrossMultipleCategories() {
    ImportHarness harness = new ImportHarness();

    var result =
        harness.importer.importCatalog(new ClassPathResource("static/artworks/artworks.json"));

    assertThat(result.catalogSize()).isEqualTo(10);
    assertThat(result.categoriesCreated()).isEqualTo(5);
    assertThat(result.artworksCreated()).isEqualTo(10);
    assertThat(harness.categories.keySet())
        .containsExactlyInAnyOrder("abstract", "landscape", "portrait", "sculpture", "urban");
    assertThat(harness.artworks).hasSize(10);
  }

  @Test
  void importsOneArtworkWithMetadataAssociationAndDerivedRendition() throws IOException {
    ImportHarness harness = new ImportHarness();
    FileSystemResource catalog = catalog(singleArtwork("Abstract", "abstract-one.jpg", "One"));

    var result = harness.importer.importCatalog(catalog);

    ArtEntity artwork = harness.artworks.get("abstract-one.jpg");
    assertThat(result.artworksCreated()).isOne();
    assertThat(artwork.getTitle()).isEqualTo("One");
    assertThat(artwork.getArtist()).isEqualTo("Catalog Artist");
    assertThat(artwork.getGenre()).isEqualTo("Catalog Genre");
    assertThat(artwork.getSubTitle()).isNull();
    assertThat(artwork.getMedia()).isNull();
    assertThat(artwork.getDescription()).isEqualTo("Catalog description");
    assertThat(artwork.getCategories())
        .extracting(Category::getCategoryName)
        .containsExactly("Abstract");
    assertThat(artwork.getGalleryPicture().objectKey()).isEqualTo("artworks/images/test.png");
    assertThat(artwork.getGalleryPicture().contentType()).isEqualTo("image/png");
    assertThat(artwork.getGalleryPicture().width()).isEqualTo(3);
    assertThat(artwork.getGalleryPicture().height()).isEqualTo(2);
    assertThat(artwork.getGalleryPicture().sizeBytes()).isPositive();
    assertThat(artwork.getGalleryPicture().checksum()).hasSize(64);
  }

  @Test
  void reusesAnExistingCategory() throws IOException {
    ImportHarness harness = new ImportHarness();
    Category existing = new Category();
    existing.setCategoryName("Abstract");
    harness.categories.put("abstract", existing);

    var result =
        harness.importer.importCatalog(
            catalog(singleArtwork("Abstract", "abstract-one.jpg", "One")));

    assertThat(result.categoriesCreated()).isZero();
    assertThat(harness.artworks.get("abstract-one.jpg").getCategories()).containsExactly(existing);
    verify(harness.categoryRepository, never()).save(any(Category.class));
  }

  @Test
  void createsAGenuinelyMissingCategory() throws IOException {
    ImportHarness harness = new ImportHarness();

    var result =
        harness.importer.importCatalog(
            catalog(singleArtwork("Abstract", "abstract-one.jpg", "One")));

    assertThat(result.categoriesCreated()).isOne();
    assertThat(harness.categories).containsKey("abstract");
  }

  @Test
  void repeatedImportIsIdempotent() throws IOException {
    ImportHarness harness = new ImportHarness();
    FileSystemResource catalog = catalog(singleArtwork("Abstract", "abstract-one.jpg", "One"));

    var first = harness.importer.importCatalog(catalog);
    var second = harness.importer.importCatalog(catalog);

    assertThat(first.artworksCreated()).isOne();
    assertThat(second.artworksCreated()).isZero();
    assertThat(second.artworksUpdated()).isOne();
    assertThat(harness.artworks).hasSize(1);
    assertThat(harness.categories).hasSize(1);
    ArtEntity artwork = harness.artworks.get("abstract-one.jpg");
    assertThat(artwork.getCategories()).hasSize(1);
    assertThat(artwork.getImageRendition()).containsOnlyKeys(RenditionType.GALLERY);
  }

  @Test
  void missingImageFailsClearlyBeforeImport() throws IOException {
    ImportHarness harness = new ImportHarness();
    Files.createDirectories(temporaryDirectory.resolve("images"));
    Path catalogFile = temporaryDirectory.resolve("artworks.json");
    Files.writeString(catalogFile, singleArtwork("Abstract", "abstract-one.jpg", "One"));

    assertThatThrownBy(() -> harness.importer.importCatalog(new FileSystemResource(catalogFile)))
        .isInstanceOf(ArtworkCatalogImportException.class)
        .hasMessageContaining("refers to missing image images/test.png");
    assertThat(harness.artworks).isEmpty();
  }

  @Test
  void malformedJsonFailsClearly() throws IOException {
    ImportHarness harness = new ImportHarness();
    Path catalogFile = temporaryDirectory.resolve("artworks.json");
    Files.writeString(catalogFile, "{not-json");

    assertThatThrownBy(() -> harness.importer.importCatalog(new FileSystemResource(catalogFile)))
        .isInstanceOf(ArtworkCatalogImportException.class)
        .hasMessageContaining("Catalog JSON is malformed");
  }

  @Test
  void missingRequiredAttributeFailsClearly() throws IOException {
    ImportHarness harness = new ImportHarness();
    String invalid = singleArtwork("Abstract", "abstract-one.jpg", "");
    FileSystemResource catalog = catalog(invalid);

    assertThatThrownBy(() -> harness.importer.importCatalog(catalog))
        .isInstanceOf(ArtworkCatalogImportException.class)
        .hasMessageContaining("missing required title");
  }

  @Test
  void inconsistentCategoryCasingFailsClearly() throws IOException {
    ImportHarness harness = new ImportHarness();
    createImage("first.png");
    createImage("second.png");
    String first = artworkObject("Abstract", "one.jpg", "One", "images/first.png");
    String second = artworkObject("abstract", "two.jpg", "Two", "images/second.png");
    Path catalogFile = temporaryDirectory.resolve("artworks.json");
    Files.writeString(catalogFile, "{\"artworks\":[" + first + "," + second + "]}");

    assertThatThrownBy(() -> harness.importer.importCatalog(new FileSystemResource(catalogFile)))
        .isInstanceOf(ArtworkCatalogImportException.class)
        .hasMessageContaining("inconsistent category casing");
  }

  private FileSystemResource catalog(String json) throws IOException {
    Files.createDirectories(temporaryDirectory.resolve("images"));
    createImage("test.png");
    Path catalogFile = temporaryDirectory.resolve("artworks.json");
    Files.writeString(catalogFile, json);
    return new FileSystemResource(catalogFile);
  }

  private void createImage(String name) throws IOException {
    Files.createDirectories(temporaryDirectory.resolve("images"));
    BufferedImage image = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
    ImageIO.write(image, "png", temporaryDirectory.resolve("images").resolve(name).toFile());
  }

  private String singleArtwork(String category, String key, String title) {
    return "{\"artworks\":[" + artworkObject(category, key, title, "images/test.png") + "]}";
  }

  private String artworkObject(String category, String key, String title, String fileName) {
    return "{"
        + "\"title\":\""
        + title
        + "\",\"category\":\""
        + category
        + "\",\"artist\":\"Catalog Artist\","
        + "\"description\":\"Catalog description\",\"fileName\":\""
        + fileName
        + "\",\"logicalFileName\":\""
        + key
        + "\",\"genre\":\"Catalog Genre\","
        + "\"prices\":[{\"type\":\"SELL\",\"amount\":25.00,\"currency\":\"USD\"}]}";
  }

  private static final class ImportHarness {
    private final ArtworkRepository artworkRepository = mock(ArtworkRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final Map<String, ArtEntity> artworks = new HashMap<>();
    private final Map<String, Category> categories = new HashMap<>();
    private final ArtworkCatalogImporter importer;

    private ImportHarness() {
      when(categoryRepository.findByCategoryNameIgnoreCase(any(String.class)))
          .thenAnswer(
              invocation ->
                  Optional.ofNullable(categories.get(normalize(invocation.getArgument(0)))));
      when(categoryRepository.save(any(Category.class)))
          .thenAnswer(
              invocation -> {
                Category category = invocation.getArgument(0);
                categories.put(normalize(category.getCategoryName()), category);
                return category;
              });
      when(artworkRepository.findByCatalogKeyIgnoreCase(any(String.class)))
          .thenAnswer(
              invocation ->
                  Optional.ofNullable(artworks.get(normalize(invocation.getArgument(0)))));
      when(artworkRepository.save(any(ArtEntity.class)))
          .thenAnswer(
              invocation -> {
                ArtEntity artwork = invocation.getArgument(0);
                artworks.put(normalize(artwork.getCatalogKey()), artwork);
                return artwork;
              });
      importer =
          new ArtworkCatalogImporter(
              new ObjectMapper(),
              new DefaultResourceLoader(),
              artworkRepository,
              categoryRepository);
    }

    private static String normalize(String value) {
      return value.toLowerCase(Locale.ROOT);
    }
  }
}
