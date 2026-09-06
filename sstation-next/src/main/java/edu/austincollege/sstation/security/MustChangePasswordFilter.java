package edu.austincollege.sstation.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Corners an account flagged {@code mustChangePassword} on {@code /change-password} (TC-121).
 *
 * <p>Applies to the bootstrap admin, whose password arrived in an environment variable and stays
 * readable in the deployment's own configuration. Letting that account browse the app while still
 * holding that credential is the thing this card exists to prevent.
 */
public class MustChangePasswordFilter extends OncePerRequestFilter {

  /**
   * Paths that stay reachable while flagged.
   *
   * <p>{@code /change-password} is the destination. {@code /logout} is the way out for someone who
   * would rather not proceed — without it the account is trapped. {@code /login} and {@code /error}
   * are here because redirecting either one produces a loop: {@code /login} is where the session
   * ends up after the change, and an error during the change would otherwise bounce back into the
   * form and hide itself.
   */
  private static final Set<String> ALLOWED =
      Set.of("/change-password", "/logout", "/login", "/error");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null
        && auth.isAuthenticated()
        && auth.getPrincipal() instanceof SstationUserDetails user
        && user.isMustChangePassword()
        && !isAllowed(request)) {

      response.sendRedirect(request.getContextPath() + "/change-password");
      return;
    }
    chain.doFilter(request, response);
  }

  /** Static assets are exempt, or the change-password page renders unstyled. */
  private boolean isAllowed(HttpServletRequest request) {
    String path = request.getRequestURI().substring(request.getContextPath().length());
    return ALLOWED.contains(path)
        || path.startsWith("/webjars/")
        || path.startsWith("/css/")
        || path.startsWith("/js/")
        || path.equals("/favicon.ico");
  }
}
