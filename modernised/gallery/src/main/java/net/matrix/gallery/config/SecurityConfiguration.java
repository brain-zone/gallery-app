package net.matrix.gallery.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
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
                    .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/", "/login", "/css/**", "/images/**")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/bio",
                        "/categories",
                        "/categories/**",
                        "/artworks/**",
                        "/api/categories",
                        "/api/categories/**",
                        "/api/artworks/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/admin", true).permitAll())
        .httpBasic(
            basic ->
                basic.authenticationEntryPoint(
                    (request, response, exception) -> {
                      response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Realm\"");
                      response.setStatus(401);
                      RequestDispatcher dispatcher =
                          request.getRequestDispatcher("/authentication-required");
                      dispatcher.forward(request, response);
                    }))
        .build();
  }
}
