package net.matrix.gallery.security;

import net.matrix.gallery.domain.model.AdministratorAccount;
import net.matrix.gallery.repository.AdministratorAccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads persisted administrator credentials for Spring Security. */
@Service
public class AdministratorUserDetailsService implements UserDetailsService {

  private final AdministratorAccountRepository accountRepository;

  public AdministratorUserDetailsService(AdministratorAccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    AdministratorAccount account =
        accountRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Unknown administrator account"));

    return User.withUsername(account.getUsername())
        .password(account.getPassword())
        .authorities(account.getAuthority())
        .build();
  }
}
