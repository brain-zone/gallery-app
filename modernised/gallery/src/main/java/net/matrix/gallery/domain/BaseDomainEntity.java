package net.matrix.gallery.domain;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;

/**
 * An abstraction for domain entities in the gallery context.
 *
 * @author Anand Hemadri
 */
@MappedSuperclass
public abstract class BaseDomainEntity {
  @Version @Getter private Integer version;
}
