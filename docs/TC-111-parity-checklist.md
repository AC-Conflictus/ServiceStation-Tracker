# TC-111 — Feature parity checklist (Grails → Spring Boot rewrite)

**Status:** draft for stakeholder review · last updated 2026-09-11

## What this is, and why it isn't an HTML diff

TC-111 originally asked for a side-by-side HTML diff against the Grails app. That is no longer
achievable and should not be attempted: TC-107 restyled every page to Bootstrap 5, and TC-108 added
seven flows the Grails app never had. There is no comparable page left to diff.

Parity is therefore established by **feature coverage**: every user-visible capability of the Grails
app is listed below with its equivalent in `sstation-next`, and — where one exists — the end-to-end
test that proves it works in a real browser.

**How to read the coverage column:**

| Symbol | Meaning |
|---|---|
| 🟢 | Covered by the Playwright E2E suite (`sstation-next/src/e2eTest`), which drives a real browser against the Docker stack on Postgres |
| 🔵 | Covered by the JUnit suite (service/controller level) but not by a browser test |
| 🔴 | **Not ported** — see [Gaps](#gaps) |

---

## Authentication and access control

| Grails capability | Rewrite equivalent | Coverage |
|---|---|---|
| Three roles (admin / moderator / student) | Spring Security 6, `User`/`Role`/`UserRole` (TC-104) | 🟢 `AuthAndRoutingE2eTest` |
| Form login / logout | Same, BCrypt via delegating encoder | 🟢 sign-in and sign-out both asserted |
| Role-based landing page | `HomeController` routes admin→`/admin`, student→`/student`, others→landing | 🟢 all three roles |
| `@Secured` controller gating | `@PreAuthorize` + `@EnableMethodSecurity` | 🟢 student denied `/admin`, moderator denied `/admin` |
| — (Grails had no CSRF) | CSRF on by default (TC-018) | 🔵 |

## Dashboards and reports

| Grails capability | Rewrite equivalent | Coverage |
|---|---|---|
| Admin dashboard KPIs | `StatsService` + `/admin` (TC-105) | 🟢 `DashboardAndReportsE2eTest` |
| Charts: by classification, by status, by year | Four Chart.js canvases (TC-107, TC-119) | 🟢 asserted to actually paint pixels, not merely exist |
| Summary report (+ year selector) | `/reports/summary` (TC-105; year selector was TC-002) | 🟢 |
| Report by year | `/reports/year` | 🟢 chart + table |
| Report by semester (Fall/Janterm/Spring/Summer) | `/reports/semester` | 🟢 |
| Report per event | `/reports/event` | 🟢 chart + table |
| Report per community agency | `/reports/community-org` | 🔵 same shape as `/reports/event` |
| Report per campus org | `/reports/campus-org` | 🔵 same shape as `/reports/event` |
| Per-student report view | `/student/report` (TC-105) | 🔵 |

## Service hours

| Grails capability | Rewrite equivalent | Coverage |
|---|---|---|
| Admin hour list (all + pending) | `/admin/hours`, `/admin/hours/pending` (TC-106) | 🟢 |
| Inline approve / reject | `POST /admin/hours/{id}/status`, JSON (TC-106) | 🟢 round trip asserted, badge updates in place |
| Quick status AJAX dialog (`ajaxUpdateStatus`) | Replaced by the JSON endpoint above | 🟢 |
| Admin-only status changes | `@PreAuthorize` — moderators are not offered the control | 🟢 asserted for moderators |
| — (Grails had no audit trail) | `ServiceHourAuditLog`, written on every change (TC-106c) | 🟢 audit entry asserted after approve |
| Create / edit a student's hours | `/admin/hours/new`, `/admin/hours/{id}/edit` | 🔵 |

## CRUD on reference data

| Grails capability | Rewrite equivalent | Coverage |
|---|---|---|
| CRUD students | `/admin/students` (TC-106) | 🔵 |
| **Bulk student CSV import** | `/admin/students/import` (TC-123) | 🟢 upload, bad-row reporting, re-import, and the permission change |
| CRUD events | `/admin/events` | 🟢 full create → edit → delete cycle |
| CRUD campus orgs (`ACGroupController`) | `/admin/campus-orgs` | 🔵 same controller shape as events |
| CRUD community agencies (`CommOrgController`) | `/admin/agencies` | 🔵 same controller shape as events |
| Promote / demote moderators | `/admin/moderators`, ADMIN-only (TC-106) | 🔵 |
| — (Grails cascade-deleted) | Deleting reference data **detaches** it from hours instead | 🔵 |
| Form validation | `@Valid` + `BindingResult` re-render with field errors | 🟢 blank event form asserted to re-render with errors |

## Email

| Grails capability | Rewrite equivalent | Coverage |
|---|---|---|
| `mail` plugin wiring — **credentials blank, non-functional** | `NotificationService` (TC-108a), env-driven SMTP | 🔵 |

> The Grails app could never actually send mail: the plugin credentials in `Config.groovy` were
> never populated (TC-021). The rewrite sends real mail once `SSTATION_MAIL_HOST` is set, and logs
> instead of failing when it isn't. This is an improvement, not a regression — but note that mail is
> still silent until AC IT supplies a relay.

## Net-new in the rewrite (no Grails equivalent)

These have no parity obligation; they are listed so the reviewer knows what is new.

| Capability | Card | Coverage |
|---|---|---|
| Bulk approve / reject | TC-108b | 🔵 |
| CSV export on all six reports + student report | TC-108c | 🟢 all six + student CSV |
| PDF export of the student report | TC-108d | 🟢 magic bytes checked, not just content type |
| Date-range filter on the dashboard | TC-108e | 🟢 |
| Student event sign-up + admin roster | TC-108f | 🟢 sign-up flow |
| Self-service password reset | TC-108g | 🔵 |
| In-app password change | TC-121 | 🔵 |
| Custom 403/404/500 error pages | TC-120 | 🟢 403 asserted for two roles |
| Day-one admin bootstrap | TC-121 | 🔵 Postgres integration test |

---

## Gaps

### ✅ Student CSV import — was missing, now ported (TC-123)

This checklist originally found one genuine feature regression: the Grails bulk student import
(`AcStudentController.upload()` → `StudentService.importStudents()`) had no equivalent in
`sstation-next`. **It has since been built** — `/admin/students/import`, reachable from the students
list, with the same column layout, the same add-or-update-by-student-ID semantics, and the same
"unknown classification becomes OTHER" behaviour.

Two deliberate differences, both improvements:

- **Invalid rows are reported rather than dropped in silence.** Grails counted only successes, so a
  file with a dozen malformed rows looked like a clean import. Skipped rows now come back with their
  line number and the reason.
- **Students can no longer do it.** The Grails controller's class-level `@Secured` included
  `ROLE_STUDENT`, so any signed-in student could overwrite the entire roster. It is now ADMIN +
  MODERATOR, matching who can already manage students individually.

> ⚠️ **One thing still needs the office's eyes.** The column layout was matched against the *Grails
> source*, not against a real registrar export — nobody on this project has seen the actual file.
> The importer expects, zero-indexed: `0` student ID, `1` **ignored**, `2` first name, `3` last name,
> `4` status, `5` AC box, `6` classification, `7` **ignored**, `8` email, with the first row treated
> as a header. **Please import one real export and confirm the columns land in the right fields** —
> a layout mismatch imports plausible-looking garbage rather than failing, which is the worst way for
> this to be wrong. The result page makes this easy to eyeball.

No other Grails capability is missing.

---

## Stakeholder sign-off

The E2E suite proves the flows work. It cannot prove they work *the way the office needs them to* —
that requires a person who does this job clicking through it.

**Before sign-off:**

- [ ] A Service Station staff member clicks through the demo (see [DEPLOY.md](../DEPLOY.md))
- [ ] **One real registrar CSV is imported** and the columns are confirmed to land in the right
      fields (see the warning above — this is the one thing that cannot be verified without a real
      file)
- [ ] Anything they flag is either fixed or recorded as a known difference

**Signed off by:** _______________  **Date:** ___________
