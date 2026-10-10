package edu.austincollege.sstation.security;

import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Asks Cloudflare whether a Turnstile token is genuine (TC-125). Tokens are single-use and expire
 * after a few minutes, so this is called once per sign-in attempt that needs one.
 *
 * <p>Fails closed: if Cloudflare cannot be reached, the attempt is refused rather than let through,
 * so disrupting this call is never a way around the CAPTCHA. That only affects user names that have
 * already failed several times; everyone else signs in without a CAPTCHA at all.
 */
class TurnstileVerifier implements CaptchaVerifier {

  private static final Logger log = LoggerFactory.getLogger(TurnstileVerifier.class);

  private final AuthProperties.Turnstile config;
  private final RestClient http;

  TurnstileVerifier(AuthProperties.Turnstile config) {
    this.config = config;
    SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
    // A sign-in should never hang on Cloudflare; give up and say "try again shortly" instead.
    timeouts.setConnectTimeout(Duration.ofSeconds(3));
    timeouts.setReadTimeout(Duration.ofSeconds(5));
    this.http = RestClient.builder().requestFactory(timeouts).build();
  }

  @Override
  public Result verify(String token, String remoteIp) {
    if (token == null || token.isBlank()) {
      return Result.REJECTED;
    }
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("secret", config.secretKey());
    form.add("response", token);
    if (remoteIp != null && !remoteIp.isBlank()) {
      form.add("remoteip", remoteIp);
    }
    try {
      Map<?, ?> answer =
          http.post()
              .uri(config.verifyUrl())
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .body(form)
              .retrieve()
              .body(Map.class);
      if (answer != null && Boolean.TRUE.equals(answer.get("success"))) {
        return Result.VERIFIED;
      }
      log.info(
          "Turnstile rejected a token: {}",
          answer == null ? "empty answer" : answer.get("error-codes"));
      return Result.REJECTED;
    } catch (RestClientException unreachable) {
      log.error("Could not verify a Turnstile token with Cloudflare", unreachable);
      return Result.UNAVAILABLE;
    }
  }
}
