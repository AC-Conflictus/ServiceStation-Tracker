package edu.austincollege.sstation.web;

import edu.austincollege.sstation.service.SiteAvailabilityService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** Signed machine endpoint: no session, browser login or participant records. */
@RestController
public class SiteAvailabilityController {
  private final SiteAvailabilityService availability;

  public SiteAvailabilityController(SiteAvailabilityService availability) {
    this.availability = availability;
  }

  @PostMapping(value = "/api/site-availability", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> receive(
      HttpServletRequest request,
      @RequestHeader(value = "X-Sstation-Signature", required = false) String signature)
      throws IOException {
    byte[] raw = request.getInputStream().readNBytes(4097);
    if (raw.length > 4096) return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
    availability.receive(raw, signature);
    return ResponseEntity.noContent().build();
  }
}
