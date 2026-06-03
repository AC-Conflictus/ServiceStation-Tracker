package edu.austincollege.sstation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SstationNextApplicationTests {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate rest;

  @Test
  void contextLoads() {}

  @Test
  void loginPageIsServed() {
    // Since TC-104 the app is locked down; the public login page is the anonymous entry point.
    String body = rest.getForObject("http://localhost:" + port + "/login", String.class);
    assertThat(body).contains("Sign in");
  }
}
