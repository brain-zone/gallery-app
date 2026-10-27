package net.matrix.gallery.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import javax.imageio.ImageIO;
import net.matrix.gallery.domain.model.ArtEntity;
import net.matrix.gallery.domain.value.RenditionType;
import net.matrix.gallery.repository.ArtworkRepository;
import net.matrix.gallery.repository.CategoryRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "GALLERY_ADMIN_BOOTSTRAP_PASSWORD=upload-test-password")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdministratorArtworkUploadIntegrationTest {

  private static final Path STORAGE_ROOT = prepareStorage();

  @Autowired private MockMvc mockMvc;
  @Autowired private ArtworkRepository artworkRepository;
  @Autowired private CategoryRepository categoryRepository;

  static Path prepareStorage() {
    try {
      Path storageRoot = Files.createTempDirectory("gallery-upload-integration-");
      Path curatedRoot = Path.of("artwork-media");
      try (var paths = Files.walk(curatedRoot)) {
        for (Path source : paths.toList()) {
          Path target = storageRoot.resolve(curatedRoot.relativize(source).toString());
          if (Files.isDirectory(source)) Files.createDirectories(target);
          else Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
      }
      return storageRoot;
    } catch (IOException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  @DynamicPropertySource
  static void configureStorage(DynamicPropertyRegistry registry) {
    registry.add("gallery.artwork.storage.root", () -> STORAGE_ROOT.toString());
  }

  @Test
  @Order(1)
  void uploadRoutesRequireAdministratorAndCsrf() throws Exception {
    mockMvc.perform(get("/admin/artworks/upload")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/admin/artworks/upload").with(user("visitor").roles("USER")))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/admin/artworks/upload").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Create an Exhibition")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Upload Artwork")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Log Out")));
    mockMvc
        .perform(
            multipart("/admin/artworks/upload")
                .file(image("art.jpg", "jpeg"))
                .param("title", "Protected")
                .param("categoryId", firstCategoryId())
                .with(user("admin").roles("ADMIN")))
        .andExpect(status().isForbidden());
  }

  @Test
  @Order(2)
  void validJpegIsPublishedWithThreeRenditionsAndPublicPagesAndMedia() throws Exception {
    assertThat(artworkRepository.count()).isEqualTo(14);
    mockMvc
        .perform(
            multipart("/admin/artworks/upload")
                .file(image("new-work.jpeg", "jpeg"))
                .param("title", "New Public Work")
                .param("artist", "Test Artist")
                .param("description", "Uploaded description")
                .param("categoryId", firstCategoryId())
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("/artworks/*"));

    ArtEntity saved =
        artworkRepository.findAll().stream()
            .filter(candidate -> "New Public Work".equals(candidate.getTitle()))
            .findFirst()
            .orElseThrow();
    ArtEntity artwork = artworkRepository.findDetailById(saved.getId()).orElseThrow();
    assertThat(artwork.isGeneralViewable()).isTrue();
    assertThat(artwork.isPrivilegeViewable()).isFalse();
    assertThat(artwork.getCategories()).hasSize(1);
    assertThat(artwork.getImageRendition()).containsOnlyKeys(RenditionType.values());
    assertThat(artwork.getStoragePicture().width()).isEqualTo(1600);
    assertThat(artwork.getGalleryPicture().width()).isEqualTo(800);
    assertThat(artwork.getThumbnailPicture().width()).isEqualTo(96);

    mockMvc
        .perform(get("/artworks/{id}", artwork.getId()))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("New Public Work")));
    mockMvc
        .perform(get("/categories/{id}", artwork.getCategories().iterator().next().getId()))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("New Public Work")));
    mockMvc
        .perform(get("/media/{key}", artwork.getStoragePicture().objectKey()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG))
        .andExpect(content().bytes(imageBytes("jpeg")));
    assertThat(artworkRepository.count()).isEqualTo(15);
  }

  @Test
  @Order(3)
  void validPngIsAcceptedAndInvalidInputsLeaveNoRecords() throws Exception {
    long before = artworkRepository.count();
    mockMvc
        .perform(
            multipart("/admin/artworks/upload")
                .file(image("transparent.png", "png"))
                .param("title", "PNG Work")
                .param("categoryId", firstCategoryId())
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().is3xxRedirection());
    assertThat(artworkRepository.count()).isEqualTo(before + 1);

    MockMultipartFile corrupt =
        new MockMultipartFile(
            "image", "corrupt.jpg", "image/jpeg", "not-an-image".getBytes(StandardCharsets.UTF_8));
    mockMvc
        .perform(
            multipart("/admin/artworks/upload")
                .file(corrupt)
                .param("title", "Corrupt")
                .param("categoryId", firstCategoryId())
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("admin-artwork-upload"));

    mockMvc
        .perform(
            multipart("/admin/artworks/upload")
                .file(image("missing-category.jpg", "jpeg"))
                .param("title", "Missing Category")
                .param("categoryId", "999999")
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("existing category")));
    assertThat(artworkRepository.count()).isEqualTo(before + 1);
  }

  @Test
  @Order(4)
  void oversizedImageFailsWithoutPersistence() throws Exception {
    long before = artworkRepository.count();
    MockMultipartFile oversized =
        new MockMultipartFile("image", "large.jpg", "image/jpeg", new byte[10 * 1024 * 1024 + 1]);
    mockMvc
        .perform(
            multipart("/admin/artworks/upload")
                .file(oversized)
                .param("title", "Too Large")
                .param("categoryId", firstCategoryId())
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("10 MB")));
    assertThat(artworkRepository.count()).isEqualTo(before);
  }

  private String firstCategoryId() {
    return categoryRepository.findAll().stream()
        .min(Comparator.comparing(category -> category.getCategoryName()))
        .orElseThrow()
        .getId()
        .toString();
  }

  private MockMultipartFile image(String name, String format) throws IOException {
    String contentType = "png".equals(format) ? "image/png" : "image/jpeg";
    return new MockMultipartFile("image", name, contentType, imageBytes(format));
  }

  private byte[] imageBytes(String format) throws IOException {
    BufferedImage image = new BufferedImage(1600, 900, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    try {
      graphics.setColor(new Color(30, 80, 130));
      graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    } finally {
      graphics.dispose();
    }
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(image, format, output);
    return output.toByteArray();
  }
}
