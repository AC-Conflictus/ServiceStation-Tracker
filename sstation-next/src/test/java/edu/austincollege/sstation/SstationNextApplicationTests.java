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
  void helloWorldPageIsServed() {
    String body = rest.getForObject("http://localhost:" + port + "/", String.class);
    assertThat(body).contains("rewrite-in-progress");
  }
}
