package edu.austincollege.sstation.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.access.RequestMatcherDelegatingAccessDeniedHandler;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

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

  /** Used to render JSON error bodies for API callers (TC-120). */
  private final ObjectMapper json;

  public SecurityConfig(ObjectMapper json) {
    this.json = json;
  }

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
                auth.requestMatchers(
                        "/login",
                        "/forgot-password",
                        "/reset-password",
                        "/error",
                        "/actuator/health")
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
                    .permitAll())
        // TC-121: an account whose password came from an environment variable is confined to
        // /change-password until it has one of its own. Placed after AuthorizationFilter so the
        // SecurityContext is populated and normal authorization has already had its say.
        .addFilterAfter(new MustChangePasswordFilter(), AuthorizationFilter.class)
        // TC-120: answer API callers in their own content type.
        //
        // The quick approve/reject fetch on the hours page posts to a JSON endpoint. Without this,
        // an expired session did not fail loudly — it redirected to /login (302), fetch followed
        // the redirect, and the JS got the sign-in page back with status 200. `r.ok` was true, so
        // it went straight into r.json() and died on "Unexpected token '<'" three frames from the
        // actual problem, which was simply "you are signed out".
        //
        // Both delegates are built explicitly rather than via defaultAccessDeniedHandlerFor /
        // defaultAuthenticationEntryPointFor. Those look like the obvious API, but when they hold
        // exactly one mapping Spring Security drops the matcher and applies that handler to every
        // request — so registering "JSON callers get JSON" silently made *browsers* get JSON 403s
        // too. Spelling out the delegation keeps the fallback unambiguous.
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(entryPoint())
                    .accessDeniedHandler(accessDeniedHandler()));
    // CSRF is intentionally left enabled (the default). Thymeleaf forms inject the token
    // automatically via th:action; the AJAX endpoints in TC-106 will send the token header.
    return http.build();
  }

  /** JSON callers get 401 + JSON; everyone else keeps the redirect to the sign-in page. */
  private AuthenticationEntryPoint entryPoint() {
    AuthenticationEntryPoint json =
        (request, response, ex) ->
            writeJson(request, response, HttpStatus.UNAUTHORIZED, "Not signed in");

    LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> byRequest = new LinkedHashMap<>();
    byRequest.put(new JsonApiRequestMatcher(), json);

    DelegatingAuthenticationEntryPoint delegating =
        new DelegatingAuthenticationEntryPoint(byRequest);
    delegating.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"));
    return delegating;
  }

  /** JSON callers get 403 + JSON; everyone else falls through to the HTML error page. */
  private AccessDeniedHandler accessDeniedHandler() {
    AccessDeniedHandler json =
        (request, response, ex) ->
            writeJson(request, response, HttpStatus.FORBIDDEN, "Not permitted");

    LinkedHashMap<RequestMatcher, AccessDeniedHandler> byRequest = new LinkedHashMap<>();
    byRequest.put(new JsonApiRequestMatcher(), json);

    // The default sends 403 the ordinary way, which forwards to /error -> templates/error/403.html.
    return new RequestMatcherDelegatingAccessDeniedHandler(
        byRequest, new AccessDeniedHandlerImpl());
  }

  private void writeJson(
      HttpServletRequest request, HttpServletResponse response, HttpStatus status, String error)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    // Serialized rather than concatenated: the path is attacker-influenced and must be escaped.
    json.writeValue(
        response.getOutputStream(),
        Map.of("status", status.value(), "error", error, "path", request.getRequestURI()));
  }
}
