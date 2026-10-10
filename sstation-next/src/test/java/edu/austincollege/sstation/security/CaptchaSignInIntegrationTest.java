package edu.austincollege.sstation.security;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * TC-125 with Turnstile keys configured, through the real filter chain, against a stand-in for
 * Cloudflare that confirms only the token {@code good}.
 *
 * <p>Its own context, thrown away afterwards, because it deliberately blocks a seeded account.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DirtiesContext
class CaptchaSignInIntegrationTest {

  static final HttpServer cloudflare = startCloudflare();

  private static HttpServer startCloudflare() {
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
      server.createContext(
          "/siteverify",
          exchange -> {
            String form =
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body =
                ("{\"success\":" + form.contains("response=good&") + "}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
          });
      server.start();
      return server;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @DynamicPropertySource
  static void turnstile(DynamicPropertyRegistry props) {
    props.add("sstation.auth.turnstile.site-key", () -> "test-site-key");
    props.add("sstation.auth.turnstile.secret-key", () -> "test-secret");
    props.add(
        "sstation.auth.turnstile.verify-url",
        () -> "http://localhost:" + cloudflare.getAddress().getPort() + "/siteverify");
  }

  @AfterAll
  static void stopCloudflare() {
    cloudflare.stop(0);
  }

  @Autowired private MockMvc mvc;

  private ResultActions signIn(String username, String password, String captchaToken)
      throws Exception {
    var request =
        post("/login").with(csrf()).param("username", username).param("password", password);
    if (captchaToken != null) {
      request.param(SignInDetails.TURNSTILE_FIELD, captchaToken);
    }
    return mvc.perform(request);
  }

  @Test
  void afterThreeFailuresTheRightPasswordNeedsAConfirmedCaptcha() throws Exception {
    signIn("moderator", "nope", null).andExpect(redirectedUrl("/login?error"));
    signIn("moderator", "nope", null).andExpect(redirectedUrl("/login?error"));
    signIn("moderator", "nope", null).andExpect(redirectedUrl("/login?error&captcha"));

    signIn("moderator", "moderator_secret", null)
        .andExpect(unauthenticated())
        .andExpect(redirectedUrl("/login?captcha"));
    signIn("moderator", "moderator_secret", "forged")
        .andExpect(unauthenticated())
        .andExpect(redirectedUrl("/login?captchaFailed"));
    signIn("moderator", "moderator_secret", "good")
        .andExpect(authenticated().withUsername("moderator"));
  }

  @Test
  void evenWithConfirmedCaptchasTheFifthFailureBlocks() throws Exception {
    for (int i = 0; i < 3; i++) {
      signIn("student", "nope", null);
    }
    signIn("student", "nope", "good").andExpect(redirectedUrl("/login?error&captcha"));
    signIn("student", "nope", "good").andExpect(redirectedUrl("/login?blocked"));

    signIn("student", "student_secret", "good")
        .andExpect(unauthenticated())
        .andExpect(redirectedUrl("/login?blocked"));
  }

  @Test
  void theWidgetAppearsOnlyWhenAFailureAskedForIt() throws Exception {
    // An ordinary sign-in never loads anything from Cloudflare.
    mvc.perform(get("/login"))
        .andExpect(content().string(not(containsString("challenges.cloudflare.com"))));
    mvc.perform(get("/login").param("error", "").param("captcha", ""))
        .andExpect(content().string(containsString("challenges.cloudflare.com/turnstile")))
        .andExpect(content().string(containsString("data-sitekey=\"test-site-key\"")))
        .andExpect(content().string(containsString("please also complete the check below")))
        .andExpect(content().string(not(containsString("test-secret"))));
  }
}
