package net.matrix.gallery.web;

import java.io.IOException;
import net.matrix.gallery.repository.ArtworkRepository;
import net.matrix.gallery.storage.ArtworkStorage;
import net.matrix.gallery.storage.InvalidArtworkObjectKeyException;
import net.matrix.gallery.storage.StoredArtwork;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Serves externally stored artwork through stable application-origin URLs. */
@RestController
public class ArtworkMediaController {

  private final ArtworkStorage artworkStorage;
  private final ArtworkRepository artworkRepository;

  public ArtworkMediaController(
      ArtworkStorage artworkStorage, ArtworkRepository artworkRepository) {
    this.artworkStorage = artworkStorage;
    this.artworkRepository = artworkRepository;
  }

  @GetMapping("/media/{*objectKey}")
  public ResponseEntity<Resource> get(@PathVariable String objectKey) {
    String normalizedKey = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
    try {
      if (!artworkRepository.existsPublicByRenditionObjectKey(normalizedKey)) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
      }
      StoredArtwork artwork =
          artworkStorage
              .read(normalizedKey)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
      return ResponseEntity.ok()
          .contentType(MediaType.parseMediaType(artwork.contentType()))
          .contentLength(artwork.contentLength())
          .cacheControl(CacheControl.noCache())
          .body(artwork.resource());
    } catch (InvalidArtworkObjectKeyException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsafe artwork object key");
    } catch (IOException exception) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Artwork could not be read");
    }
  }
}
