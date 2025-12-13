package net.matrix.gallery.domain;

import java.util.Collections;
import java.util.HashSet;
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
public class Category implements BaseDomainEntity {
  @Getter
  @Setter(AccessLevel.PACKAGE)
  Long id;

  @Getter @Setter private String categoryName;

  @Getter @Setter private String categoryDescription;

  private Set<ArtEntity> artEntities = new HashSet<>();

  public void addArtToCategory(ArtEntity art) {
    this.artEntities.add(art);
  }

  public Set<ArtEntity> getArtEntities() {
    return Collections.unmodifiableSet(artEntities);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Category category = (Category) o;

    if (id == null || category.id == null) {
      return false;
    }
    if (!id.equals(category.id)) {
      return false;
    }
    return categoryName != null
        ? categoryName.equals(category.categoryName)
        : category.categoryName == null;
  }

  @Override
  public int hashCode() {
    int result = (id != null ? id.hashCode() : 0);
    result = 31 * result + (categoryName != null ? categoryName.hashCode() : 0);
    return result;
  }
}
