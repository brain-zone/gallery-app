INSERT INTO art_entity
    (title, sub_title, uploaded_date, display_date, width, height, media, description, caption,
     general_viewable, privilege_viewable)
VALUES
    ('Evening Sky', 'Sunset series', CURRENT_TIMESTAMP, DATE '2026-01-15', 1200, 800,
     'Oil on canvas', 'A warm-toned landscape at dusk.', 'Serenity at dusk', TRUE, FALSE);

INSERT INTO art_entity_category (art_entity_id, category_id)
SELECT artwork.id, category.id
FROM art_entity artwork
JOIN category ON category.category_name = 'Landscape'
WHERE artwork.title = 'Evening Sky';

INSERT INTO art_entity_rendition
    (art_entity_id, rendition_type, object_key, content_type, size_bytes, width, height, checksum)
SELECT artwork.id, 'GALLERY', 'gallery/evening-sky.jpg', 'image/jpeg', 245760, 1200, 800,
       'seed-evening-sky-gallery'
FROM art_entity artwork
WHERE artwork.title = 'Evening Sky';

INSERT INTO art_entity_rendition
    (art_entity_id, rendition_type, object_key, content_type, size_bytes, width, height, checksum)
SELECT artwork.id, 'THUMBNAIL', 'gallery/evening-sky-thumbnail.jpg', 'image/jpeg', 16384, 240, 160,
       'seed-evening-sky-thumbnail'
FROM art_entity artwork
WHERE artwork.title = 'Evening Sky';
