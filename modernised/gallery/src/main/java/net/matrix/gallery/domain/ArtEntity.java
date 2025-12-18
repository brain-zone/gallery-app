package net.matrix.gallery.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OneToMany;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Aggregate root describing an artwork, its metadata, and externalized renditions stored in object
 * storage.
 *
 * @author Anand Hemadri
 */
@Entity
public class ArtEntity extends BaseDomainEntity {
  @Getter
  @Setter(AccessLevel.PACKAGE)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
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

  @ElementCollection
  @CollectionTable(name = "art_entity_rendition", joinColumns = @JoinColumn(name = "art_entity_id"))
  @MapKeyEnumerated(EnumType.STRING)
  @MapKeyColumn(name = "rendition_type")
  private Map<RenditionType, ImageRendition> imageRendition = new EnumMap<>(RenditionType.class);

  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "art_entity_category",
      joinColumns = @JoinColumn(name = "art_entity_id"),
      inverseJoinColumns = @JoinColumn(name = "category_id"))
  private Set<Category> categories = new HashSet<>();

  @OneToMany(
      mappedBy = "commentedArt",
      fetch = FetchType.LAZY,
      cascade = {CascadeType.ALL},
      orphanRemoval = true)
  private Set<Comment> comments = new HashSet<>();

  @Getter @Setter private boolean generalViewable;

  @Getter @Setter
  private boolean
      privilegeViewable; // can be seen by logged-in non-administrators (spcial visitors)

  public ArtEntity() {}

  /** Thumbnail-sized rendition, if present. */
  public ArtRendition getThumbnailPicture() {
    return imageRendition.get(RenditionType.THUMBNAIL);
  }

  /** Gallery-sized rendition, if present. */
  public ArtRendition getGalleryPicture() {
    return imageRendition.get(RenditionType.GALLERY);
  }

  /** Original or storage rendition, if present. */
  public ArtRendition getStoragePicture() {
    return imageRendition.get(RenditionType.ORIGINAL);
  }

  /** Read-only view of available renditions. */
  public Map<RenditionType, ImageRendition> getImageRendition() {
    return Collections.unmodifiableMap(imageRendition);
  }

  /**
   * Associates a rendition with a given type.
   *
   * @param type the rendition slot (ORIGINAL/GALLERY/THUMBNAIL)
   * @param rendition metadata pointing to the stored object
   */
  public void addImageRendition(RenditionType type, ImageRendition rendition) {
    this.imageRendition.put(type, rendition);
  }

  /**
   * Removes the rendition for the given type.
   *
   * @param type rendition slot to remove
   */
  public void removeImageRendition(RenditionType type) {
    this.imageRendition.remove(type);
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
    category.addArtToCategory(this);
  }

  /**
   * Removes a category link.
   *
   * @param category category to detach
   */
  public void removeCategory(Category category) {
    this.categories.remove(category);
    category.removeArtFromCategory(this);
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
    comment.setCommentedArt(this);
  }

  /**
   * Removes a comment association.
   *
   * @param comment comment to detach
   */
  public void removeComment(Comment comment) {
    this.comments.remove(comment);
    comment.setCommentedArt(null);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ArtEntity)) return false;
    ArtEntity artEntity = (ArtEntity) o;
    return id != null && Objects.equals(id, artEntity.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
