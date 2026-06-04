package edu.austincollege.sstation.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.austincollege.sstation.service.ReportData.NamedTotal;
import edu.austincollege.sstation.service.ReportData.YearKpi;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit coverage for {@link ReportCsvService} (TC-108c) — structure and RFC-4180 escaping. */
class ReportCsvServiceTest {

  private final ReportCsvService csv = new ReportCsvService();

  @Test
  void namedTotalsWritesHeaderAndRows() {
    String out = csv.namedTotals("Event", "Hours", List.of(new NamedTotal("Great Day", 12.0)));
    assertThat(out).contains("\"Event\",\"Hours\"");
    assertThat(out).contains("\"Great Day\",\"12\"");
  }

  @Test
  void escapesNamesContainingCommasAndQuotes() {
    // A name with a comma must stay one field; an embedded quote must be doubled (RFC 4180).
    String out =
        csv.namedTotals("Org", "Hours", List.of(new NamedTotal("Boys, Girls \"Club\"", 3.5)));
    assertThat(out).contains("\"Boys, Girls \"\"Club\"\"\",\"3.5\"");
  }

  @Test
  void yearKpisHasOneRowPerYearPlusHeader() {
    String out =
        csv.yearKpis(List.of(new YearKpi(2026, 10, 1, 2, 3, 4), new YearKpi(2025, 20, 5, 6, 7, 8)));
    long lines = out.lines().count();
    assertThat(lines).isEqualTo(3); // header + 2 data rows
    assertThat(out).contains("\"Year\"");
  }
}
