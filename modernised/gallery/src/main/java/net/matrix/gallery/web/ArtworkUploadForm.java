package net.matrix.gallery.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

/** Validated administrator artwork-upload fields. */
@Getter
@Setter
public class ArtworkUploadForm {

  @NotBlank
  @Size(max = 255)
  private String title;

  @Size(max = 255)
  private String artist;

  @Size(max = 255)
  private String genre;

  @Size(max = 255)
  private String subTitle;

  @Size(max = 255)
  private String media;

  @Size(max = 4000)
  private String description;

  @Size(max = 255)
  private String caption;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate displayDate;

  @NotNull private Long categoryId;
  @NotNull private MultipartFile image;
}
