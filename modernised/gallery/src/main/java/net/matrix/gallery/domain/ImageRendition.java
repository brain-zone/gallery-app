package net.matrix.gallery.domain;

import jakarta.persistence.Embeddable;

/**
 * Immutable reference to an artwork rendition stored in object storage.
 *
 * @param objectKey object storage key/path
 * @param contentType MIME type of the stored object
 * @param sizeBytes size of the stored object in bytes
 * @param width pixel width, if known
 * @param height pixel height, if known
 * @param checksum checksum for integrity validation
 * @author Anand Hemadri
 */
@Embeddable
public record ImageRendition(
    String objectKey,
    String contentType,
    Long sizeBytes,
    Integer width,
    Integer height,
    String checksum)
    implements ArtRendition {}
