ALTER TABLE art_entity ADD COLUMN catalog_key VARCHAR(255);
ALTER TABLE art_entity ADD COLUMN artist VARCHAR(255);
ALTER TABLE art_entity ADD COLUMN genre VARCHAR(255);

-- Catalog keys are normalized to lower case by the domain model before persistence.
CREATE UNIQUE INDEX uk_art_entity_catalog_key ON art_entity (catalog_key);

-- The curated catalog supersedes the synthetic browsing-milestone record from V3.
-- Match its unique seed rendition as well as its metadata so an unrelated artwork
-- with the same title cannot be removed. Child rows use ON DELETE CASCADE.
DELETE FROM art_entity
WHERE id IN (
    SELECT artwork.id
    FROM art_entity artwork
    JOIN art_entity_rendition rendition ON rendition.art_entity_id = artwork.id
    WHERE artwork.catalog_key IS NULL
      AND artwork.title = 'Evening Sky'
      AND artwork.sub_title = 'Sunset series'
      AND artwork.description = 'A warm-toned landscape at dusk.'
      AND rendition.rendition_type = 'GALLERY'
      AND rendition.checksum = 'seed-evening-sky-gallery'
);
