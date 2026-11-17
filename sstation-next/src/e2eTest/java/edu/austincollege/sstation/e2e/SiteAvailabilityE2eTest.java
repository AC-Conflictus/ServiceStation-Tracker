package edu.austincollege.sstation.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class SiteAvailabilityE2eTest extends E2eTest {
  private void publish(String key, String secret, int remaining, boolean open, int step)
      throws Exception {
    String body =
        "{\"version\":1,\"siteKey\":\""
            + key
            + "\",\"capacity\":8,\"remaining\":"
            + remaining
            + ",\"acceptingResponses\":"
            + open
            + ",\"observedAt\":\""
            + Instant.now().plusMillis(step)
            + "\"}";
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    String signature =
        "sha256="
            + HexFormat.of()
                .formatHex(mac.doFinal(("v1\n" + body).getBytes(StandardCharsets.UTF_8)));
    var response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create(url("/api/site-availability")))
                    .header("Content-Type", "application/json")
                    .header("X-Sstation-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(204);
  }

  @Test
  void adminConnectsCounterAndStudentsSeeCountsClosureAndExpiryOnDesktopAndMobile()
      throws Exception {
    String name = "Availability-" + UUID.randomUUID();
    signInAsAdmin();
    page.navigate(url("/admin/events/new"));
    page.fill("[name=name]", name);
    page.fill("[name=description]", "Synthetic availability test");
    page.fill("[name=contact]", "Office");
    page.fill("[name=contactPhone]", "9030000000");
    page.fill("[name=contactEmail]", "office@example.invalid");
    submit();
    Locator row = page.locator("tr").filter(new Locator.FilterOptions().setHasText(name));
    row.getByText("Sites", new Locator.GetByTextOptions().setExact(true)).click();
    String adminList = page.url();
    String eventId = adminList.replaceAll(".*/events/(\\d+)/sites", "$1");
    page.selectOption("[name=registrationMode]", "SITE_FORMS");
    submit();
    page.getByText("New site", new Page.GetByTextOptions().setExact(true)).click();
    page.fill("[name=name]", "Community Pantry");
    page.fill("[name=location]", "Example location, Sherman, TX");
    page.fill("[name=registrationUrl]", "https://forms.gle/availability-synthetic");
    submit();
    page.getByText("Availability", new Page.GetByTextOptions().setExact(true)).click();
    page.getByText("Create / replace connection", new Page.GetByTextOptions().setExact(true))
        .click();
    String key = page.textContent("#sync-site-key"), secret = page.textContent("#sync-secret");
    assertThat(key).isNotBlank();
    assertThat(secret).hasSize(64);
    page.navigate(adminList);
    assertThat(page.textContent("main")).doesNotContain(secret);
    page.click("button:has-text('Sign out')");
    signInAsStudent();
    String studentUrl = url("/student/events/" + eventId);
    page.navigate(studentUrl);
    assertThat(page.locator("[data-availability-label]")).hasText("Availability unavailable");
    publish(key, secret, 5, true, 0);
    page.reload();
    assertThat(page.locator("[data-availability-label]")).hasText("5 of 8 individual spots left");
    assertThat(page.locator("[data-registration-link]")).isVisible();
    assertThat(page.textContent("main")).doesNotContain(secret, key);
    page.setViewportSize(1440, 1000);
    Path screenshots = Paths.get("build/reports/tests/e2eTest");
    Files.createDirectories(screenshots);
    page.screenshot(
        new Page.ScreenshotOptions()
            .setPath(screenshots.resolve("site-availability-desktop.png"))
            .setFullPage(true));
    publish(key, secret, 2, false, 1);
    page.reload();
    assertThat(page.locator("[data-availability-label]")).hasText("Registration closed");
    assertThat(page.locator("[data-registration-link]")).isHidden();
    assertThat(page.locator("[data-registration-blocked]")).isDisabled();
    publish(key, secret, 2, true, 2);
    page.reload();
    assertThat(page.locator("[data-registration-link]")).isVisible();
    publish(key, secret, 0, false, 3);
    page.reload();
    assertThat(page.locator("[data-availability-label]")).hasText("Site full");
    assertThat(page.locator("[data-registration-link]")).isHidden();
    page.setViewportSize(390, 844);
    assertThat((Boolean) page.evaluate("document.documentElement.scrollWidth <= window.innerWidth"))
        .isTrue();
    page.clock().install();
    page.reload();
    page.clock().fastForward(601000);
    assertThat(page.locator("[data-availability-label]"))
        .hasText("Availability may be out of date");
    assertThat(page.locator("[data-registration-link]")).isVisible();
    assertThat(page.locator("[data-registration-link]")).hasText("Check registration form");
    assertThat(page.locator("[data-availability-detail]")).containsText("Last known: 0 of 8");
    page.screenshot(
        new Page.ScreenshotOptions()
            .setPath(screenshots.resolve("site-availability-stale-mobile.png"))
            .setFullPage(true));
    page.setViewportSize(1440, 1000);
    page.click("button:has-text('Sign out')");
    signInAsAdmin();
    page.navigate(url("/admin/events/" + eventId + "/roster"));
    assertThat(page.textContent("main")).contains("No sign-ups yet.");
    page.navigate(url("/admin/events"));
    row.getByText("Delete", new Locator.GetByTextOptions().setExact(true)).click();
    assertThat(row).hasCount(0);
  }
}
