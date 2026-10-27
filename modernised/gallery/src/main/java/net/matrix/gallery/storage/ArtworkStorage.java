package net.matrix.gallery.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/** Retrieves artwork media by its persistent logical object key. */
public interface ArtworkStorage {

  Optional<StoredArtwork> read(String objectKey) throws IOException;

  /** Atomically stores all objects in a new logical artwork directory. */
  void writeDirectory(String directoryKey, Map<String, Path> sourceObjects) throws IOException;

  /** Removes one artwork directory and all objects within it. */
  void deleteDirectory(String directoryKey) throws IOException;
}
