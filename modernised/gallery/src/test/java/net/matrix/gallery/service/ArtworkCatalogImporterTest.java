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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.imageio.ImageIO;
import net.matrix.gallery.domain.model.ArtEntity;
import net.matrix.gallery.domain.model.Category;
import net.matrix.gallery.domain.value.RenditionType;
import net.matrix.gallery.repository.ArtworkRepository;
import net.matrix.gallery.repository.CategoryRepository;
import net.matrix.gallery.storage.FileSystemArtworkStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;

class ArtworkCatalogImporterTest {

  @TempDir Path temporaryDirectory;

  @Test
  void normalizesCatalogKeyBeforeLookupSoRepeatedImportRemainsIdempotent() throws IOException {
    ImportHarness harness = new ImportHarness();
    FileSystemResource catalog = catalog(singleArtwork("Abstract", " Abstract-One.JPG ", "One"));

    var first = harness.importer.importCatalog(catalog);
    var second = harness.importer.importCatalog(catalog);

    assertThat(first.artworksCreated()).isOne();
    assertThat(first.artworksUpdated()).isZero();

    assertThat(second.artworksCreated()).isZero();
    assertThat(second.artworksUpdated()).isOne();

    assertThat(harness.artworks).hasSize(1);
    assertThat(harness.artworks).containsKey("abstract-one.jpg");

    ArtEntity artwork = harness.artworks.get("abstract-one.jpg");
    assertThat(artwork.getCatalogKey()).isEqualTo("abstract-one.jpg");
  }

  @Test
  void storesAllArtworkResourceKeysUsingForwardSlashes() throws IOException {
    ImportHarness harness = new ImportHarness();
    FileSystemResource catalog = catalog(singleArtwork("Abstract", "abstract-one.jpg", "One"));

    harness.importer.importCatalog(catalog);

    ArtEntity artwork = harness.artworks.get("abstract-one.jpg");

    assertThat(artwork).isNotNull();
    assertThat(artwork.getStoragePicture().objectKey())
        .isEqualTo("artworks/images/test/original.png")
        .doesNotContain("\\");
    assertThat(artwork.getGalleryPicture().objectKey())
        .isEqualTo("artworks/images/test/gallery.png")
        .doesNotContain("\\");
    assertThat(artwork.getThumbnailPicture().objectKey())
        .isEqualTo("artworks/images/test/thumbnail.png")
        .doesNotContain("\\");
  }

  @Test
  void parsesAndImportsTheBundledCatalogAcrossMultipleCategories() {
    ImportHarness harness = new ImportHarness(Path.of("artwork-media"));

    var result =
        harness.importer.importCatalog(new ClassPathResource("static/artworks/artworks.json"));

    assertThat(result.catalogSize()).isEqualTo(14);
    assertThat(result.categoriesCreated()).isEqualTo(6);
    assertThat(result.artworksCreated()).isEqualTo(14);
    assertThat(harness.categories.keySet())
        .containsExactlyInAnyOrder(
            "abstract", "landscape", "portrait", "sculpture", "urban", "wireframes");
    assertThat(harness.artworks).hasSize(14);
  }

  @Test
  void importsOneArtworkWithMetadataAssociationAndThreeIndependentRenditions()
      throws IOException, NoSuchAlgorithmException {
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
    assertThat(artwork.getImageRendition())
        .containsOnlyKeys(RenditionType.ORIGINAL, RenditionType.GALLERY, RenditionType.THUMBNAIL);
    assertRendition(artwork.getStoragePicture(), "test/original.png", 7, 5);
    assertRendition(artwork.getGalleryPicture(), "test/gallery.png", 5, 3);
    assertRendition(artwork.getThumbnailPicture(), "test/thumbnail.png", 2, 1);
    assertThat(artwork.getWidth()).isEqualTo(7);
    assertThat(artwork.getHeight()).isEqualTo(5);
    assertThat(
            artwork.getImageRendition().values().stream()
                .map(rendition -> rendition.checksum())
                .distinct())
        .hasSize(3);
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
    assertThat(artwork.getImageRendition())
        .containsOnlyKeys(RenditionType.ORIGINAL, RenditionType.GALLERY, RenditionType.THUMBNAIL);
  }

  @Test
  void missingImageFailsClearlyBeforeImport() throws IOException {
    ImportHarness harness = new ImportHarness();
    Files.createDirectories(temporaryDirectory.resolve("artworks/images"));
    Path catalogFile = temporaryDirectory.resolve("artworks.json");
    Files.writeString(catalogFile, singleArtwork("Abstract", "abstract-one.jpg", "One"));

    assertThatThrownBy(() -> harness.importer.importCatalog(new FileSystemResource(catalogFile)))
        .isInstanceOf(ArtworkCatalogImportException.class)
        .hasMessageContaining(
            "refers to missing required ORIGINAL image artworks/images/test/original.png");
    assertThat(harness.artworks).isEmpty();
  }

  @Test
  void missingDerivativeFailsClearlyBeforeImport() throws IOException {
    ImportHarness harness = new ImportHarness();
    Files.createDirectories(temporaryDirectory.resolve("artworks/images"));
    createImage("test/original.png", 7, 5, 0xAA0000);
    Path catalogFile = temporaryDirectory.resolve("artworks.json");
    Files.writeString(catalogFile, singleArtwork("Abstract", "abstract-one.jpg", "One"));

    assertThatThrownBy(() -> harness.importer.importCatalog(new FileSystemResource(catalogFile)))
        .isInstanceOf(ArtworkCatalogImportException.class)
        .hasMessageContaining(
            "refers to missing required GALLERY image artworks/images/test/gallery.png");
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
    createRenditions("first.png");
    createRenditions("second.png");
    String first = artworkObject("Abstract", "one.jpg", "One", "images/first.png");
    String second = artworkObject("abstract", "two.jpg", "Two", "images/second.png");
    Path catalogFile = temporaryDirectory.resolve("artworks.json");
    Files.writeString(catalogFile, "{\"artworks\":[" + first + "," + second + "]}");

    assertThatThrownBy(() -> harness.importer.importCatalog(new FileSystemResource(catalogFile)))
        .isInstanceOf(ArtworkCatalogImportException.class)
        .hasMessageContaining("inconsistent category casing");
  }

  private FileSystemResource catalog(String json) throws IOException {
    Files.createDirectories(temporaryDirectory.resolve("artworks/images"));
    createRenditions("test.png");
    Path catalogFile = temporaryDirectory.resolve("artworks.json");
    Files.writeString(catalogFile, json);
    return new FileSystemResource(catalogFile);
  }

  private void createRenditions(String name) throws IOException {
    int extensionStart = name.lastIndexOf('.');
    String directory = name.substring(0, extensionStart);
    String extension = name.substring(extensionStart);
    createImage(directory + "/original" + extension, 7, 5, 0xAA0000);
    createImage(directory + "/gallery" + extension, 5, 3, 0x00AA00);
    createImage(directory + "/thumbnail" + extension, 2, 1, 0x0000AA);
  }

  private void createImage(String name, int width, int height, int rgb) throws IOException {
    Path imagePath = temporaryDirectory.resolve("artworks/images").resolve(name);
    Files.createDirectories(Objects.requireNonNull(imagePath.getParent()));
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    image.setRGB(0, 0, rgb);
    ImageIO.write(image, "png", imagePath.toFile());
  }

  private void assertRendition(
      net.matrix.gallery.domain.value.ImageRendition rendition,
      String fileName,
      int width,
      int height)
      throws IOException, NoSuchAlgorithmException {
    Path file = temporaryDirectory.resolve("artworks/images").resolve(fileName);
    assertThat(rendition.objectKey()).isEqualTo("artworks/images/" + fileName);
    assertThat(rendition.contentType()).isEqualTo("image/png");
    assertThat(rendition.width()).isEqualTo(width);
    assertThat(rendition.height()).isEqualTo(height);
    assertThat(rendition.sizeBytes()).isEqualTo(Files.size(file));
    assertThat(rendition.checksum())
        .isEqualTo(
            HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))));
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

  private final class ImportHarness {
    private final ArtworkRepository artworkRepository = mock(ArtworkRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final Map<String, ArtEntity> artworks = new HashMap<>();
    private final Map<String, Category> categories = new HashMap<>();
    private final ArtworkCatalogImporter importer;

    private ImportHarness() {
      this(temporaryDirectory);
    }

    private ImportHarness(Path storageRoot) {
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
              categoryRepository,
              new FileSystemArtworkStorage(storageRoot.toString()));
    }

    private static String normalize(String value) {
      return value.toLowerCase(Locale.ROOT);
    }
  }
}
