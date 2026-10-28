package net.matrix.gallery.repository;

import java.util.Optional;
import net.matrix.gallery.domain.model.AdministratorAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministratorAccountRepository extends JpaRepository<AdministratorAccount, Long> {

  Optional<AdministratorAccount> findByUsername(String username);
}
