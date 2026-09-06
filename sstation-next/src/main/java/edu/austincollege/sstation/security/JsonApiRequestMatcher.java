package edu.austincollege.sstation.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * TC-120: matches callers that asked for JSON and not for HTML — the single definition of "this is
 * an API call", shared by the security exception handling and {@code GlobalErrorAdvice}.
 *
 * <p>A browser navigation sends {@code Accept: text/html,...} and a bare {@code fetch()} sends
 * {@code Accept: * / *}; both are treated as "wants a page", so only a client that explicitly asks
 * for JSON gets a JSON error back. That is why the quick approve/reject fetch now sets the header
 * itself rather than relying on being guessed correctly.
 */
public class JsonApiRequestMatcher implements RequestMatcher {

  @Override
  public boolean matches(HttpServletRequest request) {
    boolean r = wantsJson(request);
    System.out.println(
        "PROBE matcher uri="
            + request.getRequestURI()
            + " accept="
            + request.getHeader("Accept")
            + " -> "
            + r);
    return r;
  }

  public static boolean wantsJson(HttpServletRequest request) {
    String accept = request.getHeader("Accept");
    if (accept == null || accept.isBlank()) {
      return false;
    }
    List<MediaType> accepted;
    try {
      accepted = MediaType.parseMediaTypes(accept);
    } catch (IllegalArgumentException malformed) {
      return false;
    }
    boolean html = accepted.stream().anyMatch(MediaType.TEXT_HTML::isCompatibleWith);
    boolean json = accepted.stream().anyMatch(MediaType.APPLICATION_JSON::isCompatibleWith);
    return json && !html;
  }
}
