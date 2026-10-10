package edu.austincollege.sstation.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import edu.austincollege.sstation.security.CaptchaVerifier.Result;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** TC-125: talks to a stand-in for Cloudflare's siteverify endpoint, never the real one. */
class TurnstileVerifierTest {

  private HttpServer cloudflare;
  private final AtomicReference<String> lastRequest = new AtomicReference<>();

  private TurnstileVerifier verifierAnswering(int status, String body) throws IOException {
    cloudflare = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    cloudflare.createContext(
        "/siteverify",
        exchange -> {
          lastRequest.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    cloudflare.start();
    return verifier("http://localhost:" + cloudflare.getAddress().getPort() + "/siteverify");
  }

  private static TurnstileVerifier verifier(String url) {
    return new TurnstileVerifier(new AuthProperties.Turnstile("site", "the-secret", url));
  }

  @AfterEach
  void stop() {
    if (cloudflare != null) {
      cloudflare.stop(0);
    }
  }

  @Test
  void aGenuineTokenIsVerifiedAndTheSecretTokenAndIpAreSent() throws IOException {
    Result result = verifierAnswering(200, "{\"success\":true}").verify("tok", "10.0.0.7");

    assertThat(result).isEqualTo(Result.VERIFIED);
    assertThat(lastRequest.get())
        .contains("secret=the-secret")
        .contains("response=tok")
        .contains("remoteip=10.0.0.7");
  }

  @Test
  void aTokenCloudflareDoesNotConfirmIsRejected() throws IOException {
    String answer = "{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}";

    assertThat(verifierAnswering(200, answer).verify("tok", null)).isEqualTo(Result.REJECTED);
  }

  @Test
  void aMissingTokenIsRejectedWithoutAskingCloudflare() throws IOException {
    TurnstileVerifier verifier = verifierAnswering(200, "{\"success\":true}");

    assertThat(verifier.verify("", "10.0.0.7")).isEqualTo(Result.REJECTED);
    assertThat(lastRequest.get()).isNull();
  }

  @Test
  void aCloudflareErrorMeansUnavailableNotVerified() throws IOException {
    assertThat(verifierAnswering(503, "{}").verify("tok", null)).isEqualTo(Result.UNAVAILABLE);
  }

  @Test
  void anUnreachableCloudflareMeansUnavailableNotVerified() throws IOException {
    int closedPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      closedPort = socket.getLocalPort();
    }

    assertThat(verifier("http://localhost:" + closedPort + "/siteverify").verify("tok", null))
        .isEqualTo(Result.UNAVAILABLE);
  }
}
