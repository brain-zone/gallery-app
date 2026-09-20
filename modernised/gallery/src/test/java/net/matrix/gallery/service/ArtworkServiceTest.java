package net.matrix.gallery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import net.matrix.gallery.domain.model.ArtEntity;
import net.matrix.gallery.domain.model.Category;
import net.matrix.gallery.domain.value.ImageRendition;
import net.matrix.gallery.domain.value.RenditionType;
import net.matrix.gallery.repository.ArtworkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArtworkServiceTest {

  @Mock private ArtworkRepository artworkRepository;

  @InjectMocks private ArtworkService artworkService;

  @Test
  void mapsArtworkCategoriesAndRenditionsToDetail() {
    ArtEntity artwork = artwork();
    when(artworkRepository.findDetailById(9L)).thenReturn(Optional.of(artwork));

    var detail = artworkService.getArtwork(9L);

    assertThat(detail.id()).isEqualTo(9L);
    assertThat(detail.title()).isEqualTo("Evening Sky");
    assertThat(detail.subTitle()).isEqualTo("Sunset series");
    assertThat(detail.displayDate()).isEqualTo(LocalDate.of(2026, 1, 15));
    assertThat(detail.width()).isEqualTo(1200);
    assertThat(detail.height()).isEqualTo(800);
    assertThat(detail.media()).isEqualTo("Oil on canvas");
    assertThat(detail.description()).isEqualTo("A warm-toned landscape at dusk.");
    assertThat(detail.caption()).isEqualTo("Serenity at dusk");
    assertThat(detail.generalViewable()).isTrue();
    assertThat(detail.privilegeViewable()).isFalse();
    assertThat(detail.categories())
        .singleElement()
        .satisfies(
            category -> {
              assertThat(category.id()).isEqualTo(5L);
              assertThat(category.categoryName()).isEqualTo("Landscapes");
            });
    assertThat(detail.renditions())
        .singleElement()
        .satisfies(
            rendition -> {
              assertThat(rendition.type()).isEqualTo(RenditionType.GALLERY);
              assertThat(rendition.objectKey()).isEqualTo("gallery/evening-sky.jpg");
              assertThat(rendition.contentType()).isEqualTo("image/jpeg");
              assertThat(rendition.sizeBytes()).isEqualTo(245760L);
              assertThat(rendition.width()).isEqualTo(1200);
              assertThat(rendition.height()).isEqualTo(800);
              assertThat(rendition.checksum()).isEqualTo("checksum");
            });
    verify(artworkRepository).findDetailById(9L);
  }

  @Test
  void missingArtworkRaisesNotFoundException() {
    when(artworkRepository.findDetailById(404L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> artworkService.getArtwork(404L))
        .isInstanceOf(GalleryResourceNotFoundException.class)
        .hasMessage("Artwork 404 was not found");
  }

  private ArtEntity artwork() {
    Category category = new Category();
    ReflectionTestUtils.setField(category, "id", 5L);
    category.setCategoryName("Landscapes");

    ArtEntity artwork = new ArtEntity();
    ReflectionTestUtils.setField(artwork, "id", 9L);
    artwork.setTitle("Evening Sky");
    artwork.setSubTitle("Sunset series");
    artwork.setUploadedDate(Instant.parse("2026-01-01T00:00:00Z"));
    artwork.setDisplayDate(LocalDate.of(2026, 1, 15));
    artwork.setWidth(1200);
    artwork.setHeight(800);
    artwork.setMedia("Oil on canvas");
    artwork.setDescription("A warm-toned landscape at dusk.");
    artwork.setCaption("Serenity at dusk");
    artwork.setGeneralViewable(true);
    artwork.addCategory(category);
    artwork.addImageRendition(
        RenditionType.GALLERY,
        new ImageRendition(
            "gallery/evening-sky.jpg", "image/jpeg", 245760L, 1200, 800, "checksum"));
    return artwork;
  }
}
