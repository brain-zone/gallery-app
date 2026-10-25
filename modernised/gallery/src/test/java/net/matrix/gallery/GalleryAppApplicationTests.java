package net.matrix.gallery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "GALLERY_ADMIN_BOOTSTRAP_PASSWORD=test-bootstrap-password")
class GalleryAppApplicationTests {

  @Test
  void contextLoads() {}
}
