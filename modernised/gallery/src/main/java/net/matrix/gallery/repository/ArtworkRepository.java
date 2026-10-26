package net.matrix.gallery.repository;

import java.util.Optional;
import net.matrix.gallery.domain.model.ArtEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtworkRepository extends JpaRepository<ArtEntity, Long> {

  Optional<ArtEntity> findByCatalogKeyIgnoreCase(String catalogKey);

  @EntityGraph(attributePaths = {"categories", "imageRendition"})
  @Query("SELECT DISTINCT artwork FROM ArtEntity artwork WHERE artwork.id = :id")
  Optional<ArtEntity> findDetailById(@Param("id") Long id);

  @EntityGraph(attributePaths = {"categories", "imageRendition"})
  @Query(
      "SELECT DISTINCT artwork FROM ArtEntity artwork WHERE artwork.id = :id AND artwork.generalViewable = true")
  Optional<ArtEntity> findPublicDetailById(@Param("id") Long id);

  @Query(
      "SELECT CASE WHEN count(artwork) > 0 THEN true ELSE false END FROM ArtEntity artwork JOIN artwork.imageRendition rendition WHERE artwork.generalViewable = true AND rendition.objectKey = :objectKey")
  boolean existsPublicByRenditionObjectKey(@Param("objectKey") String objectKey);
}
