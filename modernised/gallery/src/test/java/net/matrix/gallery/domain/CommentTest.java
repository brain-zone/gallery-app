package net.matrix.gallery.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Tests for Comment domain behavior.
 *
 * @author Anand Hemadri
 */
class CommentTest {

  @Test
  void settersAndGettersRetainValues() {
    Comment comment = new Comment();
    ArtEntity art = new ArtEntity();
    Instant now = Instant.now();

    comment.id = 10L;
    comment.setComment("A thoughtful critique");
    comment.setCommentedArt(art);
    comment.setCommentedOn(now);
    comment.setFirstName("Ada");
    comment.setLastName("Lovelace");
    comment.setEmailAddress("ada@example.com");
    comment.setTelephone("123-456-7890");
    comment.setVersion(1);

    assertEquals(10L, comment.getId());
    assertEquals("A thoughtful critique", comment.getComment());
    assertEquals(art, comment.getCommentedArt());
    assertEquals(now, comment.getCommentedOn());
    assertEquals("Ada", comment.getFirstName());
    assertEquals("Lovelace", comment.getLastName());
    assertEquals("ada@example.com", comment.getEmailAddress());
    assertEquals("123-456-7890", comment.getTelephone());
    assertEquals(1, comment.getVersion());
  }
}
