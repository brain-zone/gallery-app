package net.matrix.gallery.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Persisted administrator identity used by Spring Security. */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class AdministratorAccount extends BaseDomainEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Getter
  private Long id;

  @Column(nullable = false, unique = true)
  @Getter
  private String username;

  @Column(nullable = false)
  @Getter
  private String password;

  @Column(nullable = false)
  @Getter
  private String authority;

  public AdministratorAccount(String username, String password, String authority) {
    this.username = Objects.requireNonNull(username);
    this.password = Objects.requireNonNull(password);
    this.authority = Objects.requireNonNull(authority);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof AdministratorAccount account)) return false;
    return id != null && Objects.equals(id, account.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
