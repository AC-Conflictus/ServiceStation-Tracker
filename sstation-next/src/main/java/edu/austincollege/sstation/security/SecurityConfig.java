package edu.austincollege.sstation.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.austincollege.sstation.repository.StudentRepository;
import edu.austincollege.sstation.repository.UserRepository;
import edu.austincollege.sstation.repository.UserRoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
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
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
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
 * <p>🏫 Sign-in seam (TC-124): {@code SSTATION_AUTH_MODE=directory} checks AC user names and
 * passwords against AC's directory (LDAP / Active Directory) through the same form, alongside the
 * local accounts. If AC IT would rather redirect to a SAML/OIDC identity provider, that is a second
 * {@code SecurityFilterChain} here, and {@link DirectoryAccountMapper}'s first-sign-in rules are
 * the part to reuse.
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

  /** Used to render JSON error bodies for API callers (TC-120). */
  private final ObjectMapper json;

  private final AuthProperties auth;

  public SecurityConfig(ObjectMapper json, AuthProperties auth) {
    this.json = json;
    this.auth = auth;
  }

  /**
   * Delegating encoder that produces {@code {bcrypt}} hashes and can verify them. Stored hashes
   * carry their algorithm id, so we can rotate algorithms later without a migration.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  /** Decides who an AC sign-in is in this app (TC-124). Only consulted in directory mode. */
  @Bean
  public DirectoryAccountMapper directoryAccountMapper(
      UserRepository users, UserRoleRepository userRoles, StudentRepository students) {
    return new DirectoryAccountMapper(
        users, userRoles, students, passwordEncoder(), auth.directory());
  }

  /** Failed sign-ins per user name (TC-125). */
  @Bean
  public SignInLimiter signInLimiter() {
    AuthProperties.SignIn limits = auth.signIn();
    if (auth.turnstile().enabled()) {
      LoggerFactory.getLogger(SecurityConfig.class)
          .info(
              "Sign-in limits: Turnstile CAPTCHA after {} failures, blocked after {}, for {}"
                  + " minutes",
              limits.captchaAfter(),
              limits.blockAfter(),
              limits.window().toMinutes());
    } else {
      LoggerFactory.getLogger(SecurityConfig.class)
          .info(
              "Sign-in limits: no Turnstile keys, so a user name is blocked after {} failures,"
                  + " for {} minutes",
              limits.captchaAfter(),
              limits.window().toMinutes());
    }
    return new SignInLimiter(
        limits, auth.turnstile().enabled(), auth.directory().emailDomain(), Clock.systemUTC());
  }

  /**
   * Confirms a solved CAPTCHA with Cloudflare (TC-125). Without keys no CAPTCHA is ever asked for,
   * so this is never called.
   */
  @Bean
  public CaptchaVerifier captchaVerifier() {
    return new TurnstileVerifier(auth.turnstile());
  }

  /**
   * TC-124: local accounts always; AC credentials too in directory mode.
   *
   * <p><b>Local is checked first, deliberately.</b> If AC's directory is unreachable, Spring
   * Security stops at the directory provider's error instead of trying the next one — so with the
   * order reversed, an outage would lock out the bootstrap admin and break-glass accounts at
   * exactly the moment someone needs them. Checking local first costs nothing for AC users: the
   * local check refuses directory accounts outright (see {@link CustomUserDetailsService}).
   */
  private AuthenticationManager authenticationManager(
      CustomUserDetailsService localAccounts,
      DirectoryAccountMapper directoryAccounts,
      SignInLimiter limiter,
      CaptchaVerifier captcha) {
    DaoAuthenticationProvider local = new DaoAuthenticationProvider(passwordEncoder());
    local.setUserDetailsService(localAccounts);
    List<AuthenticationProvider> providers = new ArrayList<>(List.of(local));
    if (auth.directoryMode()) {
      providers.add(DirectoryAuthentication.provider(auth.directory(), directoryAccounts));
    }
    // TC-125: the limit is checked before either provider, so a blocked user name never reaches
    // AC's directory and cannot push the real account toward an AD lockout.
    return new LimitedAuthenticationManager(new ProviderManager(providers), limiter, captcha);
  }

  /**
   * Each refusal gets its own message on the sign-in page. Only {@code ?error} is shown for a wrong
   * password — the others happen <em>after</em> the directory has accepted the password, so saying
   * why reveals nothing an attacker could use.
   *
   * <p>TC-125: a wrong password that used up the last attempt says so straight away, rather than
   * letting the person type a correct password into a block they were never told about.
   */
  private static AuthenticationFailureHandler failureHandler(SignInLimiter limiter) {
    return (request, response, ex) -> {
      String outcome = "error";
      if (ex instanceof SignInLimitException limited) {
        outcome =
            switch (limited.reason()) {
              case BLOCKED -> "blocked";
              case CAPTCHA_REQUIRED -> "captcha";
              case CAPTCHA_FAILED -> "captchaFailed";
              case CAPTCHA_UNAVAILABLE -> "captchaUnavailable";
            };
      } else if (ex instanceof BadCredentialsException) {
        outcome =
            switch (limiter.check(request.getParameter("username"))) {
              case BLOCKED -> "blocked";
              // Show the CAPTCHA with the "wrong password" message, so the next try can succeed.
              case CAPTCHA_REQUIRED -> "error&captcha";
              case ALLOWED -> "error";
            };
      } else if (ex instanceof DirectoryAccountException refused) {
        outcome =
            refused.reason() == DirectoryAccountException.Reason.NOT_REGISTERED
                ? "notRegistered"
                : "conflict";
      } else if (ex instanceof InternalAuthenticationServiceException) {
        // The directory (or the database) could not be reached — not the user's fault.
        LoggerFactory.getLogger(SecurityConfig.class).error("Sign-in unavailable", ex);
        outcome = "unavailable";
      }
      response.sendRedirect(request.getContextPath() + "/login?" + outcome);
    };
  }

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http,
      CustomUserDetailsService localAccounts,
      DirectoryAccountMapper directoryAccounts,
      SignInLimiter limiter,
      CaptchaVerifier captcha)
      throws Exception {
    http.authenticationManager(
        authenticationManager(localAccounts, directoryAccounts, limiter, captcha));
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
                    .failureHandler(failureHandler(limiter))
                    // TC-125: carries the solved CAPTCHA token, if any, to the limiter.
                    .authenticationDetailsSource(SignInDetails::new)
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
