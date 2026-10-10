package edu.austincollege.sstation.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * The client address Spring Security already records, plus the solved CAPTCHA token if the sign-in
 * form carried one (TC-125). This is how the token reaches {@link LimitedAuthenticationManager}.
 */
public class SignInDetails extends WebAuthenticationDetails {

  /** The form field Cloudflare's widget adds to the form it sits in. */
  static final String TURNSTILE_FIELD = "cf-turnstile-response";

  private final String captchaToken;

  public SignInDetails(HttpServletRequest request) {
    super(request);
    this.captchaToken = request.getParameter(TURNSTILE_FIELD);
  }

  public String captchaToken() {
    return captchaToken;
  }
}
