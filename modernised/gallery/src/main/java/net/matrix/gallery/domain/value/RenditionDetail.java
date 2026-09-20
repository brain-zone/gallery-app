package net.matrix.gallery.domain.value;

/** Public metadata describing one stored image rendition. */
public record RenditionDetail(
    RenditionType type,
    String objectKey,
    String contentType,
    Long sizeBytes,
    Integer width,
    Integer height,
    String checksum) {}
