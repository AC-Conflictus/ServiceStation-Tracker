package edu.austincollege.sstation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.austincollege.sstation.domain.SiteAvailability;
import edu.austincollege.sstation.repository.SiteAvailabilityRepository;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class SiteAvailabilityServiceTest {
  private final Instant now = Instant.parse("2027-04-10T14:00:00Z");
  private final SiteAvailabilityRepository repository = mock(SiteAvailabilityRepository.class);
  private final SiteAvailabilityService service =
      new SiteAvailabilityService(
          repository,
          new ObjectMapper(),
          "test-only-master-secret-abcdefghijklmnopqrstuvwxyz",
          Clock.fixed(now, ZoneOffset.UTC));

  @Test
  void sharedAppsScriptFixtureMatchesJavaHmacBytes() throws Exception {
    try (var in = getClass().getResourceAsStream("/site-availability-v1.json")) {
      var fixture = new ObjectMapper().readTree(in);
      assertThat(
              "sha256="
                  + HexFormat.of()
                      .formatHex(
                          SiteAvailabilityService.hmac(
                              fixture.path("secret").asText(),
                              "v1\n" + fixture.path("body").asText())))
          .isEqualTo(fixture.path("signature").asText());
    }
  }

  @Test
  void staleFullOrClosedCountsAllowCheckingFormAndNeverLookCurrent() {
    var value = mock(SiteAvailability.class);
    when(value.getObservedAt()).thenReturn(now.minusSeconds(601));
    when(value.getCapacity()).thenReturn(8);
    when(value.getRemaining()).thenReturn(0);
    when(value.getAcceptingResponses()).thenReturn(false);
    var view = service.view(value);
    assertThat(view.state()).isEqualTo("STALE");
    assertThat(view.blocked()).isFalse();
    assertThat(view.detail()).contains("Last known: 0 of 8", "UTC");
    when(value.getObservedAt()).thenReturn(now.minusSeconds(599));
    assertThat(service.view(value).blocked()).isTrue();
  }

  @Test
  void missingMasterDisablesConnectionAndReceiverAndHidesPreviousCounts() {
    var disabled =
        new SiteAvailabilityService(
            repository, new ObjectMapper(), "", Clock.fixed(now, ZoneOffset.UTC));
    assertThat(disabled.isEnabled()).isFalse();
    assertThatThrownBy(() -> disabled.connect(1L))
        .isInstanceOfSatisfying(
            ResponseStatusException.class,
            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    assertThatThrownBy(() -> disabled.receive("{}".getBytes(StandardCharsets.UTF_8), null))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(disabled.view(mock(SiteAvailability.class)).state()).isEqualTo("UNAVAILABLE");
    verifyNoInteractions(repository);
  }
}
