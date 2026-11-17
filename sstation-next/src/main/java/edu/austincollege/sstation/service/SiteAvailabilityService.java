package edu.austincollege.sstation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.austincollege.sstation.domain.EventSite;
import edu.austincollege.sstation.domain.SiteAvailability;
import edu.austincollege.sstation.repository.SiteAvailabilityRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SiteAvailabilityService {
  private static final Duration FRESHNESS = Duration.ofMinutes(10);
  private final SiteAvailabilityRepository snapshots;
  private final ObjectMapper json;
  private final String masterSecret;
  private final Clock clock;

  @Autowired
  public SiteAvailabilityService(
      SiteAvailabilityRepository snapshots,
      ObjectMapper json,
      @Value("${sstation.site-sync.master-secret:}") String masterSecret) {
    this(snapshots, json, masterSecret, Clock.systemUTC());
  }

  SiteAvailabilityService(
      SiteAvailabilityRepository snapshots, ObjectMapper json, String masterSecret, Clock clock) {
    if (!masterSecret.isEmpty() && (masterSecret.length() < 32 || masterSecret.isBlank()))
      throw new IllegalArgumentException(
          "Site sync master secret must be blank or at least 32 characters.");
    this.snapshots = snapshots;
    this.json = json;
    this.masterSecret = masterSecret;
    this.clock = clock;
  }

  public boolean isEnabled() {
    return !masterSecret.isEmpty();
  }

  /**
   * A new mapping revokes the old key and clears counts. Only admins receive the derived secret.
   */
  @Transactional
  public Connection connect(Long siteId) {
    requireEnabled();
    snapshots.deleteById(siteId);
    snapshots.flush();
    String key = UUID.randomUUID().toString();
    snapshots.save(new SiteAvailability(siteId, key));
    return new Connection(key, secretFor(key));
  }

  @Transactional
  public void disconnect(Long siteId) {
    snapshots.deleteById(siteId);
  }

  private void requireEnabled() {
    if (!isEnabled())
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Site sync is not configured.");
  }

  private String secretFor(String key) {
    return HexFormat.of().formatHex(hmac(masterSecret, "site-availability-key-v1\n" + key));
  }

  static byte[] hmac(String key, String message) {
    return hmacBytes(key, message.getBytes(StandardCharsets.UTF_8));
  }

  private static byte[] hmacBytes(String key, byte[] message) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return mac.doFinal(message);
    } catch (java.security.GeneralSecurityException ex) {
      throw new IllegalStateException("HMAC unavailable", ex);
    }
  }

  @Transactional
  public void receive(byte[] raw, String signature) {
    requireEnabled();
    if (signature == null || !signature.matches("sha256=[0-9a-f]{64}")) throw refused();
    try {
      JsonNode node =
          json.reader()
              .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
              .readTree(raw);
      if (node == null
          || !node.isObject()
          || !node.path("siteKey").isTextual()
          || !node.path("siteKey").textValue().matches("[A-Za-z0-9_-]{1,100}")) throw invalid();
      String key = node.path("siteKey").textValue();
      SiteAvailability target =
          snapshots.findBySyncKey(key).orElseThrow(SiteAvailabilityService::refused);
      byte[] message =
          java.nio.ByteBuffer.allocate(3 + raw.length)
              .put(new byte[] {118, 49, 10})
              .put(raw)
              .array();
      byte[] expected = hmacBytes(secretFor(key), message);
      if (!MessageDigest.isEqual(expected, HexFormat.of().parseHex(signature.substring(7))))
        throw refused();
      if (node.size() != 6
          || !node.path("version").isIntegralNumber()
          || !node.path("version").canConvertToInt()
          || node.path("version").intValue() != 1
          || !integer(node.path("capacity"))
          || !integer(node.path("remaining"))
          || node.path("remaining").intValue() > node.path("capacity").intValue()
          || !node.path("acceptingResponses").isBoolean()
          || !node.path("observedAt").isTextual()) throw invalid();
      // Match the database timestamp precision before both comparison and persistence.
      Instant observedAt =
          Instant.parse(node.path("observedAt").textValue())
              .truncatedTo(java.time.temporal.ChronoUnit.MICROS);
      Instant now = clock.instant();
      if (observedAt.isBefore(now.minus(FRESHNESS)) || observedAt.isAfter(now.plusSeconds(60)))
        throw invalid();
      snapshots.observe(
          target.getSiteId(),
          key,
          node.path("capacity").intValue(),
          node.path("remaining").intValue(),
          node.path("acceptingResponses").booleanValue(),
          observedAt);
    } catch (java.io.IOException | java.time.format.DateTimeParseException ex) {
      throw invalid();
    }
  }

  private static boolean integer(JsonNode node) {
    return node.isIntegralNumber() && node.canConvertToInt() && node.intValue() >= 0;
  }

  private static ResponseStatusException refused() {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  }

  private static ResponseStatusException invalid() {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST);
  }

  @Transactional(readOnly = true)
  public Map<Long, View> views(List<EventSite> sites) {
    Map<Long, View> result = new HashMap<>();
    if (!sites.isEmpty())
      for (SiteAvailability value :
          snapshots.findBySiteIdIn(sites.stream().map(EventSite::getId).toList()))
        result.put(value.getSiteId(), view(value));
    for (EventSite site : sites)
      result.putIfAbsent(
          site.getId(), new View("UNAVAILABLE", "Availability unavailable", null, false, null));
    return result;
  }

  View view(SiteAvailability value) {
    if (!isEnabled() || value.getObservedAt() == null)
      return new View("UNAVAILABLE", "Availability unavailable", null, false, null);
    String updated =
        DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a 'UTC'", Locale.US)
            .withZone(ZoneOffset.UTC)
            .format(value.getObservedAt());
    boolean stale = value.getObservedAt().isBefore(clock.instant().minus(FRESHNESS));
    String count = value.getRemaining() + " of " + value.getCapacity() + " individual spots left";
    if (stale)
      return new View(
          "STALE",
          "Availability may be out of date",
          "Last known: " + count + ". Updated " + updated,
          false,
          value.getObservedAt());
    if (value.getRemaining() == 0)
      return new View(
          "FULL", "Site full", count + ". Updated " + updated, true, value.getObservedAt());
    if (!Boolean.TRUE.equals(value.getAcceptingResponses()))
      return new View(
          "CLOSED",
          "Registration closed",
          count + ". Updated " + updated,
          true,
          value.getObservedAt());
    return new View("OPEN", count, "Updated " + updated, false, value.getObservedAt());
  }

  public record Connection(String siteKey, String secret) {}

  public record View(
      String state, String label, String detail, boolean blocked, Instant observedAt) {
    public Long freshUntil() {
      return observedAt == null ? null : observedAt.plus(FRESHNESS).toEpochMilli();
    }

    public String staleDetail() {
      if (state.equals("STALE")) return detail;
      return "Last known: " + (state.equals("OPEN") ? label + ". " + detail : detail);
    }
  }
}
