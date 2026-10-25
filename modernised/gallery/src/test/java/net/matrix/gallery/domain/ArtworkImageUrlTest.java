package net.matrix.gallery.domain;

import static org.assertj.core.api.Assertions.assertThat;

import net.matrix.gallery.domain.value.ArtworkImageUrl;
import org.junit.jupiter.api.Test;

class ArtworkImageUrlTest {

  @Test
  void resolvesLogicalKeyThroughApplicationMediaPathAndEncodesSegments() {
    assertThat(ArtworkImageUrl.fromObjectKey("artworks/images/image one+.png"))
        .isEqualTo("/media/artworks/images/image%20one+.png");
  }

  @Test
  void keepsMissingObjectKeyBehavior() {
    assertThat(ArtworkImageUrl.fromObjectKey(null)).isNull();
    assertThat(ArtworkImageUrl.fromObjectKey(" ")).isNull();
  }
}
