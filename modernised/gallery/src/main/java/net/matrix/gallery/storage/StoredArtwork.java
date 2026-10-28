package net.matrix.gallery.storage;

import org.springframework.core.io.Resource;

/** A readable artwork object and the HTTP metadata derived from its stored file. */
public record StoredArtwork(Resource resource, String contentType, long contentLength) {}
