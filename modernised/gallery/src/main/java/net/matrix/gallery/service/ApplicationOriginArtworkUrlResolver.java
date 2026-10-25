package net.matrix.gallery.service;

import net.matrix.gallery.domain.value.ArtworkImageUrl;
import org.springframework.stereotype.Component;

/** Delivers artwork through this application's public media endpoint. */
@Component
public class ApplicationOriginArtworkUrlResolver implements ArtworkUrlResolver {

  @Override
  public String resolve(String objectKey) {
    return ArtworkImageUrl.fromObjectKey(objectKey);
  }
}
