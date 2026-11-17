package edu.austincollege.sstation.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.austincollege.sstation.domain.*;
import edu.austincollege.sstation.repository.*;
import edu.austincollege.sstation.service.SiteAvailabilityService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = "sstation.site-sync.master-secret=integration-test-only-master-secret-0000000")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SiteAvailabilityIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired EventRepository events;
  @Autowired EventSiteRepository sites;
  @Autowired SiteAvailabilityRepository snapshots;
  @Autowired SiteAvailabilityService service;
  Event event;
  EventSite site;

  @BeforeEach
  void setup() {
    event = new Event();
    event.setName("Sync " + UUID.randomUUID());
    event.setDescription("Synthetic");
    event.setContact("Office");
    event.setContactPhone("9030000000");
    event.setContactEmail("test@example.invalid");
    event.setRegistrationMode(EventRegistrationMode.SITE_FORMS);
    events.save(event);
    site = new EventSite();
    site.setEvent(event);
    site.setName("Example nonprofit");
    site.setLocation("Example St");
    site.setRegistrationUrl("https://forms.gle/synthetic");
    sites.save(site);
  }

  @AfterEach
  void cleanup() {
    events.deleteById(event.getId());
  }

  String adminPath() {
    return "/admin/events/" + event.getId() + "/sites/" + site.getId() + "/availability";
  }

  String body(
      SiteAvailabilityService.Connection connection,
      int capacity,
      int remaining,
      boolean accepting,
      Instant at)
      throws Exception {
    return json.writeValueAsString(
        Map.of(
            "version",
            1,
            "siteKey",
            connection.siteKey(),
            "capacity",
            capacity,
            "remaining",
            remaining,
            "acceptingResponses",
            accepting,
            "observedAt",
            at.toString()));
  }

  String sign(String secret, String body) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return "sha256="
        + HexFormat.of().formatHex(mac.doFinal(("v1\n" + body).getBytes(StandardCharsets.UTF_8)));
  }

  org.springframework.test.web.servlet.ResultActions send(String body, String secret)
      throws Exception {
    return mvc.perform(
        post("/api/site-availability")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .header("X-Sstation-Signature", sign(secret, body)));
  }

  @Test
  void signedAnonymousUpdatesAreIsolatedOrderedAndNeverCreateSignups() throws Exception {
    var connection = service.connect(site.getId());
    // Force sub-microsecond precision even on hosts whose clock only returns microseconds.
    Instant now =
        Instant.now()
            .minusSeconds(1)
            .truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
            .plusNanos(123);
    send(body(connection, 8, 5, true, now), connection.secret())
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
    var saved = snapshots.findById(site.getId()).orElseThrow();
    assertThat(saved.getCapacity()).isEqualTo(8);
    assertThat(saved.getRemaining()).isEqualTo(5);
    // Duplicate and delayed authenticated observations acknowledge but cannot replace the newer
    // count.
    send(body(connection, 8, 0, false, now.minusSeconds(1)), connection.secret())
        .andExpect(status().isNoContent());
    send(body(connection, 8, 1, true, now), connection.secret()).andExpect(status().isNoContent());
    assertThat(snapshots.findById(site.getId()).orElseThrow().getRemaining()).isEqualTo(5);
    var html =
        mvc.perform(get("/student/events/" + event.getId()).with(user("student").roles("STUDENT")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(html)
        .contains("5 of 8 individual spots left", "Updated ", "Open registration form")
        .doesNotContain(connection.secret(), connection.siteKey());
    // Sender's full state is remaining=0 plus acceptingResponses=false; distinguish it from office
    // closure.
    send(body(connection, 8, 0, false, now.plusMillis(100)), connection.secret())
        .andExpect(status().isNoContent());
    mvc.perform(get("/student/events/" + event.getId()).with(user("student").roles("STUDENT")))
        .andExpect(
            xpath("//article//button[@disabled and not(@hidden) and text()='Site full']").exists());
    send(body(connection, 10, 2, false, now.plusMillis(200)), connection.secret())
        .andExpect(status().isNoContent());
    mvc.perform(get("/student/events/" + event.getId()).with(user("student").roles("STUDENT")))
        .andExpect(
            xpath("//article//button[@disabled and not(@hidden) and text()='Registration closed']")
                .exists());
    send(body(connection, 10, 2, true, now.plusMillis(300)), connection.secret())
        .andExpect(status().isNoContent());
    mvc.perform(get("/student/events/" + event.getId()).with(user("student").roles("STUDENT")))
        .andExpect(
            xpath("//article//a[not(@hidden) and text()='Open registration form']").exists());
    var replacement = service.connect(site.getId());
    send(body(connection, 8, 5, true, now), connection.secret())
        .andExpect(status().isUnauthorized());
    assertThat(snapshots.findById(site.getId()).orElseThrow().getObservedAt()).isNull();
    // A site's secret cannot sign an observation for another mapping.
    send(body(replacement, 8, 5, true, now), connection.secret())
        .andExpect(status().isUnauthorized());
    service.disconnect(site.getId());
    send(body(replacement, 8, 5, true, now), replacement.secret())
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsUnauthenticatedAlteredMalformedOversizedAndOutOfWindowUpdates() throws Exception {
    var connection = service.connect(site.getId());
    Instant now = Instant.now();
    String valid = body(connection, 8, 5, true, now);
    mvc.perform(
            post("/api/site-availability").contentType(MediaType.APPLICATION_JSON).content(valid))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/site-availability")
                .contentType(MediaType.APPLICATION_JSON)
                .content(valid.replace(":5", ":4"))
                .header("X-Sstation-Signature", sign(connection.secret(), valid)))
        .andExpect(status().isUnauthorized());
    send(body(connection, 8, 9, true, now), connection.secret()).andExpect(status().isBadRequest());
    send(body(connection, -1, 0, true, now), connection.secret())
        .andExpect(status().isBadRequest());
    send(valid.replace("\"capacity\":8", "\"capacity\":1.5"), connection.secret())
        .andExpect(status().isBadRequest());
    send(valid.replace("\"version\":1", "\"version\":4294967297"), connection.secret())
        .andExpect(status().isBadRequest());
    send(
            valid.replace("\"acceptingResponses\":true", "\"acceptingResponses\":\"true\""),
            connection.secret())
        .andExpect(status().isBadRequest());
    send(valid.replace("{", "{\"extra\":1,"), connection.secret())
        .andExpect(status().isBadRequest());
    send(body(connection, 8, 5, true, now.minusSeconds(601)), connection.secret())
        .andExpect(status().isBadRequest());
    send(body(connection, 8, 5, true, now.plusSeconds(62)), connection.secret())
        .andExpect(status().isBadRequest());
    send(valid + " {}", connection.secret()).andExpect(status().isBadRequest());
    send("{not-json", connection.secret()).andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/site-availability")
                .contentType(MediaType.APPLICATION_JSON)
                .content("x".repeat(4097)))
        .andExpect(status().isPayloadTooLarge());
    mvc.perform(post("/api/site-availability").contentType(MediaType.TEXT_PLAIN).content(valid))
        .andExpect(status().isUnsupportedMediaType());
    assertThat(snapshots.findById(site.getId()).orElseThrow().getObservedAt()).isNull();
  }

  @Test
  void adminProvisioningRequiresRoleCsrfAndCorrectEventAndDoesNotRedisplaySecrets()
      throws Exception {
    for (String role : List.of("STUDENT", "MODERATOR")) {
      mvc.perform(get(adminPath()).with(user("other").roles(role)))
          .andExpect(status().isForbidden());
      mvc.perform(post(adminPath() + "/connect").with(user("other").roles(role)).with(csrf()))
          .andExpect(status().isForbidden());
      mvc.perform(post(adminPath() + "/disconnect").with(user("other").roles(role)).with(csrf()))
          .andExpect(status().isForbidden());
    }
    mvc.perform(post(adminPath() + "/connect").with(user("admin").roles("ADMIN")))
        .andExpect(status().isForbidden());
    mvc.perform(
            post(adminPath().replace("/events/" + event.getId(), "/events/999999999") + "/connect")
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
        .andExpect(status().isNotFound());
    var result =
        mvc.perform(post(adminPath() + "/connect").with(user("admin").roles("ADMIN")).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andReturn();
    var credentials =
        (SiteAvailabilityService.Connection) result.getModelAndView().getModel().get("connection");
    assertThat(result.getResponse().getContentAsString()).contains(credentials.secret());
    assertThat(result.getRequest().getSession().getAttributeNames().asIterator())
        .toIterable()
        .noneMatch(n -> n.contains("connection"));
    mvc.perform(get(adminPath()).with(user("admin").roles("ADMIN")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(credentials.secret()))));
    mvc.perform(post(adminPath() + "/disconnect").with(user("admin").roles("ADMIN")).with(csrf()))
        .andExpect(status().is3xxRedirection());
    assertThat(snapshots.findById(site.getId())).isEmpty();
    // The machine chain exemption does not weaken neighboring browser/API endpoints.
    mvc.perform(
            post("/api/site-availability/other")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
  }
}
