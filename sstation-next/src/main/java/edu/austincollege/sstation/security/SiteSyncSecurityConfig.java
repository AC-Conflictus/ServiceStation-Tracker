package edu.austincollege.sstation.security;

import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SiteSyncSecurityConfig {
  @Bean
  @Order(1)
  SecurityFilterChain siteSyncChain(HttpSecurity http) throws Exception {
    // This exact machine endpoint authenticates its raw body with HMAC in the service.
    // Browser endpoints retain their existing session auth, role checks and CSRF protection.
    return http.securityMatcher("/api/site-availability")
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(cache -> cache.disable())
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(HttpMethod.POST, "/api/site-availability")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .build();
  }
}
