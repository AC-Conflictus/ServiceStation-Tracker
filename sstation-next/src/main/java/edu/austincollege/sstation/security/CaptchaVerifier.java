package edu.austincollege.sstation.security;

/** Confirms a CAPTCHA solved in the browser (TC-125). */
public interface CaptchaVerifier {

  enum Result {
    VERIFIED,
    /** Missing, expired, reused or simply wrong. */
    REJECTED,
    /** The CAPTCHA service could not be asked. Treated as not solved. */
    UNAVAILABLE
  }

  Result verify(String token, String remoteIp);
}
