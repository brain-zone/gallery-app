package net.matrix.gallery.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Visitor comment attached to an artwork.
 *
 * @author Anand Hemadri
 */
@NoArgsConstructor
@Entity
public class Comment extends BaseDomainEntity {
  @Getter
  @Setter(AccessLevel.PACKAGE)
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Getter @Setter private String comment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "art_entity_id", nullable = false)
  @Getter(AccessLevel.PACKAGE)
  @Setter(AccessLevel.PACKAGE)
  private ArtEntity commentedArt;

  @CreationTimestamp
  @Getter
  @Setter(AccessLevel.PACKAGE)
  private Instant commentedOn;

  @Getter @Setter private String firstName;
  @Getter @Setter private String lastName;
  @Getter @Setter private String emailAddress;
  @Getter @Setter private String telephone;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Comment)) return false;
    Comment comment = (Comment) o;
    return id != null && Objects.equals(id, comment.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
