package net.matrix.gallery.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import net.matrix.gallery.domain.model.ArtEntity;
import net.matrix.gallery.domain.model.Category;
import net.matrix.gallery.domain.value.ImageRendition;
import net.matrix.gallery.domain.value.RenditionType;
import net.matrix.gallery.repository.ArtworkRepository;
import net.matrix.gallery.repository.CategoryRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Validates and imports the curated, bundled artwork catalog. */
@Service
public class ArtworkCatalogImporter {

  static final String BUNDLED_CATALOG = "classpath:/static/artworks/artworks.json";

  private final ObjectMapper objectMapper;
  private final ResourceLoader resourceLoader;
  private final ArtworkRepository artworkRepository;
  private final CategoryRepository categoryRepository;

  @SuppressFBWarnings(
      value = "EI_EXPOSE_REP2",
      justification = "ObjectMapper is an injected, application-scoped Spring dependency")
  public ArtworkCatalogImporter(
      ObjectMapper objectMapper,
      ResourceLoader resourceLoader,
      ArtworkRepository artworkRepository,
      CategoryRepository categoryRepository) {
    this.objectMapper = objectMapper;
    this.resourceLoader = resourceLoader;
    this.artworkRepository = artworkRepository;
    this.categoryRepository = categoryRepository;
  }

  @Transactional
  public CatalogImportResult importBundledCatalog() {
    return importCatalog(resourceLoader.getResource(BUNDLED_CATALOG));
  }

  @Transactional
  public CatalogImportResult importCatalog(Resource catalogResource) {
    List<PreparedArtwork> catalog = readAndValidate(catalogResource);
    Map<String, Category> categories = new HashMap<>();
    int categoriesCreated = 0;
    int artworksCreated = 0;
    int artworksUpdated = 0;

    for (PreparedArtwork source : catalog) {
      String normalizedCategory = normalize(source.category());
      Category category = categories.get(normalizedCategory);
      if (category == null) {
        category = categoryRepository.findByCategoryNameIgnoreCase(source.category()).orElse(null);
        if (category == null) {
          category = new Category();
          category.setCategoryName(source.category());
          category = categoryRepository.save(category);
          categoriesCreated++;
        }
        categories.put(normalizedCategory, category);
      }

      ArtEntity artwork =
          artworkRepository.findByCatalogKeyIgnoreCase(source.catalogKey()).orElse(null);
      if (artwork == null) {
        artwork = new ArtEntity();
        artwork.setCatalogKey(source.catalogKey());
        artworksCreated++;
      } else {
        artworksUpdated++;
      }

      applyMetadata(artwork, source);
      synchronizeCategory(artwork, category);
      artwork.addImageRendition(
          RenditionType.GALLERY,
          new ImageRendition(
              source.objectKey(),
              source.contentType(),
              source.sizeBytes(),
              source.width(),
              source.height(),
              source.checksum()));
      artworkRepository.save(artwork);
    }

    return new CatalogImportResult(
        catalog.size(), categoriesCreated, artworksCreated, artworksUpdated);
  }

  private List<PreparedArtwork> readAndValidate(Resource catalogResource) {
    CatalogDocument document;
    try (InputStream input = catalogResource.getInputStream()) {
      document = objectMapper.readValue(input, CatalogDocument.class);
    } catch (JsonProcessingException exception) {
      throw new ArtworkCatalogImportException(
          "Catalog JSON is malformed: " + catalogResource.getDescription(), exception);
    } catch (IOException exception) {
      throw new ArtworkCatalogImportException(
          "Catalog could not be read: " + catalogResource.getDescription(), exception);
    }

    if (document.artworks() == null || document.artworks().isEmpty()) {
      throw new ArtworkCatalogImportException("Catalog must contain at least one artwork");
    }

    Set<String> catalogKeys = new HashSet<>();
    Set<String> titles = new HashSet<>();
    Map<String, String> categoryNames = new HashMap<>();
    List<PreparedArtwork> prepared = new ArrayList<>();
    for (int index = 0; index < document.artworks().size(); index++) {
      ArtworkSource source = document.artworks().get(index);
      String location = "artworks[" + index + "]";
      validateRequired(source, location);

      String normalizedKey = normalize(source.logicalFileName());
      if (!catalogKeys.add(normalizedKey)) {
        throw new ArtworkCatalogImportException(
            location + " duplicates catalog key " + source.logicalFileName());
      }
      if (!titles.add(normalize(source.title()))) {
        throw new ArtworkCatalogImportException(location + " duplicates title " + source.title());
      }

      String normalizedCategory = normalize(source.category());
      String establishedCategory = categoryNames.putIfAbsent(normalizedCategory, source.category());
      if (establishedCategory != null && !establishedCategory.equals(source.category())) {
        throw new ArtworkCatalogImportException(
            location
                + " uses inconsistent category casing: "
                + establishedCategory
                + " and "
                + source.category());
      }

      validatePrices(source.prices(), location);
      prepared.add(prepareArtwork(catalogResource, source, location));
    }
    return List.copyOf(prepared);
  }

  private PreparedArtwork prepareArtwork(
      Resource catalogResource, ArtworkSource source, String location) {
    Path sourcePath;
    try {
      sourcePath = Path.of(source.fileName()).normalize();
    } catch (RuntimeException exception) {
      throw new ArtworkCatalogImportException(
          location + " has an invalid image path: " + source.fileName(), exception);
    }
    if (sourcePath.isAbsolute()
        || source.fileName().contains("\\")
        || sourcePath.startsWith("..")
        || sourcePath.getNameCount() != 2
        || !"images".equals(sourcePath.getName(0).toString())) {
      throw new ArtworkCatalogImportException(
          location + " image path must be relative to the catalog images directory");
    }

    Resource imageResource;
    try {
      imageResource = catalogResource.createRelative(sourcePath.toString());
    } catch (IOException exception) {
      throw new ArtworkCatalogImportException(
          location + " image path could not be resolved: " + source.fileName(), exception);
    }
    if (!imageResource.exists() || !imageResource.isReadable()) {
      throw new ArtworkCatalogImportException(
          location + " refers to missing image " + source.fileName());
    }

    byte[] imageBytes;
    try (InputStream input = imageResource.getInputStream()) {
      imageBytes = input.readAllBytes();
    } catch (IOException exception) {
      throw new ArtworkCatalogImportException(
          location + " image could not be read: " + source.fileName(), exception);
    }
    BufferedImage image;
    try (MemoryCacheImageInputStream imageInput =
        new MemoryCacheImageInputStream(new ByteArrayInputStream(imageBytes))) {
      Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
      if (!readers.hasNext()) {
        image = null;
      } else {
        ImageReader reader = readers.next();
        try {
          reader.setInput(imageInput, true, true);
          image = reader.read(0);
        } finally {
          reader.dispose();
        }
      }
    } catch (IOException exception) {
      throw new ArtworkCatalogImportException(
          location + " image is invalid: " + source.fileName(), exception);
    }
    if (image == null) {
      throw new ArtworkCatalogImportException(
          location + " is not a supported image: " + source.fileName());
    }

    Path imageFileName = sourcePath.getFileName();
    if (imageFileName == null) {
      throw new ArtworkCatalogImportException(location + " image filename is missing");
    }
    String contentType = contentType(imageFileName.toString(), location);
    return new PreparedArtwork(
        source.logicalFileName(),
        source.title(),
        source.category(),
        source.artist(),
        source.description(),
        source.genre(),
        "artworks/" + sourcePath,
        contentType,
        (long) imageBytes.length,
        image.getWidth(),
        image.getHeight(),
        sha256(imageBytes));
  }

  private void validateRequired(ArtworkSource source, String location) {
    if (source == null) {
      throw new ArtworkCatalogImportException(location + " must be an object");
    }
    requireText(source.title(), "title", location, 255);
    requireText(source.category(), "category", location, 255);
    requireText(source.artist(), "artist", location, 255);
    requireText(source.description(), "description", location, Integer.MAX_VALUE);
    requireText(source.fileName(), "fileName", location, 255);
    requireText(source.logicalFileName(), "logicalFileName", location, 255);
    requireText(source.genre(), "genre", location, 255);
  }

  private void requireText(String value, String field, String location, int maxLength) {
    if (value == null || value.isBlank()) {
      throw new ArtworkCatalogImportException(location + " is missing required " + field);
    }
    if (value.length() > maxLength) {
      throw new ArtworkCatalogImportException(location + " " + field + " exceeds " + maxLength);
    }
  }

  private void validatePrices(List<PriceSource> prices, String location) {
    if (prices == null || prices.isEmpty()) {
      throw new ArtworkCatalogImportException(location + " is missing required prices");
    }
    Set<String> priceTypes = new HashSet<>();
    String currency = null;
    for (PriceSource price : prices) {
      if (price == null
          || price.type() == null
          || price.type().isBlank()
          || price.amount() == null
          || price.currency() == null
          || price.currency().isBlank()) {
        throw new ArtworkCatalogImportException(location + " contains an incomplete price");
      }
      if (!priceTypes.add(normalize(price.type()))) {
        throw new ArtworkCatalogImportException(
            location + " contains duplicate price type " + price.type());
      }
      if (currency == null) {
        currency = price.currency();
      } else if (!currency.equalsIgnoreCase(price.currency())) {
        throw new ArtworkCatalogImportException(location + " contains inconsistent currencies");
      }
    }
  }

  private void applyMetadata(ArtEntity artwork, PreparedArtwork source) {
    artwork.setCatalogKey(source.catalogKey());
    artwork.setTitle(source.title());
    artwork.setArtist(source.artist());
    artwork.setGenre(source.genre());
    artwork.setSubTitle(null);
    artwork.setDescription(source.description());
    artwork.setMedia(null);
    artwork.setWidth(source.width());
    artwork.setHeight(source.height());
  }

  private void synchronizeCategory(ArtEntity artwork, Category requiredCategory) {
    for (Category existing : new HashSet<>(artwork.getCategories())) {
      if (!existing.getCategoryName().equalsIgnoreCase(requiredCategory.getCategoryName())) {
        artwork.removeCategory(existing);
      }
    }
    boolean alreadyAssociated =
        artwork.getCategories().stream()
            .anyMatch(
                category ->
                    category
                        .getCategoryName()
                        .equalsIgnoreCase(requiredCategory.getCategoryName()));
    if (!alreadyAssociated) {
      artwork.addCategory(requiredCategory);
    }
  }

  private String contentType(String fileName, String location) {
    String lowerName = fileName.toLowerCase(Locale.ROOT);
    if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
      return "image/jpeg";
    }
    if (lowerName.endsWith(".png")) {
      return "image/png";
    }
    throw new ArtworkCatalogImportException(
        location + " uses an unsupported image extension: " + fileName);
  }

  private String sha256(byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }

  private String normalize(String value) {
    return value.strip().toLowerCase(Locale.ROOT);
  }

  private record CatalogDocument(List<ArtworkSource> artworks) {}

  private record ArtworkSource(
      String title,
      String category,
      String artist,
      String description,
      String fileName,
      String logicalFileName,
      String genre,
      List<PriceSource> prices) {}

  private record PriceSource(String type, BigDecimal amount, String currency) {}

  private record PreparedArtwork(
      String catalogKey,
      String title,
      String category,
      String artist,
      String description,
      String genre,
      String objectKey,
      String contentType,
      Long sizeBytes,
      Integer width,
      Integer height,
      String checksum) {}

  public record CatalogImportResult(
      int catalogSize, int categoriesCreated, int artworksCreated, int artworksUpdated) {}
}
