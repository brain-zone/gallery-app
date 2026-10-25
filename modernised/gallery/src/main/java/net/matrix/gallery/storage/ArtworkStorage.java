package net.matrix.gallery.storage;

import java.io.IOException;
import java.util.Optional;

/** Retrieves artwork media by its persistent logical object key. */
public interface ArtworkStorage {

  Optional<StoredArtwork> read(String objectKey) throws IOException;
}
