package net.matrix.gallery.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import net.matrix.gallery.domain.model.AdministratorAccount;
import net.matrix.gallery.repository.AdministratorAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "GALLERY_ADMIN_BOOTSTRAP_PASSWORD=test-bootstrap-password")
@Transactional
class AdministratorBootstrapTest {

  @Autowired private AdministratorAccountRepository accountRepository;
  @Autowired private AdministratorBootstrap administratorBootstrap;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void freshDatabaseCreatesExactlyOneBcryptEncodedAdministrator() {
    assertThat(accountRepository.count()).isOne();

    AdministratorAccount account = accountRepository.findByUsername("admin").orElseThrow();
    assertThat(account.getAuthority()).isEqualTo("ROLE_ADMIN");
    assertThat(account.getPassword()).isNotEqualTo("test-bootstrap-password").startsWith("$2");
    assertThat(passwordEncoder.matches("test-bootstrap-password", account.getPassword())).isTrue();
  }

  @Test
  void repeatedBootstrapDoesNotDuplicateOrReplacePasswordHash() throws Exception {
    String existingHash = accountRepository.findByUsername("admin").orElseThrow().getPassword();

    administratorBootstrap.run(mock(ApplicationArguments.class));

    assertThat(accountRepository.count()).isOne();
    assertThat(accountRepository.findByUsername("admin").orElseThrow().getPassword())
        .isEqualTo(existingHash);
  }

  @Test
  void missingBootstrapPasswordFailsWithoutEncodingWhenAccountIsAbsent() {
    AdministratorAccountRepository emptyRepository = mock(AdministratorAccountRepository.class);
    PasswordEncoder unusedEncoder = mock(PasswordEncoder.class);
    when(emptyRepository.findByUsername("admin")).thenReturn(Optional.empty());
    AdministratorBootstrap bootstrap =
        new AdministratorBootstrap(emptyRepository, unusedEncoder, " ");

    assertThatThrownBy(() -> bootstrap.run(mock(ApplicationArguments.class)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "GALLERY_ADMIN_BOOTSTRAP_PASSWORD must be set when the administrator account does not exist");
    verifyNoInteractions(unusedEncoder);
  }

  @Test
  void existingAdministratorDoesNotRequireBootstrapPassword() throws Exception {
    AdministratorAccountRepository existingRepository = mock(AdministratorAccountRepository.class);
    PasswordEncoder unusedEncoder = mock(PasswordEncoder.class);
    AdministratorAccount account =
        new AdministratorAccount("admin", "preserved-hash", "ROLE_ADMIN");
    when(existingRepository.findByUsername("admin")).thenReturn(Optional.of(account));
    AdministratorBootstrap bootstrap =
        new AdministratorBootstrap(existingRepository, unusedEncoder, "");

    bootstrap.run(mock(ApplicationArguments.class));

    verifyNoInteractions(unusedEncoder);
    verify(existingRepository).findByUsername("admin");
    verifyNoMoreInteractions(existingRepository);
  }
}
