package net.matrix.gallery.config;

public class GallerySecuredPaths {
  static final String[] PUBLIC_GET_PATHS = {
    "/",
    "/login",
    "/bio",
    "/categories",
    "/categories/**",
    "/artworks/**",
    "/api/categories",
    "/api/categories/**",
    "/api/artworks/**"
  };

  static final String[] PUBLIC_ASSET_PATHS = {
    "/css/**", "/images/**", "/js/**", "/artworks/images/**"
  };
}
