package net.matrix.gallery.service;

import net.matrix.gallery.service.ArtworkCatalogImporter.CatalogImportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Runs the deterministic bundled catalog import once during application startup. */
@Component
public class ArtworkCatalogStartup implements ApplicationRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(ArtworkCatalogStartup.class);

  private final ArtworkCatalogImporter importer;

  public ArtworkCatalogStartup(ArtworkCatalogImporter importer) {
    this.importer = importer;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    CatalogImportResult result = importer.importBundledCatalog();
    LOGGER.info(
        "Curated catalog ready: {} records, {} categories created, {} artworks created, {} artworks updated",
        result.catalogSize(),
        result.categoriesCreated(),
        result.artworksCreated(),
        result.artworksUpdated());
  }
}
