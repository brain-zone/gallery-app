package net.matrix.gallery.repository;

import java.util.Optional;
import net.matrix.gallery.domain.model.ArtEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtworkRepository extends JpaRepository<ArtEntity, Long> {

  @EntityGraph(attributePaths = {"categories", "imageRendition"})
  @Query("SELECT DISTINCT artwork FROM ArtEntity artwork WHERE artwork.id = :id")
  Optional<ArtEntity> findDetailById(@Param("id") Long id);
}
