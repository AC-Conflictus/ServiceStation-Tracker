package edu.austincollege.sstation.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.APIResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TC-111: the CSV (TC-108c) and PDF (TC-108d) exports.
 *
 * <p>Fetched through the signed-in browser context's request API, so these go over the real filter
 * chain with the real session cookie — a download that only works for an anonymous request, or only
 * with CSRF disabled, would fail here.
 *
 * <p>Content type alone is a weak assertion: a controller can set {@code application/pdf} and still
 * stream something unopenable. Each check therefore looks at the bytes as well.
 */
@DisplayName("E2E: CSV and PDF downloads")
class DownloadsE2eTest extends E2eTest {

  private APIResponse fetch(String path) {
    return context.request().get(url(path));
  }

  @Test
  @DisplayName("every report offers a CSV with the right type, filename and a header row")
  void reportCsvDownloads() {
    signInAsAdmin();

    for (String report :
        new String[] {"summary", "semester", "year", "event", "community-org", "campus-org"}) {
      APIResponse response = fetch("/reports/" + report + ".csv");

      assertThat(response.status()).as("%s.csv status", report).isEqualTo(200);
      assertThat(response.headers().get("content-type"))
          .as("%s.csv type", report)
          .contains("text/csv");
      assertThat(response.headers().get("content-disposition"))
          .as("%s.csv disposition", report)
          .contains("attachment");

      String body = new String(response.body(), StandardCharsets.UTF_8);
      assertThat(body).as("%s.csv should not be empty", report).isNotBlank();
      assertThat(body.lines().count())
          .as("%s.csv needs a header plus data", report)
          .isGreaterThan(1);
    }
  }

  @Test
  @DisplayName("a student can download their own report as CSV")
  void studentReportCsvDownloads() {
    signInAsStudent();

    APIResponse response = fetch("/student/report.csv");

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.headers().get("content-type")).contains("text/csv");
    assertThat(new String(response.body(), StandardCharsets.UTF_8)).isNotBlank();
  }

  @Test
  @DisplayName("a student can download their own report as a real PDF")
  void studentReportPdfDownloads() {
    signInAsStudent();

    APIResponse response = fetch("/student/report.pdf");

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.headers().get("content-type")).contains("application/pdf");

    // The bytes, not just the header. openhtmltopdf renders a Thymeleaf template through jsoup
    // (TC-108d); a broken template would still come back as application/pdf.
    byte[] body = response.body();
    assertThat(body.length).as("a PDF of a real report should not be a stub").isGreaterThan(1000);
    assertThat(new String(body, 0, 5, StandardCharsets.ISO_8859_1))
        .as("PDF magic bytes")
        .isEqualTo("%PDF-");
  }

  @Test
  @DisplayName("downloads are not available to an anonymous visitor")
  void downloadsRequireAuthentication() {
    // No sign-in on this one. The exports read real student data, so they must sit behind auth
    // like every other page rather than being quietly public because they are "just a file".
    APIResponse csv = fetch("/reports/year.csv");
    APIResponse pdf = fetch("/student/report.pdf");

    assertThat(csv.url()).as("anonymous CSV should be bounced to sign-in").contains("/login");
    assertThat(pdf.url()).as("anonymous PDF should be bounced to sign-in").contains("/login");
  }

  @Test
  @DisplayName("a student cannot download the admin-wide reports")
  void studentCannotDownloadAdminReports() {
    signInAsStudent();

    assertThat(fetch("/reports/year.csv").status())
        .as("the all-students export is not a student's to read")
        .isEqualTo(403);
  }
}
