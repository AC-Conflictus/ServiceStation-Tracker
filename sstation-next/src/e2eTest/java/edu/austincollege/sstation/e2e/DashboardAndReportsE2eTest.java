package edu.austincollege.sstation.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TC-111: the admin dashboard and one report of each shape.
 *
 * <p>The charts are the reason this file exists. A {@code <canvas>} that never draws looks
 * identical to a working one from the server's side — the HTML is byte-for-byte the same, so
 * MockMvc cannot tell them apart. TC-119 (the Highcharts → Chart.js swap) is exactly the change
 * that could have shipped a silently blank dashboard, and it was caught then only by reading canvas
 * pixels by hand in a browser. These tests make that check permanent.
 */
@DisplayName("E2E: admin dashboard and reports")
class DashboardAndReportsE2eTest extends E2eTest {

  /**
   * Counts non-transparent pixels on a canvas, waiting out Chart.js's entry animation.
   *
   * <p>Polling rather than a fixed sleep: a sleep long enough to be safe on CI is slow everywhere
   * else, and one short enough to be fast is flaky — the TC-119 verification hit exactly that,
   * reading a mid-animation frame and briefly reporting zero.
   */
  private int paintedPixels(String canvasId) {
    page.waitForFunction(
        "id => {"
            + "  const c = document.getElementById(id);"
            + "  if (!c || !c.width) return false;"
            + "  const d = c.getContext('2d').getImageData(0, 0, c.width, c.height).data;"
            + "  for (let i = 3; i < d.length; i += 4) { if (d[i] !== 0) return true; }"
            + "  return false;"
            + "}",
        canvasId);

    Object painted =
        page.evaluate(
            "id => {"
                + "  const c = document.getElementById(id);"
                + "  const d = c.getContext('2d').getImageData(0, 0, c.width, c.height).data;"
                + "  let n = 0;"
                + "  for (let i = 3; i < d.length; i += 4) { if (d[i] !== 0) n++; }"
                + "  return n;"
                + "}",
            canvasId);
    return ((Number) painted).intValue();
  }

  @Test
  @DisplayName("the dashboard renders its KPIs and all four charts actually draw")
  void dashboardRendersKpisAndCharts() {
    signInAsAdmin();
    page.navigate(url("/admin"));

    // The seeded demo data (fixed Random(42)) always produces hours, so a zero here means the
    // dashboard failed to read them — not that the fixture happened to be empty.
    assertThat(page.textContent("body")).contains("Pending requests");

    for (String canvas :
        new String[] {"chart-5year", "chart-monthly", "chart-classification", "chart-status"}) {
      assertThat(paintedPixels(canvas)).as("painted pixels on #%s", canvas).isGreaterThan(0);
    }
  }

  @Test
  @DisplayName("the dashboard's date-range filter re-renders without breaking the charts")
  void dashboardDateRangeFilterWorks() {
    signInAsAdmin();
    page.navigate(url("/admin?from=2020-01-01&to=2030-12-31"));

    // TC-108e's filter. Ranged mode swaps the KPI labels, and the charts must survive it.
    assertThat(page.textContent("body")).contains("Approved (period)");
    assertThat(paintedPixels("chart-5year")).isGreaterThan(0);
  }

  @Test
  @DisplayName("the year report draws its chart and lists rows")
  void yearReportRendersChartAndTable() {
    signInAsAdmin();
    page.navigate(url("/reports/year"));

    assertThat(paintedPixels("chart")).isGreaterThan(0);
    assertThat(page.locator("table tbody tr").count()).isGreaterThan(0);
  }

  @Test
  @DisplayName("the event report draws its chart and lists rows")
  void eventReportRendersChartAndTable() {
    signInAsAdmin();
    page.navigate(url("/reports/event"));

    assertThat(paintedPixels("chart")).isGreaterThan(0);
    assertThat(page.locator("table tbody tr").count()).isGreaterThan(0);
  }

  @Test
  @DisplayName("the summary report renders as a table with a year selector")
  void summaryReportRendersTable() {
    signInAsAdmin();
    page.navigate(url("/reports/summary"));

    // Table-only by design — no canvas on this one (TC-119 left summary/semester unchanged).
    assertThat(page.locator("canvas").count()).isZero();
    assertThat(page.locator("table tbody tr").count()).isGreaterThan(0);
    // TC-002's year selector, which the Grails original lacked until it was fixed.
    assertThat(page.locator("select[name='year'], input[name='year']").count()).isGreaterThan(0);
  }

  @Test
  @DisplayName("the semester report renders")
  void semesterReportRenders() {
    signInAsAdmin();
    page.navigate(url("/reports/semester"));

    assertThat(page.locator("table tbody tr").count()).isGreaterThan(0);
  }

  @Test
  @DisplayName("a moderator can reach the reports but not the admin dashboard")
  void moderatorSeesReportsButNotDashboard() {
    signInAsModerator();

    page.navigate(url("/reports"));
    assertThat(page.textContent("body")).doesNotContain("You don't have access to that page");

    page.navigate(url("/admin"));
    assertThat(page.textContent("body")).contains("You don't have access to that page");
  }

  @Test
  @DisplayName("every vendored asset the pages need actually loads")
  void vendoredAssetsLoad() {
    // TC-107/TC-038 vendored Bootstrap/jQuery/Chart.js/DataTables into the jar at /webjars/**.
    // Those paths vary by artifact and are easy to get wrong (TC-119 needed Chart.js's UMD build
    // specifically); a 404 there degrades the page silently rather than failing the request.
    List<String> failures = new ArrayList<>();
    page.onResponse(
        response -> {
          if (response.status() >= 400) {
            failures.add(response.status() + " " + response.url());
          }
        });

    signInAsAdmin();
    page.navigate(url("/admin"));
    page.waitForSelector(
        "canvas", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.ATTACHED));
    page.waitForLoadState();

    assertThat(failures).isEmpty();
  }
}
