package net.matrix.gallery.service;

/** Resolves a persistent artwork object key to a browser-facing delivery URL. */
public interface ArtworkUrlResolver {

  String resolve(String objectKey);
}
