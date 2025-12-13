package net.matrix.gallery.domain;

/**
 * Immutable reference to an artwork rendition stored in object storage.
 *
 * @param type the rendition slot (ORIGINAL/GALLERY/THUMBNAIL)
 * @param objectKey object storage key/path
 * @param contentType MIME type of the stored object
 * @param sizeBytes size of the stored object in bytes
 * @param width pixel width, if known
 * @param height pixel height, if known
 * @param checksum checksum for integrity validation
 * @author Anand Hemadri
 */
public record ImageRendition(
    RenditionType type,
    String objectKey,
    String contentType,
    Long sizeBytes,
    Integer width,
    Integer height,
    String checksum)
    implements ArtRendition {}
