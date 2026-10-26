ALTER TABLE art_entity ALTER COLUMN general_viewable SET DEFAULT TRUE;

-- Correct the old default once for curated catalog rows. Later explicit visibility
-- changes remain untouched because this migration is not rerun during startup imports.
UPDATE art_entity
SET general_viewable = TRUE
WHERE catalog_key IS NOT NULL;
