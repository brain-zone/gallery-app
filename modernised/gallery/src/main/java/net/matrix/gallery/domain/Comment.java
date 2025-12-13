package net.matrix.gallery.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Visitor comment attached to an artwork.
 *
 * @author Anand Hemadri
 */
@SuppressFBWarnings(
    value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
    justification = "Domain association intentionally exposed")
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Comment {
  @Getter
  @Setter(AccessLevel.PACKAGE)
  Long id;

  @Getter @Setter private String comment;
  @Getter @Setter private ArtEntity commentedArt;
  @Setter private Instant commentedOn;
  @Getter @Setter private String firstName;
  @Getter @Setter private String lastName;
  @Getter @Setter private String emailAddress;
  @Getter @Setter private String telephone;

  @Getter @Setter private Integer version;

  public Instant getCommentedOn() {
    return commentedOn;
  }
}
