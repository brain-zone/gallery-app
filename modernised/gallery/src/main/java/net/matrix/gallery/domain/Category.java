package net.matrix.gallery.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Groups artwork under a named category.
 *
 * @author Anand Hemadri
 */
@NoArgsConstructor
@Entity
public class Category extends BaseDomainEntity {
  @Getter
  @Setter(AccessLevel.PACKAGE)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Getter @Setter private String categoryName;

  @Getter @Setter private String categoryDescription;

  @ManyToMany(mappedBy = "categories", fetch = FetchType.LAZY)
  private Set<ArtEntity> artEntities = new HashSet<>();

  void addArtToCategory(ArtEntity art) {
    this.artEntities.add(art);
  }

  void removeArtFromCategory(ArtEntity art) {
    this.artEntities.remove(art);
  }

  public Set<ArtEntity> getArtEntities() {
    return Collections.unmodifiableSet(artEntities);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Category)) return false;
    Category category = (Category) o;
    return id != null && Objects.equals(id, category.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
