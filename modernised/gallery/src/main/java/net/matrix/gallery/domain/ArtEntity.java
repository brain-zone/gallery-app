package net.matrix.gallery.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Aggregate root describing an artwork, its metadata, and externalized renditions stored in object
 * storage.
 *
 * @author Anand Hemadri
 */
public class ArtEntity implements BaseDomainEntity {
  @Getter
  @Setter(AccessLevel.PACKAGE)
  Long id;

  @Getter @Setter private String title;

  @Getter @Setter private String subTitle;

  @Getter @Setter private Instant uploadedDate;

  @Getter @Setter private LocalDate displayDate;

  @Getter @Setter private int width;

  @Getter @Setter private int height;

  @Getter @Setter private String media;

  @Getter @Setter private String description;

  @Getter @Setter private String caption;

  private Map<RenditionType, ArtRendition> renditions = new EnumMap<>(RenditionType.class);
  private Set<Category> categories = new HashSet<>();
  private Set<Comment> comments = new HashSet<>();

  @Setter private boolean generalViewable;

  @Setter
  private boolean
      privilegeViewable; // can be seen by logged-in non-administrators (spcial visitors)

  public ArtEntity() {}

  /** Thumbnail-sized rendition, if present. */
  public ArtRendition getThumbnailPicture() {
    return renditions.get(RenditionType.THUMBNAIL);
  }

  /** Gallery-sized rendition, if present. */
  public ArtRendition getGalleryPicture() {
    return renditions.get(RenditionType.GALLERY);
  }

  /** Original or storage rendition, if present. */
  public ArtRendition getStoragePicture() {
    return renditions.get(RenditionType.ORIGINAL);
  }

  /** Read-only view of available renditions. */
  public Map<RenditionType, ArtRendition> getRenditions() {
    return Collections.unmodifiableMap(renditions);
  }

  /**
   * Associates a rendition with a given type.
   *
   * @param type the rendition slot (ORIGINAL/GALLERY/THUMBNAIL)
   * @param rendition metadata pointing to the stored object
   */
  public void addImageRendition(RenditionType type, ArtRendition rendition) {
    this.renditions.put(type, rendition);
  }

  /**
   * Removes the rendition for the given type.
   *
   * @param type rendition slot to remove
   */
  public void removeImageRendition(RenditionType type) {
    this.renditions.remove(type);
  }

  /** Categories this art is associated with. */
  public Set<Category> getCategories() {
    return Collections.unmodifiableSet(categories);
  }

  /**
   * Adds a category link.
   *
   * @param category category to associate
   */
  public void addCategory(Category category) {
    this.categories.add(category);
  }

  /**
   * Removes a category link.
   *
   * @param category category to detach
   */
  public void removeCategory(Category category) {
    this.categories.remove(category);
  }

  /** Comments posted for this art. */
  public Set<Comment> getComments() {
    return Collections.unmodifiableSet(comments);
  }

  /**
   * Adds a comment association.
   *
   * @param comment comment to attach
   */
  public void addComment(Comment comment) {
    this.comments.add(comment);
  }

  /**
   * Removes a comment association.
   *
   * @param comment comment to detach
   */
  public void removeComment(Comment comment) {
    this.comments.remove(comment);
  }

  public boolean isGeneralViewable() {
    return generalViewable;
  }

  public boolean isPrivilegeViewable() {
    return privilegeViewable;
  }

  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    ArtEntity other = (ArtEntity) obj;
    return id != null && id.equals(other.id);
  }

  public int hashCode() {
    return 31 + (id == null ? 0 : id.hashCode());
  }
}
