package net.matrix.gallery.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/** Security policy for the modernised gallery. */
@Configuration
public class SecurityConfiguration {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            requests ->
                requests
                    .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD)
                    .permitAll()
                    .requestMatchers("/admin", "/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, GallerySecuredPaths.PUBLIC_GET_PATHS)
                    .permitAll()
                    .requestMatchers(HttpMethod.HEAD, GallerySecuredPaths.PUBLIC_GET_PATHS)
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, GallerySecuredPaths.PUBLIC_ASSET_PATHS)
                    .permitAll()
                    .requestMatchers(HttpMethod.HEAD, GallerySecuredPaths.PUBLIC_ASSET_PATHS)
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
