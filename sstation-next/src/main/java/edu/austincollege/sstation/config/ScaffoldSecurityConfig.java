package edu.austincollege.sstation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Temporary permit-all security so the TC-101 Hello World page is reachable without a login. This
 * is intentionally wide open and is REPLACED by the real form-login / role-based config in TC-104.
 * Do not build on top of it.
 */
@Configuration
public class ScaffoldSecurityConfig {

  @Bean
  public SecurityFilterChain scaffoldFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }
}
