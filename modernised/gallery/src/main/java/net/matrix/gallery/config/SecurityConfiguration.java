package net.matrix.gallery.config;

import static org.springframework.security.config.Customizer.withDefaults;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Security policy for the modernised gallery. */
@Configuration
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            requests ->
                requests
                    .dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/categories",
                        "/categories/**",
                        "/artworks/**",
                        "/api/categories",
                        "/api/categories/**",
                        "/api/artworks/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .httpBasic(withDefaults())
        .build();
  }
}
