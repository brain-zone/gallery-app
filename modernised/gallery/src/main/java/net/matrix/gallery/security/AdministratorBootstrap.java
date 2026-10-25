package net.matrix.gallery.security;

import net.matrix.gallery.domain.model.AdministratorAccount;
import net.matrix.gallery.repository.AdministratorAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the initial administrator without changing an existing credential. */
@Component
public class AdministratorBootstrap implements ApplicationRunner {

  public static final String ADMIN_USERNAME = "admin";
  public static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

  private final AdministratorAccountRepository accountRepository;
  private final PasswordEncoder passwordEncoder;
  private final String bootstrapPassword;

  public AdministratorBootstrap(
      AdministratorAccountRepository accountRepository,
      PasswordEncoder passwordEncoder,
      @Value("${GALLERY_ADMIN_BOOTSTRAP_PASSWORD:}") String bootstrapPassword) {
    this.accountRepository = accountRepository;
    this.passwordEncoder = passwordEncoder;
    this.bootstrapPassword = bootstrapPassword;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments arguments) {
    if (accountRepository.findByUsername(ADMIN_USERNAME).isPresent()) {
      return;
    }

    if (bootstrapPassword == null || bootstrapPassword.isBlank()) {
      throw new IllegalStateException(
          "GALLERY_ADMIN_BOOTSTRAP_PASSWORD must be set when the administrator account does not exist");
    }

    accountRepository.save(
        new AdministratorAccount(
            ADMIN_USERNAME, passwordEncoder.encode(bootstrapPassword), ADMIN_AUTHORITY));
  }
}
