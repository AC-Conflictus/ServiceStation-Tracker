package edu.austincollege.sstation.service;

import com.opencsv.CSVWriter;
import edu.austincollege.sstation.service.ReportData.NamedTotal;
import edu.austincollege.sstation.service.ReportData.SemesterReport;
import edu.austincollege.sstation.service.ReportData.SummaryReport;
import edu.austincollege.sstation.service.ReportData.YearKpi;
import edu.austincollege.sstation.service.StudentData.Bucket;
import edu.austincollege.sstation.service.StudentData.Report;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

/**
 * Renders the report DTOs to CSV text for the "Download CSV" buttons (TC-023 / TC-108c). Uses
 * OpenCSV so org/event names containing commas or quotes are escaped per RFC 4180. The
 * multi-section reports (summary, semester, per-student) emit each table separated by a blank row,
 * mirroring what the page shows.
 */
@Service
public class ReportCsvService {

  public String yearKpis(List<YearKpi> kpis) {
    return write(
        w -> {
          w.writeNext(
              new String[] {
                "Year",
                "Total hours",
                "Avg / student",
                "Avg / campus org",
                "Avg / community org",
                "Avg / event"
              });
          for (YearKpi k : kpis) {
            w.writeNext(
                new String[] {
                  Integer.toString(k.year()),
                  num(k.total()),
                  num(k.avgByStudent()),
                  num(k.avgByGroup()),
                  num(k.avgByCommOrg()),
                  num(k.avgByEvent())
                });
          }
        });
  }

  /** Event / community-org / campus-org reports — all share the (name, total) shape. */
  public String namedTotals(String nameHeader, String totalHeader, List<NamedTotal> totals) {
    return write(
        w -> {
          w.writeNext(new String[] {nameHeader, totalHeader});
          for (NamedTotal t : totals) {
            w.writeNext(new String[] {t.name(), num(t.total())});
          }
        });
  }

  public String summary(SummaryReport r) {
    return write(
        w -> {
          w.writeNext(new String[] {"Summary report", "Year", Integer.toString(r.year())});
          w.writeNext(new String[] {"Total approved hours", num(r.totalHours())});
          topSections(w, r.topAgencies(), r.topCampusOrgs(), r.topEvents());
        });
  }

  public String semester(SemesterReport r) {
    return write(
        w -> {
          w.writeNext(
              new String[] {
                "Semester report", "Year", Integer.toString(r.year()), "Semester", r.semester()
              });
          w.writeNext(new String[] {"Total hours", num(r.totalHours())});
          topSections(w, r.topAgencies(), r.topCampusOrgs(), r.topEvents());
        });
  }

  public String studentReport(Report r) {
    return write(
        w -> {
          w.writeNext(new String[] {"Student report", r.studentName()});
          blank(w);
          buckets(w, "By semester", r.bySemester());
          blank(w);
          buckets(w, "By campus org", r.byCampusOrg());
        });
  }

  // ----- helpers -----

  private void topSections(
      CSVWriter w,
      List<NamedTotal> agencies,
      List<NamedTotal> campusOrgs,
      List<NamedTotal> events) {
    blank(w);
    namedTotalSection(w, "Top community agencies", agencies);
    blank(w);
    namedTotalSection(w, "Top campus orgs", campusOrgs);
    blank(w);
    namedTotalSection(w, "Top events", events);
  }

  private void namedTotalSection(CSVWriter w, String title, List<NamedTotal> totals) {
    w.writeNext(new String[] {title, "Hours"});
    for (NamedTotal t : totals) {
      w.writeNext(new String[] {t.name(), num(t.total())});
    }
  }

  private void buckets(CSVWriter w, String title, List<Bucket> buckets) {
    w.writeNext(new String[] {title, "Hours", "Entries"});
    for (Bucket b : buckets) {
      w.writeNext(new String[] {b.label(), num(b.hours()), Integer.toString(b.count())});
    }
  }

  private static void blank(CSVWriter w) {
    w.writeNext(new String[] {""});
  }

  /** Trims a trailing {@code .0} so whole-number totals read cleanly in the spreadsheet. */
  private static String num(double value) {
    return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
  }

  private static String write(Consumer<CSVWriter> body) {
    StringWriter sw = new StringWriter();
    try (CSVWriter w = new CSVWriter(sw)) {
      body.accept(w);
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Failed to render CSV", e);
    }
    return sw.toString();
  }
}
