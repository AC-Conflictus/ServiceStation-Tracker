package edu.austincollege.sstation.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * TC-120: turns an uncaught exception into the custom 500 page, and logs it with the request that
 * caused it.
 *
 * <p>The log line is the point. The 500 page deliberately shows no exception text or stack trace,
 * and tells the user to report the address instead — so the log has to be searchable by that same
 * address, which the servlet container's own "threw exception" line is not.
 */
@ControllerAdvice
public class GlobalErrorAdvice {

  private static final Logger log = LoggerFactory.getLogger(GlobalErrorAdvice.class);

  /**
   * Handles anything that escapes a controller <em>except</em> exceptions that already carry a
   * correct status — see {@link #alreadyMapped}.
   *
   * <p>Rethrowing the same instance is the supported escape hatch: {@code
   * ExceptionHandlerExceptionResolver} compares identity, so it leaves the exception unresolved
   * without logging a spurious warning and the next resolver (or the filter chain) picks it up as
   * if we had never looked at it.
   */
  @ExceptionHandler(Exception.class)
  public Object handleUncaught(Exception ex, HttpServletRequest request) throws Exception {
    if (alreadyMapped(ex)) {
      throw ex;
    }

    String path = request.getRequestURI();
    log.error("Unhandled exception serving {} {} — returning 500", request.getMethod(), path, ex);

    if (wantsJson(request)) {
      // An API caller gets an API answer. Handing a fetch() an HTML page is how you turn a clear
      // 500 into "Unexpected token '<'" three frames away from the actual problem.
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("status", 500, "error", "Internal Server Error", "path", path));
    }

    ModelAndView mav = new ModelAndView("error/500");
    mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    mav.addObject("path", path);
    return mav;
  }

  /**
   * True for exceptions this advice must keep its hands off, because something else already knows
   * the right status for them. Catching {@code Exception} is a blunt instrument and both of these
   * were live bugs before the tests caught them:
   *
   * <ul>
   *   <li><b>Spring Security's exceptions.</b> {@code @ExceptionHandler} resolution runs inside the
   *       DispatcherServlet, <em>upstream</em> of {@code ExceptionTranslationFilter}. Handling
   *       {@link AccessDeniedException} here breaks the two behaviours that filter owns: sending an
   *       anonymous user to the sign-in page, and rendering 403 for an authenticated one.
   *   <li><b>Spring MVC's {@link ErrorResponse} family.</b> {@code NoResourceFoundException},
   *       {@code HttpRequestMethodNotSupportedException} and friends carry their own status.
   *       Swallowing them turns every 404 into a 500 — which is exactly what happened here until
   *       {@code unknownPathRendersTheCustom404} failed.
   * </ul>
   */
  private static boolean alreadyMapped(Exception ex) {
    return ex instanceof AccessDeniedException
        || ex instanceof AuthenticationException
        || ex instanceof ErrorResponse
        || AnnotatedElementUtils.hasAnnotation(ex.getClass(), ResponseStatus.class);
  }

  /** True when the caller asked for JSON and did not ask for HTML. Browsers send HTML or star. */
  static boolean wantsJson(HttpServletRequest request) {
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
