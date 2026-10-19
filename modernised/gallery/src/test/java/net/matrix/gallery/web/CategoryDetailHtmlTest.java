package net.matrix.gallery.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import net.matrix.gallery.domain.value.ArtworkSummary;
import net.matrix.gallery.domain.value.CategoryViewer;
import net.matrix.gallery.domain.value.LightboxArtwork;
import net.matrix.gallery.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryDetailHtmlTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CategoryService categoryService;

  @Test
  void exposesOrderedLightboxArtworkMetadataWithoutChangingVisibleViewerContent() throws Exception {
    var selected =
        new ArtworkSummary(22L, "Selected & Featured", null, "/images/selected-thumbnail.jpg");
    var viewer =
        new CategoryViewer(
            5L,
            "Landscapes",
            "Landscape works",
            List.of(selected),
            List.of(
                new LightboxArtwork(
                    22L,
                    "Selected & Featured",
                    "Ava \"Ace\" & Co.",
                    "/images/original?size=full&crop=none"),
                new LightboxArtwork(7L, "Second <Study>", null, null)),
            selected,
            null,
            null,
            "Ava \"Ace\" & Co.",
            "A featured description.",
            "/images/selected-gallery.jpg",
            "/images/selected-original.jpg");
    when(categoryService.getCategoryViewer(5L)).thenReturn(viewer);

    String html =
        mockMvc
            .perform(get("/categories/5"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String normalizedHtml = html.replaceAll("\\s+", " ");

    String firstArtwork =
        "data-artwork-id=\"22\" data-title=\"Selected &amp; Featured\" "
            + "data-artist=\"Ava &quot;Ace&quot; &amp; Co.\" "
            + "data-image-url=\"/images/original?size=full&amp;crop=none\"";
    String secondArtwork =
        "data-artwork-id=\"7\" data-title=\"Second &lt;Study&gt;\" "
            + "data-artist=\"\" data-image-url=\"\"";

    assertThat(normalizedHtml)
        .contains("<div hidden aria-hidden=\"true\" data-lightbox-artworks>")
        .contains(firstArtwork, secondArtwork)
        .contains("data-selected-artwork-id=\"22\"")
        .contains("<p data-artwork-lightbox-artist")
        .contains("<button type=\"button\" data-artwork-lightbox-previous>Previous</button>")
        .contains("<button type=\"button\" data-artwork-lightbox-next>Next</button>")
        .contains("<h2 class=\"gallery-image-title\">Selected &amp; Featured</h2>")
        .contains("src=\"/images/selected-gallery.jpg\"")
        .contains("href=\"/categories/5/artworks/22\"")
        .doesNotContain("data-title=\"Second <Study>\"");
    assertThat(normalizedHtml.indexOf(firstArtwork))
        .isLessThan(normalizedHtml.indexOf(secondArtwork));
    assertThat(countOccurrences(html, "data-lightbox-artwork\n")).isEqualTo(2);
  }

  private int countOccurrences(String value, String target) {
    return (value.length() - value.replace(target, "").length()) / target.length();
  }
}
