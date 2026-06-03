package edu.austincollege.sstation.service;

import edu.austincollege.sstation.domain.Status;
import java.time.LocalDateTime;
import java.util.List;

/** DTOs backing the student dashboard and per-student report (TC-105c). */
public final class StudentData {

  private StudentData() {}

  /** One row in the student's hours table. FK-derived names are pre-resolved (null-safe). */
  public record HourRow(
      LocalDateTime startTime,
      double duration,
      Status status,
      String eventName,
      String campusOrgName,
      String communityAgencyName) {}

  /** The student dashboard: headline totals + the student's own hours. */
  public record Dashboard(
      String studentName,
      double grandTotal,
      double approvedTotal,
      double pendingTotal,
      double rejectedTotal,
      List<HourRow> hours) {}

  /** A labelled hours bucket (a semester, or a campus org) in the per-student report. */
  public record Bucket(String label, double hours, int count) {}

  /** Per-student report: approved hours bucketed by semester and by campus org. */
  public record Report(String studentName, List<Bucket> bySemester, List<Bucket> byCampusOrg) {}
}
