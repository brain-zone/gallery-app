-- Index for faster comment lookups by art entity
CREATE INDEX idx_comment_art_entity_id ON comment (art_entity_id);

-- Index for faster art lookups by category
-- The primary key on art_entity_category already covers (art_entity_id, category_id),
-- but an index on just category_id is useful for finding all art in a category.
CREATE INDEX idx_art_entity_category_category_id ON art_entity_category (category_id);

-- Index for searching art by title
CREATE INDEX idx_art_entity_title ON art_entity (title);
