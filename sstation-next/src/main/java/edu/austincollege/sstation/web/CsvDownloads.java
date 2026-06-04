package edu.austincollege.sstation.web;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Shared helper for serving a CSV string as a file download (TC-023 / TC-108c). */
final class CsvDownloads {

  private CsvDownloads() {}

  static ResponseEntity<String> attachment(String filename, String csv) {
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(csv);
  }
}
