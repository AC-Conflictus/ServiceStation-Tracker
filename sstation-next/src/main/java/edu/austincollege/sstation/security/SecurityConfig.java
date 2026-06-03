package edu.austincollege.sstation.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 6 configuration — the modern replacement for the EOL Grails Spring Security
 * plugin (2.0-RC5). Fixes several Grails-era gaps at the source:
 *
 * <ul>
 *   <li><b>TC-008</b>: passwords are encoded with BCrypt via a delegating encoder; no plaintext
 *       path.
 *   <li><b>TC-018</b>: CSRF protection is on (Spring Security default; we simply don't disable it).
 *   <li><b>TC-019</b>: method security is enabled so controllers gate every endpoint with
 *       {@code @PreAuthorize}.
 * </ul>
 *
 * <p>🏫 SSO seam: if AC IT runs a SAML/OIDC IdP, add the relevant starter and a second {@code
 * SecurityFilterChain} here; the form-login chain below can stay for local/admin accounts.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  /**
   * Delegating encoder that produces {@code {bcrypt}} hashes and can verify them. Stored hashes
   * carry their algorithm id, so we can rotate algorithms later without a migration.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/login", "/error", "/actuator/health")
                    .permitAll()
                    .requestMatchers("/css/**", "/js/**", "/webjars/**", "/favicon.ico")
                    .permitAll()
                    // Method-level @PreAuthorize is the source of truth for role gating; everything
                    // else simply needs an authenticated principal.
                    .anyRequest()
                    .authenticated())
        .formLogin(
            form ->
                form.loginPage("/login")
                    .defaultSuccessUrl("/", true)
                    .failureUrl("/login?error")
                    .permitAll())
        .logout(
            logout ->
                logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll());
    // CSRF is intentionally left enabled (the default). Thymeleaf forms inject the token
    // automatically via th:action; the AJAX endpoints in TC-106 will send the token header.
    return http.build();
  }
}
