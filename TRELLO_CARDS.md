# Trello Cards — Service Station Web App

A prioritized backlog of cards to copy/paste into Trello. The goal: get the project as close to "ready to hand off to Austin College IT" as possible, leaving clearly marked placeholders for things only AC IT can supply (SMTP creds, prod DB, domain name, SSL cert, server hardening). A secondary deliverable is a public AWS demo deployment so reviewers can click through the app without a local Grails 2.4.4 install.

Cards are grouped by lane and ordered by priority within each lane. Each card has: **Title**, **Why**, **Acceptance criteria**, **Notes / files to touch**, **Estimate** (S = <½ day, M = ½–2 days, L = 2–5 days, XL = >5 days).

Legend:
- ⛔ **Blocker** — breaks the app on a fresh run or fresh data.
- ⚠️ **Bug** — wrong behavior, but app still runs.
- 🔒 **Security**
- 🧹 **Quality / tech debt**
- ✨ **Feature**
- ☁️ **Infra / DevOps**
- 📝 **Docs**
- 🏫 **AC IT placeholder** — needs Austin College IT to fill in a value/credential before going to prod.

---

## Lane 1 — Critical fixes (do these first)

These cards make the app *actually work* on a fresh checkout against fresh data. Until they're done, the dashboard is misleading and the student login is broken.

### TC-001 ⛔ Replace hardcoded `currentYear = 2015` in HourService
- **Status:** ✅ **Landed 2026-05-29** (branch `lane-01-critical-fixes`).
- **Why:** [HourService.groovy:27](sstation/grails-app/services/sstation/HourService.groovy#L27) hardcodes the year. Every "this year" KPI on the admin dashboard (totals, by-classification chart, by-status chart) silently filters to 2015, so on a 2026 run the dashboard shows zero hours.
- **Acceptance criteria:**
  - `HourService.init()` derives `currentYear` from `Calendar.getInstance().get(Calendar.YEAR)`.
  - Admin dashboard shows non-zero "current year" numbers against the BootStrap seed data (seed dates may also need adjusting — see TC-004).
  - Existing `HourServiceSpec` (if any) still passes; add one if missing.
- **Files:** [HourService.groovy](sstation/grails-app/services/sstation/HourService.groovy).
- **Estimate:** S.

### TC-002 ⛔ Replace hardcoded `year = 2016` in ReportsController.summaryReport
- **Status:** ✅ **Landed 2026-05-29** (branch `lane-01-critical-fixes`).
- **Why:** [ReportsController.groovy:50](sstation/grails-app/controllers/sstation/ReportsController.groovy#L50) hardcodes `year = 2016`. The summary report is permanently stuck on 2016.
- **Acceptance criteria:**
  - `summaryReport` defaults to the current year and accepts an optional `year` param to view past years.
  - Summary page renders non-zero totals against seed data.
- **Files:** [ReportsController.groovy](sstation/grails-app/controllers/sstation/ReportsController.groovy), [summaryReport.gsp](sstation/grails-app/views/reports/summaryReport.gsp) (add a year selector).
- **Estimate:** S.

### TC-003 ⛔ Fix `IndexOutOfBoundsException` risk in summaryReport / semesterReport
- **Status:** ✅ **Landed 2026-05-29** (branch `lane-01-critical-fixes`).
- **Why:** Both methods loop `for (int i = 0; i < constant; i++)` where `constant = 5` and call `allAgs.get(i)`, `allOrgs.get(i)`, `allEvs.get(i)`. If any of those lists has fewer than 5 entries (very likely with real-world data, possible even with seed data), the report 500s.
- **Acceptance criteria:**
  - Replace `constant = 5` with `Math.min(5, allAgs.size())`, similarly for orgs/events.
  - Manual test: delete entries until each list has 2 items, hit summary and semester reports — no 500.
- **Files:** [ReportsController.groovy:48-93](sstation/grails-app/controllers/sstation/ReportsController.groovy#L48-L93), [ReportsController.groovy:95-163](sstation/grails-app/controllers/sstation/ReportsController.groovy#L95-L163).
- **Estimate:** S.

### TC-004 ⛔ Seed data uses Java legacy Date constructor — dates land in years 2011–2015
- **Status:** ✅ **Landed 2026-05-29** (branch `lane-01-critical-fixes`).
- **Why:** [BootStrap.groovy:158](sstation/grails-app/conf/BootStrap.groovy#L158) does `def year = 111 + random.nextInt(5)` then `new Date(year, month, date, ...)`. `new Date(int year, ...)` uses `year + 1900`, so seed `starttime` values fall in 2011–2015. Combined with TC-001/TC-002, this is why nothing shows up on the current-year dashboard.
- **Acceptance criteria:**
  - Seed service hours have `starttime` distributed across the last 5 calendar years ending today.
  - Use `Calendar.getInstance()` + `add(Calendar.DAY_OF_YEAR, -random.nextInt(5*365))` instead of `new Date(year,...)`.
- **Files:** [BootStrap.groovy](sstation/grails-app/conf/BootStrap.groovy).
- **Estimate:** S.

### TC-005 ⛔ Seed an `AcStudent` for the `student` test login
- **Status:** ✅ **Landed 2026-05-29** (branch `lane-01-critical-fixes`). Also fixed the hardcoded `id:1` redirect in `HomeController` as part of this card.
- **Why:** [HomeController.groovy:41](sstation/grails-app/controllers/sstation/HomeController.groovy#L41) looks up the student by `AcStudent.findByAcEmail(username + "@austincollege.edu")`. The `student` user is seeded but no matching `AcStudent` is. Result: logging in as `student` lands on a broken redirect.
- **Acceptance criteria:**
  - BootStrap creates one deterministic `AcStudent` with `acEmail = "student@austincollege.edu"` (plus a known acid / firstname / lastname / classification) and attaches a handful of `ServiceHour` records spanning several statuses.
  - `student` / `student_secret` login lands on a populated student dashboard.
- **Files:** [BootStrap.groovy](sstation/grails-app/conf/BootStrap.groovy).
- **Estimate:** S.
- **Depends on:** TC-004 (otherwise the student's hours will still all be in the past).

### TC-006 ⚠️ Null-safe access in ReportsController.semesterReport
- **Status:** ✅ **Landed 2026-05-29** (branch `lane-01-critical-fixes`).
- **Why:** `semesterReport` dereferences `s.commAg.name`, `s.event.name`, `s.campusOrg.name` ([ReportsController.groovy:144-153](sstation/grails-app/controllers/sstation/ReportsController.groovy#L144-L153)), but all three are declared `nullable:true` in [ServiceHour.groovy:25-37](sstation/grails-app/domain/sstation/ServiceHour.groovy#L25-L37). A single hour record without one of these will NPE the whole report.
- **Acceptance criteria:**
  - All three uses guard with `?.`.
  - Add a unit test that inserts a `ServiceHour` with null `commAg` and asserts the report still renders.
- **Files:** [ReportsController.groovy](sstation/grails-app/controllers/sstation/ReportsController.groovy), [ReportsControllerSpec.groovy](sstation/test/unit/sstation/ReportsControllerSpec.groovy).
- **Estimate:** S.

### TC-007 ⛔ Gate `BootStrap.init` to non-production environments
- **Status:** ✅ **Landed 2026-05-29** (branch `lane-01-critical-fixes`).
- **Why:** `BootStrap.init` runs in every environment, including production. On every prod boot it tries to recreate the three test users and ~100 random students with the same passwords (`admin_secret`, etc.). At best the asserts fail and the app refuses to start; at worst (production with `dbCreate = "update"`) the seeded test users are recreated alongside real ones, creating a permanent admin backdoor.
- **Acceptance criteria:**
  - Wrap the random data seeding in `if (Environment.current != Environment.PRODUCTION)`.
  - In production, only ensure the three `AcRole` rows exist (ROLE_ADMIN, ROLE_STUDENT, ROLE_MODERATOR). No `AcUser`, no test data.
  - Remove the `assert AcUser.count() == 3` lines or move them inside the dev branch.
- **Files:** [BootStrap.groovy](sstation/grails-app/conf/BootStrap.groovy).
- **Estimate:** S.
- **Dependency for prod cutover:** must land before any AWS / AC IT deploy.

### TC-008 🔒 Remove plaintext password fallback in AcUser.encodePassword
- **Status:** ✅ **Landed 2026-05-29** (branch `lane-01-critical-fixes`).
- **Why:** [AcUser.groovy:52](sstation/grails-app/domain/sstation/AcUser.groovy#L52) reads `springSecurityService?.passwordEncoder ? springSecurityService.encodePassword(password) : password`. The trailing `: password` means if `springSecurityService` is null (or the encoder bean isn't wired yet, e.g. early in BootStrap), passwords get stored as plaintext. This is exactly when you'd notice it least — early boot — and once stored plaintext, the bcrypt check on login silently fails.
- **Acceptance criteria:**
  - If `springSecurityService` is null, throw `IllegalStateException("springSecurityService not wired — cannot save AcUser")` instead of falling back.
  - Verify all three seeded users still log in after the change.
- **Files:** [AcUser.groovy](sstation/grails-app/domain/sstation/AcUser.groovy).
- **Estimate:** S.

---

## Lane 2 — Bug fixes and small quality wins

### TC-009 ⚠️ Add `AcStudent` FK on `AcUser` and remove email-string hack
- **Why:** The coupling between login identity and student record is currently a string match: `username + "@austincollege.edu"`. The TODO comment in [HomeController.groovy:39-41](sstation/grails-app/controllers/sstation/HomeController.groovy#L39-L41) explicitly calls this out. Any student whose AC email doesn't follow the pattern, or who has a typo'd email in the DB, can't see their dashboard.
- **Acceptance criteria:**
  - Add a nullable `AcStudent acStudent` field on `AcUser` (or vice-versa — pick one direction and document it).
  - Backfill on BootStrap by linking the seed `student` user (TC-005) to the seed `AcStudent`.
  - `HomeController.index` looks up student by FK, not email.
  - Add a Grails migration script under `grails-app/migrations/` rather than relying on `dbCreate`.
- **Files:** [AcUser.groovy](sstation/grails-app/domain/sstation/AcUser.groovy), [AcStudent.groovy](sstation/grails-app/domain/sstation/AcStudent.groovy), [HomeController.groovy](sstation/grails-app/controllers/sstation/HomeController.groovy), [BootStrap.groovy](sstation/grails-app/conf/BootStrap.groovy).
- **Estimate:** M.

### TC-010 🧹 Convert ReportsController helpers to proper Grails services
- **Why:** `CampusOrgReportService`, `CommAgReportService`, `EventReportService` are instantiated with `new` inside `summaryReport` / `semesterReport` and carry per-request mutable state. They're services in name only. This makes them hard to test and prevents Spring from managing transactions.
- **Acceptance criteria:**
  - Refactor each into a stateless service injected into `ReportsController`. The target entity and hour list become method parameters, not constructor args.
  - All report pages still render identically.
- **Files:** the three services under [grails-app/services/sstation/](sstation/grails-app/services/sstation/), [ReportsController.groovy](sstation/grails-app/controllers/sstation/ReportsController.groovy).
- **Estimate:** M.

### TC-011 ⚠️ Reports re-iterate `ServiceHour.list()` inside nested loops
- **Why:** `commOrgReport` and `campusOrgReport` call `ServiceHour.list()` inside a `for(orgList)` loop. With N orgs and M hours that's N×M scans. Trivially fixable with one `groupBy` pass. Hits performance once data crosses a few hundred rows.
- **Acceptance criteria:**
  - Build a single `Map<String, Double>` of org-name → total hours, then look up per org.
  - Same render output.
- **Files:** [ReportsController.groovy:165-195](sstation/grails-app/controllers/sstation/ReportsController.groovy#L165-L195).
- **Estimate:** S.

### TC-012 🧹 Promote `Status` and `Classification` enums to Groovy and co-locate with domain
- **Why:** They live under [src/java/sstation/](sstation/src/java/sstation/) as Java files. Grails 2.4 handles them fine, but moving to `grails-app/domain/sstation/` (or `src/groovy`) reduces the build's surface area and removes the awkward "Java sub-source-set" — useful when we eventually port off Grails 2.
- **Acceptance criteria:** Enums moved, all references compile, `grails test-app` passes.
- **Estimate:** S.

### TC-013 ⚠️ `ServiceHour.status` constraint says `blank:false` on an enum
- **Why:** [ServiceHour.groovy:33](sstation/grails-app/domain/sstation/ServiceHour.groovy#L33) declares `status(nullable:false,blank:false)`. `blank` is meaningful for strings, not for enum-typed fields — it's silently ignored by GORM. Either it's a no-op (cosmetic) or the original author intended a string field. Confirm intent, fix the constraint.
- **Acceptance criteria:** Constraint cleaned up; nothing else changes.
- **Estimate:** S.

### TC-014 🧹 Remove duplicate `otherCommAg` declaration in ServiceHour constraints
- **Why:** [ServiceHour.groovy:31-36](sstation/grails-app/domain/sstation/ServiceHour.groovy#L31-L36) declares `otherCommAg` twice in the constraints block. Likely a merge artifact. Harmless but smelly.
- **Estimate:** S.

### TC-015 🧹 Rename `selinium_tests/` → `selenium_tests/`
- **Why:** Typo in directory name. Either fix the spelling, or — better — delete the directory and replace it with modern Selenium WebDriver / Playwright tests (see TC-024).
- **Estimate:** S.

### TC-016 🧹 Spring Security plugin is on a release candidate (2.0-RC5)
- **Why:** [BuildConfig.groovy:65](sstation/grails-app/conf/BuildConfig.groovy#L65) pins `spring-security-core:2.0-RC5`. Upgrade to the final 2.0.0 (last release compatible with Grails 2.4.x) — RC5 has known bugs that were fixed in the final release.
- **Acceptance criteria:** Plugin upgraded, all three logins still work, `grails test-app` passes.
- **Estimate:** S.

### TC-033 ☁️ Containerize the app (Dockerfile + docker-compose) — **Grails only**
- **Status:** ⏸️ **Parked** on Lane 7 path — use [TC-113](#tc-113-) for `sstation-next/`. Only pursue this card if AC IT forces a Grails handoff (Phase 1B).
- **Why:** Even though AC IT will probably deploy to a VM, a working `docker compose up` is the fastest "does this run?" smoke test for any reviewer, and the same image can drive the AWS demo if we move off Beanstalk later (ECS Fargate, AppRunner, etc.).
- **Acceptance criteria:**
  - `Dockerfile` based on `tomcat:8-jre8` (or Corretto 8 base), `COPY target/sstation-*.war /usr/local/tomcat/webapps/sstation.war`.
  - `docker-compose.yml` brings up `app` + `postgres:13` with seeded data.
  - One-command demo: `docker compose up` → `http://localhost:8080/sstation`.
  - Documented in a new `DEPLOY.md`.
- **Depends on:** TC-029 (Postgres migration) for a fully wired compose stack; can ship a minimal H2-only compose file first as a dev convenience.
- **Superseded by:** [TC-113](#tc-113-) (Spring Boot rewrite).
- **Estimate:** M.

---

## Lane 3 — Security hardening (before any public deploy)

### TC-017 🔒 Move seed passwords out of source control
- **Why:** `admin_secret` / `student_secret` / `moderator_secret` are committed in [BootStrap.groovy](sstation/grails-app/conf/BootStrap.groovy). Fine for a dev seed, but the file currently runs in prod (see TC-007). Even after TC-007 lands, leaving the strings hardcoded means anyone who reads the repo knows the dev passwords — and dev DBs sometimes accidentally get exposed.
- **Acceptance criteria:**
  - Read dev seed passwords from environment variables with a default of `changeme-{role}` and a startup log line saying "using default dev credentials — set `SSTATION_DEV_ADMIN_PASSWORD` to override".
  - Document in [CLAUDE.md](CLAUDE.md) and the AC IT runbook (TC-035).
- **Estimate:** S.

### TC-018 🔒 Enable CSRF protection on state-changing endpoints
- **Why:** Spring Security Core plugin 2.0 doesn't enable CSRF by default. `ajaxUpdateStatus`, all CRUD save actions, and the moderator promote/demote endpoint are vulnerable.
- **Acceptance criteria:**
  - Add `<g:set var="org.codehaus.groovy.grails.SYNCHRONIZER_TOKEN_URI" .../>` (or the equivalent useToken: true on forms) for non-AJAX forms.
  - For AJAX endpoints (`ajaxUpdateStatus`), require a CSRF header or move to a same-origin POST with a token rendered into the page.
- **Estimate:** M.

### TC-019 🔒 Audit `@Secured` annotations on every controller action
- **Why:** Some actions inherit class-level `@Secured(['ROLE_ADMIN','ROLE_STUDENT','ROLE_MODERATOR'])`. `AcStudentController` notably mixes student-self-service actions and admin actions under one wide annotation. A student could potentially hit `delete` on another student's record if the action doesn't re-check ownership.
- **Acceptance criteria:**
  - Inventory every action across all controllers; document the *intended* role(s).
  - Tighten `@Secured` per-action where the class-level annotation is too permissive.
  - Add a `before` interceptor that asserts the current user matches the `AcStudent` they're editing.
- **Files:** all controllers under [grails-app/controllers/sstation/](sstation/grails-app/controllers/sstation/).
- **Estimate:** M.

### TC-020 🔒 H2 version pinned at Grails 2.4.4 default has known CVEs
- **Why:** Grails 2.4.4 ships H2 ~1.3.x which has the H2-2022-23221 RCE among others. Even in dev this is sketchy if the H2 web console is exposed. In prod (file-based H2) it's a hard no — see TC-029 for Postgres migration.
- **Acceptance criteria:**
  - Confirm H2 web console is disabled (`grails.dbconsole.enabled = false` in [Config.groovy](sstation/grails-app/conf/Config.groovy) for non-dev).
  - Pin H2 to the latest 1.4.x patch the Grails 2.4.4 plugin set will tolerate, OR migrate to Postgres (TC-029).
- **Estimate:** S (after Postgres migration this becomes a non-issue).

---

## Lane 4 — Features (paper-form-replacement parity)

> **⚠️ This entire lane is delivered — but in `sstation-next/`, not the Grails app.** All eight cards below
> shipped as **TC-108a–g** (plus the audit log in TC-106c) on 2026-06-03. They are kept here for traceability;
> **do not implement any of them in `sstation/`.** See [TC-108](#tc-108--port--implement-the-lane-4-features-in-the-new-stack--landed-2026-06-03).

These bring the app to "we can actually replace the paper form" — the original project pitch.

### TC-021 ✨ Email notifications on approve / reject
- **Status:** ✅ **Delivered in the rewrite** as **TC-108a** (`NotificationService` + Spring Mail, `templates/email/status-change.html`). This Grails-targeted card is superseded — do not implement in `sstation/`.
- **Why:** The `mail` plugin is wired in [Config.groovy:62](sstation/grails-app/conf/Config.groovy#L62), but SMTP credentials are empty and no controller actually calls `mailService.sendMail`. The README explicitly promises this.
- **Acceptance criteria:**
  - `HomeController.ajaxUpdateStatus` (and the equivalent in `HourController`) sends an email to the affected `AcStudent.acEmail` on transitions to APPROVED or REJECTED.
  - Email template is a GSP under `grails-app/views/email/`.
  - SMTP host / port / username / password / from-address are read from environment variables with the placeholders 🏫 in TC-035.
  - In dev (no creds set) the call is a no-op with a `log.info` line.
- **Files:** [Config.groovy](sstation/grails-app/conf/Config.groovy), [HomeController.groovy](sstation/grails-app/controllers/sstation/HomeController.groovy), [HourController.groovy](sstation/grails-app/controllers/sstation/HourController.groovy), new `views/email/*.gsp`.
- **Estimate:** M.

### TC-022 ✨ Bulk approve / reject from the pending queue
- **Status:** ✅ **Delivered in the rewrite** as **TC-108b** (`HourController` bulk endpoint + checkbox UI on `hours/list.html`). Superseded for the Grails app.
- **Why:** The pending queue today only supports one-at-a-time approval via the AJAX dialog. For an office processing 100+ paper forms a week, this is a non-starter.
- **Acceptance criteria:**
  - Checkbox column on the pending table.
  - Toolbar "Approve selected" / "Reject selected" buttons that POST a list of IDs.
  - Single audit log entry per batch (see TC-027).
- **Files:** [hour/pending.gsp](sstation/grails-app/views/hour/pending.gsp), [HourController.groovy](sstation/grails-app/controllers/sstation/HourController.groovy).
- **Estimate:** M.

### TC-023 ✨ CSV export on every report page
- **Status:** ✅ **Delivered in the rewrite** as **TC-108c** (`ReportCsvService` via OpenCSV, download link on all six reports + the student report). Superseded for the Grails app.
- **Why:** The `csv` plugin (`org.grails.plugins:csv:0.3.1`) is already in [BuildConfig.groovy:66](sstation/grails-app/conf/BuildConfig.groovy#L66) but unused. The actual paper-form workflow ends with "give me a spreadsheet."
- **Acceptance criteria:**
  - "Download CSV" button on summaryReport, semesterReport, yearReport, eventReport, commOrgReport, campusOrgReport, and per-student report.
  - One controller action per report serves `text/csv` with a sensible filename (`summary_2026.csv`, `student_AC50012_hours.csv`).
- **Files:** [ReportsController.groovy](sstation/grails-app/controllers/sstation/ReportsController.groovy), [AcStudentController.groovy](sstation/grails-app/controllers/sstation/AcStudentController.groovy).
- **Estimate:** M.

### TC-024 ✨ PDF export of a student's per-semester report
- **Status:** ✅ **Delivered in the rewrite** as **TC-108d** (`StudentReportPdfService` via openhtmltopdf + jsoup). Superseded for the Grails app.
- **Why:** Direct paper-form replacement: students need a printable record to attach to applications.
- **Acceptance criteria:**
  - "Download PDF" button on the per-student report.
  - Uses the `rendering` plugin or generates HTML and pipes through `wkhtmltopdf` (decide during the card — note the choice in the PR).
  - PDF includes: student name, AC ID, classification, year, table of approved hours with totals per semester and grand total.
- **Estimate:** M.

### TC-025 ✨ Date-range filter on the admin dashboard
- **Status:** ✅ **Delivered in the rewrite** as **TC-108e** (`StatsService` date-range overload + filter controls on `admin/home.html`). Superseded for the Grails app.
- **Why:** "Current year" is too rigid. Office staff need "last 30 days", "this semester", "custom range" for grant reporting.
- **Acceptance criteria:**
  - Date pickers on the admin dashboard.
  - All KPIs and charts respond to the range.
  - URL is shareable (`?from=2026-01-01&to=2026-05-25`).
- **Estimate:** M.

### TC-026 ✨ Service-event sign-up flow
- **Status:** ✅ **Delivered in the rewrite** as **TC-108f** (`EventSignup` + `SignupStatus` + Flyway `V3`, student `/student/events` self-signup, admin roster). Superseded for the Grails app.
- **Why:** README promises "post service events and recruit students to participate", but no controller supports student → event sign-up. Today a student can only log hours *after* the fact.
- **Acceptance criteria:**
  - New `EventSignup` domain (`AcStudent`, `Event`, `signupTime`, `status` enum: SIGNED_UP / ATTENDED / NO_SHOW).
  - Student view: list of upcoming events with "Sign up" button.
  - Admin view: roster per event; one-click "convert attended sign-ups to ServiceHour records".
- **Estimate:** L.

### TC-027 ✨ Audit log on every ServiceHour status change
- **Status:** ✅ **Delivered in the rewrite** as part of **TC-106c** (`ServiceHourAuditLog` + Flyway `V2` + `AuditService`, admin-only per-hour view). Superseded for the Grails app.
- **Why:** "Who approved my hours?" is a real question. Today there's no record.
- **Acceptance criteria:**
  - New `ServiceHourAuditLog` domain (`serviceHour`, `actor` (`AcUser`), `fromStatus`, `toStatus`, `timestamp`, `note`).
  - Every status mutation writes an entry.
  - Admin-only view that lists the log per ServiceHour.
- **Estimate:** M.

### TC-028 ✨ Student self-service password reset
- **Status:** ✅ **Delivered in the rewrite** as **TC-108g** (`PasswordResetService`, SHA-256-hashed single-use tokens, 1-hour TTL, no user enumeration). Superseded for the Grails app.
- **Why:** No way to reset a password today. Every forgotten password is a manual DB poke.
- **Acceptance criteria:**
  - "Forgot password" link on login page → enter AC email → email with a single-use token (depends on TC-021 for SMTP).
  - Token valid for 1 hour, single-use, stored hashed.
- **Estimate:** M.

---

## Lane 5 — Infra / DevOps (this is where AC IT picks up)

This is the "leave it bow-tied for IT" track. Goal: anyone at AC IT with a Linux box and an hour to spare can deploy this.

### TC-029 ☁️ Migrate prod DataSource from H2 to PostgreSQL
- **Why:** H2 file-mode is fine for a class project, not for an office that processes hundreds of records a year. Postgres is what AC IT almost certainly already runs.
- **Acceptance criteria:**
  - Add `runtime 'org.postgresql:postgresql:9.4-1206-jdbc41'` (latest version compatible with JDK 8 / Grails 2.4.4 — pin carefully) to [BuildConfig.groovy](sstation/grails-app/conf/BuildConfig.groovy).
  - `DataSource.groovy` production block reads `jdbcUrl`, `username`, `password` from environment variables 🏫:
    - `SSTATION_DB_URL` (e.g. `jdbc:postgresql://db.austincollege.edu:5432/sstation`)
    - `SSTATION_DB_USER`
    - `SSTATION_DB_PASSWORD`
  - `dbCreate = "validate"` in prod — schema is managed by `database-migration` plugin migrations, not GORM auto-DDL.
  - Initial migration generated via `grails dbm-generate-gorm-changelog`.
  - Dev keeps H2 in-memory; test keeps H2 in-memory.
- **Files:** [BuildConfig.groovy](sstation/grails-app/conf/BuildConfig.groovy), [DataSource.groovy](sstation/grails-app/conf/DataSource.groovy), new `grails-app/migrations/changelog.groovy`.
- **Estimate:** L.

### TC-030 ☁️ Externalize all environment-specific config
- **Why:** Right now `serverURL` is missing, mail creds are empty, DB creds are empty. All of these should come from env vars or a config file Grails loads from outside the WAR.
- **Acceptance criteria:**
  - [Config.groovy](sstation/grails-app/conf/Config.groovy) uses `grails.config.locations = ["file:${System.properties['catalina.base']}/conf/sstation-config.groovy"]` (or a Tomcat-friendly equivalent) plus env var overrides.
  - A documented `sstation-config.groovy.example` lives at the repo root with every value AC IT must set, with comments. Values marked 🏫.
  - Boot logs explicitly list which placeholders were not overridden.
- **Estimate:** M.

### TC-031 ☁️ Produce a deployable WAR via CI
- **Why:** `grails war` works locally but there's no artifact pipeline.
- **Acceptance criteria:**
  - CI job (TC-034) produces `sstation-${version}.war` as a release artifact on tagged commits.
  - WAR is tested end-to-end by booting it under Tomcat 8 in the CI job and hitting `/sstation/login/auth`.
- **Estimate:** M.

### TC-032 ☁️ AWS demo deployment — Elastic Beanstalk (Tomcat 8 platform)
- **Why:** A click-through demo URL we can put in front of the AC team. EB is the lowest-effort Tomcat-friendly target.
- **Acceptance criteria:**
  - EB application created in `us-east-1` (or AC's preferred region). Platform: **Tomcat 8 with Corretto 8** (the only Tomcat 8 platform AWS still offers; required because Grails 2.4.4 won't run on Java 11+).
  - WAR uploaded via `eb deploy` from the CI job (or manually for the first cut).
  - RDS Postgres `db.t4g.micro` instance, single-AZ, in the same VPC. Credentials wired via EB environment variables matching TC-029/TC-030 (`SSTATION_DB_URL` etc.).
  - HTTPS via an ACM cert on the EB load balancer. Domain: `sstation-demo.<our-domain>` (placeholder 🏫 — we own a domain for the demo; AC IT will substitute their own DNS).
  - Security group rules: 443 from anywhere, 5432 only from EB security group.
  - Cost target: **< $25/mo** on free-tier-eligible instance sizes. Document monthly burn estimate in the card.
  - Demo seed loaded once via a one-off `grails dbm-update` run; demo users (`admin` / `student` / `moderator`) have non-trivial passwords stored in 1Password / shared vault.
  - README gets a "Live demo" link.
- **Notes:**
  - **Not for prod use by AC.** This is a showcase. AC IT deploys to their own infra.
  - Beanstalk's Tomcat 8 platform is on extended support — we should expect to retire the demo or migrate when AWS drops it.
- **Estimate:** L.

### TC-034 ☁️ GitHub Actions CI — build-only WAR pipeline
- **Status:** ✅ **Landed 2026-05-26 in a reduced form** (see "Scope reduction" below).
- **Why:** No CI today.
- **Acceptance criteria (as shipped):**
  - Workflow on push and PR. ✅
  - Pins JDK 8 (Temurin). ✅
  - Installs Grails 2.4.4 via SDKMAN — the bundled wrapper URL is dead. ✅
  - Builds the WAR (`grails prod war`) on **every push** (not just tags) — proves the code compiles. ✅
  - Uploads the WAR as a workflow artifact, keyed by `${branch}-${sha}`. ✅
  - Discord notifications on failure / main / tag pushes. ✅
  - Status badge in the README. ✅
- **Scope reduction — no test execution in CI:**
  - Grails 2.4.4 hardcodes a `-javaagent` attachment of the abandoned Spring Loaded library to its forked test JVMs.
  - Spring Loaded is incompatible with JDK 8u60+ — it crashes `Method.copy()` during Spock AST compilation. The crash is logged as SEVERE but not fatal, so the build slogs along producing thousands of error lines until it hits the 30min job timeout.
  - After multiple attempts (`-noreloading` flag, removing the JAR from `$GRAILS_HOME/lib`, removing from `sstation/wrapper/`), the agent kept reappearing from locations we hadn't grepped. We stopped chasing it.
  - **Decision:** ship CI without tests rather than burn more time on a stack we're rewriting in Lane 7. Unit specs continue to live under `sstation/test/unit/` and can be run locally on a JDK 8 dev machine via `grails test-app unit:`.
  - **Lane 7 takeover:** TC-102 sets up CI for the new Spring Boot module on JDK 21 / Gradle / JUnit 5 — none of these stack landmines apply. TC-111 ports the Spock specs to JUnit 5 as the parity gate before TC-112 deletes the Grails app.
- **Files:** [.github/workflows/ci.yml](.github/workflows/ci.yml), README badge.
- **Estimate:** M. **Actual:** M+ (we spent the back half of the estimate fighting Spring Loaded before pivoting).

### TC-035 📝 🏫 Austin College IT handoff runbook (`DEPLOY.md`)
- **Why:** The whole point of the project: ship something AC IT can stand up. We can't deploy to their network — we *can* hand them a checklist.
- **Acceptance criteria:** A single `DEPLOY.md` at the repo root with these sections, every 🏫 placeholder clearly marked and explained:
  - **Prerequisites:** JDK 8, Tomcat 8 (or 9 with compat tweaks), PostgreSQL 12+, an SMTP relay AC already runs.
  - **Build:** `grails war` (or download from GitHub Releases — link to artifact from TC-031).
  - **Database:**
    - 🏫 Create a database (suggested name `sstation`) and a user.
    - 🏫 Set `SSTATION_DB_URL`, `SSTATION_DB_USER`, `SSTATION_DB_PASSWORD`.
    - First boot runs migrations.
  - **SMTP:**
    - 🏫 `SSTATION_MAIL_HOST`, `SSTATION_MAIL_PORT`, `SSTATION_MAIL_USERNAME`, `SSTATION_MAIL_PASSWORD`, `SSTATION_MAIL_FROM`.
    - Likely values: AC's existing Exchange / Office 365 relay.
  - **Server URL:** 🏫 `SSTATION_SERVER_URL = https://service-station.austincollege.edu` (or wherever).
  - **TLS:** Assume AC IT terminates TLS at a load balancer / reverse proxy. Document the X-Forwarded-* header config Tomcat needs.
  - **First admin user:** AC IT runs a one-off SQL script (we provide it) or hits a one-time bootstrap endpoint to create a real admin account, then immediately changes the password.
  - **Backups:** point to AC's existing Postgres backup procedure — we don't prescribe one.
  - **Monitoring:** the app logs to stdout. AC IT plugs into their existing log aggregator (Splunk, Graylog, whatever).
  - **Upgrade path:** download new WAR, replace, restart Tomcat. Migrations auto-apply.
- **Estimate:** M. Iterate with AC IT once a draft exists.

### TC-036 📝 Fix the dead Grails wrapper URL or remove the wrapper
- **Why:** [wrapper/grails-wrapper.properties](sstation/wrapper/grails-wrapper.properties) points at `dist.springframework.org.s3.amazonaws.com`, which 404s. The wrapper script is therefore broken — but it's still in the repo, so anyone who tries `./grailsw run-app` first wastes an hour.
- **Acceptance criteria:**
  - Either: (a) point the wrapper at a working mirror (we host the 2.4.4 zip on our own S3 bucket — note 🏫 if we want AC to host it) and verify `./grailsw run-app` works, OR
  - (b) Delete the wrapper entirely and rely on SDKMAN-installed Grails, documented in [CLAUDE.md](CLAUDE.md) and the new [DEPLOY.md](DEPLOY.md).
  - Recommendation: (b). The wrapper buys nothing here.
- **Estimate:** S.

### TC-037 📝 Finish the README
- **Why:** README "Workflow" section is empty; `CampusOrg` description ends mid-sentence. First impression of the repo is "abandoned class project."
- **Acceptance criteria:**
  - Workflow section diagrams (Mermaid) the three primary flows: student logs hours → admin approves → student sees report.
  - CampusOrg description completed.
  - Link to live AWS demo (TC-032).
  - Link to [CLAUDE.md](CLAUDE.md) and [DEPLOY.md](DEPLOY.md).
- **Estimate:** S.

---

## Lane 6 — Future / nice-to-have

Not on the critical path to hand-off. Park these.

### TC-038 🧹 Replace CDN frontend assets with locally-served copies
- **Why:** jQuery 1.11.3, Bootstrap 3.3.5, DataTables 1.10.10, Highcharts, jQuery UI 1.11.4 are all loaded from CDNs in [main.gsp](sstation/grails-app/views/layouts/main.gsp). CDN URLs go stale; AC IT may be behind a proxy that blocks them. Vendor them into `web-app/js/` and `web-app/css/`.
- **Estimate:** S.

### TC-039 ✨ Migrate Selenium IDE HTML scripts to Playwright
- **Why:** [selinium_tests/](selinium_tests/) needs legacy Firefox + Selenium IDE to run. Rewrite the half-dozen useful flows in Playwright, plumb them into CI (TC-034).
- **Estimate:** L.

### TC-040 🧹 Port to Grails 5/6 or Spring Boot
- **Status:** **Promoted to Lane 7** — broken into TC-100 through TC-112 below. Originally parked; now planned for Summer 2026 since we have the runway.

### TC-041 ✨ Two-factor auth for admins
- **Why:** Approving service hours = a credentialing record. 2FA at least on admin accounts is worth it once we're on a maintained Spring Security version (TC-040 or a backport).
- **Estimate:** M.

---

## Lane 7 — Modernization / Rewrite (Summer 2026)

We have ~12 weeks of runway before handing the project to AC IT. That's enough for a full rewrite to a modern, maintainable stack — provided we scope tight, keep the Grails app shippable as a fallback throughout, and don't redesign the UX.

**Working assumption — recommended target stack:**

> **TC-100 status (updated 2026-09-02):** Memo sent 2026-06-02 ([docs/TC-100-stack-memo.md](docs/TC-100-stack-memo.md)); no
> written reply. **Superseded in practice:** the Service Station office has agreed to the project, and AC IT is not
> expected to push back on the stack. The stack below is therefore treated as **settled** — TC-103 → TC-108 have all
> shipped on it and it is not being revisited.
>
> **What genuinely remains open is narrower than the original memo, and only two items block anything:**
 > - ~~🏫 **Highcharts licensing**~~ ✅ **Resolved 2026-09-02 (TC-119)** — swapped to Chart.js (MIT). No license to obtain,
>   nothing to ask AC IT, and no longer a blocker on a public URL.
> - 🏫 **SMTP relay** — host/port/credentials. Until supplied, `spring.mail.host` is empty and TC-108a notifications are
>   **log-only**. The app boots fine; the emails simply never send.
>
> Lower-stakes and answerable at deploy time rather than now: prod OS, existing Java version, SSO/IdP (a config seam is
> already left in `SecurityConfig`). Postgres is settled by TC-113/TC-114.

- **Java 21 LTS** + **Spring Boot 3.x** + **Spring Security 6** + **Spring Data JPA** + **Hibernate 6**
- **Thymeleaf** server-rendered templates (1:1 conceptual port from GSP, low-risk; no SPA)
- **PostgreSQL** (matches TC-029)
- **Gradle 8** build, single executable JAR + Dockerfile
- **JUnit 5 + Mockito + Testcontainers** (replaces Spock unit specs + Selenium IDE)
- **Playwright** for E2E (matches TC-039)
- **Flyway** for migrations (replaces Grails `database-migration` plugin)

**Why Spring Boot + Thymeleaf and not [other]:**
- Java is the most universally-supported enterprise language; AC IT almost certainly has Java ops experience.
- Spring Boot is the de-facto enterprise Java standard; documentation and hires are abundant.
- Thymeleaf is server-rendered like GSP — porting is mechanical, not a redesign.
- Single executable JAR / Docker image is easier for AC IT to deploy than a WAR-into-Tomcat dance.
- We avoid frontend complexity (no SPA, no Node/npm in the deploy story).
- Spring Boot 3 + Java 21 has at least 5 years of LTS runway.

**Alternative considered:** Grails 6 (incremental 2.4 → 3 → 4 → 5 → 6 climb). Rejected because (a) each step requires hand-tuning and the cumulative effort is similar to a Spring Boot rewrite, (b) Grails has lost mindshare — harder for AC IT to hire/maintain long-term, (c) Groovy adds a language AC IT may not want.

🏫 **Confirm target stack with AC IT before TC-100 lands.** If they have a different house standard (e.g. .NET, Django), revise this lane accordingly — the *sequence* of work below still applies, only the destination changes.

---

### TC-100 📝 🏫 Lock target stack with AC IT ✅ closed 2026-09-02 (proceeding on the working assumption)
- **Status:** **Closed as "decided by default."** Memo written and sent 2026-06-02; no written reply received in three months. Per the Service Station office (2026-09-02), the project is agreed and AC IT is not expected to contest the stack. Seven cards (TC-103 → TC-108) have now shipped on Spring Boot 3 / Java 21 / Postgres and the decision is not being reopened. **Do not treat this card as a blocker anymore.** The residual 🏫 items were split out: **TC-119** (Highcharts licensing — the one with legal teeth) and the SMTP relay placeholder tracked in TC-110/[Notes for AC IT](#notes-for-ac-it-collect-placeholders-here).
- **Why (original):** Everything else in this lane depends on the answer. *(Retained for history — in practice the team proceeded on the working assumption and it was the right call.)*
- **Acceptance criteria:**
  - One-page memo emailed to AC IT contact: recommended stack (Spring Boot 3 / Java 21 / Postgres / Thymeleaf), why, what we're trading off, ask for written confirmation or a counter-proposal.
  - AC IT's preferred deploy target documented (Tomcat? bare JAR? Docker? Kubernetes? OS preference?).
  - Their existing Java version on the prod box, if any.
  - Decision recorded in this file as an update to the "Working assumption" block above.
- **Estimate:** S (the writing) + however long AC IT takes to respond.
- **Blocks:** Everything else in Lane 7.

### TC-101 ☁️ Scaffold new repo structure ✅ landed 2026-06-02
- **Status:** Done. `sstation-next/` Spring Boot 3.3.5 / Java 21 / Gradle Kotlin DSL baseline, `./gradlew bootRun` Hello World, Spotless, `README-NEXT.md`. Chose option (a) — parallel dir in-repo.
- **Why:** Decide where the rewrite lives. Two options: (a) new top-level dir `sstation-next/` in the same repo, parallel to `sstation/`; (b) brand-new repo. Option (a) keeps git history and commit references intact; option (b) is cleaner for handoff. I lean (a) for the rewrite phase, then move it to its own repo for the handoff.
- **Acceptance criteria:**
  - `sstation-next/` directory with a Spring Initializr-generated baseline: Spring Boot 3.3+, Java 21, Gradle (Kotlin DSL), dependencies: web, security, data-jpa, validation, thymeleaf, postgresql, flyway, actuator.
  - `./gradlew bootRun` produces a "Hello World" page on port 8080.
  - Lint / formatter set up (Spotless + Google Java Format, or equivalent).
  - `README-NEXT.md` in the new dir with a "this is the rewrite-in-progress" disclaimer.
- **Estimate:** S.

### TC-102 ☁️ Parallel CI for the new module ✅ landed 2026-06-02
- **Status:** Done. `ci-next.yml` runs `./gradlew check` on JDK 21, path-filtered to `sstation-next/**`; Grails `ci.yml` left unchanged; both badges in the README.
- **Why:** Lane 5's CI workflow is Grails-specific. The new module needs its own workflow that doesn't fight with the old one.
- **Acceptance criteria:**
  - `.github/workflows/ci-next.yml` runs `./gradlew check` on JDK 21.
  - Triggers on changes to `sstation-next/**` only (path filter).
  - The existing Grails workflow keeps working unchanged on `sstation/**` paths.
  - Both badges in the README.
- **Estimate:** S.

### TC-103 🧹 Port the domain model to JPA entities ✅ landed 2026-06-02
- **Status:** Done. 9 entities + 2 enums under `edu.austincollege.sstation.domain`, Flyway `V1__initial_schema.sql`, JUnit 5 validation + relationship tests (run against the Flyway schema with `ddl-auto=validate`). **Decision:** `ServiceHour.{campusOrg, commAg, event}` kept **nullable** (matches Grails + the `other*` free-text fields); `student` is required. `Contact` ported as a minimal id+name/phone/email entity (the Grails original was empty/unreferenced).
- **Why:** The domain model is the spine of the app. Get this right first — everything else (services, controllers, views) depends on the entity shapes.
- **Acceptance criteria:**
  - JPA `@Entity` classes for: `User`, `Role`, `UserRole`, `Student` (renamed from `AcStudent`), `ServiceHour`, `Event`, `CampusOrg`, `CommunityAgency` (renamed from `CommAg`), `Contact`.
  - Enums `Status` and `Classification` ported (kept as Java enums, mapped via `@Enumerated(EnumType.STRING)`).
  - **Crucially:** real `@ManyToOne` FK from `User` to `Student` — fixes the email-string hack from [TC-009](#tc-009-).
  - **Crucially:** `commAg`, `event`, `campusOrg` on `ServiceHour` are `@ManyToOne(optional = false)` unless we deliberately want them nullable (decide per field — confirm with the requirements).
  - Flyway migration `V1__initial_schema.sql` matches the entity shapes.
  - JUnit 5 tests cover entity validation + relationships.
- **Files:** new `sstation-next/src/main/java/edu/austincollege/sstation/domain/*.java`, `src/main/resources/db/migration/V1__*.sql`.
- **Estimate:** M.

### TC-104 🔒 Port authentication & authorization ✅ landed 2026-06-02
- **Status:** Done. Form login/logout, delegating `{bcrypt}` encoder, `@EnableMethodSecurity` + `@PreAuthorize` on controllers, CSRF on, `DevDataSeeder` (`@Profile("dev")`) seeds the three accounts with passwords from `SSTATION_DEV_*_PASSWORD` env vars. SSO/OIDC seam documented in `SecurityConfig`. Verified via an 8-case MockMvc suite + live.
- **Why:** Spring Security 6 is the modern equivalent of the EOL plugin we're using. Done right, it also gets us CSRF + bcrypt + proper session management for free.
- **Acceptance criteria:**
  - Form login + logout against the `User` / `Role` tables.
  - BCrypt password encoding (no plaintext fallback — fixes TC-008 at the source).
  - Role-based authorization annotations on every controller method (`@PreAuthorize("hasRole('ADMIN')")` etc.) — closes the audit gap from [TC-019](#tc-019-).
  - CSRF protection enabled (fixes TC-018 at the source).
  - Three seeded users in dev profile only (matches the current `admin` / `student` / `moderator`), credentials from env vars (fixes TC-017 at the source).
  - 🏫 Optional: pluggable SAML/OIDC if AC IT runs an IdP — leave a config seam, document it.
- **Estimate:** M.

### TC-105 ✨ Port read-only views first (dashboards, lists, reports) ✅ landed 2026-06-03
- **Status:** Done, in three slices. (a) Admin dashboard via `StatsService` + `DemoDataSeeder`. (b) Six reports via `ReportService` (`ReportsController` gated `hasAnyRole('ADMIN','MODERATOR')`). (c) Student dashboard + per-student report via `StudentStatsService`, resolving the current student through the real `User→Student` FK. All current-year logic via `LocalDate.now()`, top-N bounds-safe, all FK access null-guarded. Highcharts still via CDN pending TC-107. Verified `./gradlew check` (49 tests) + live click-through.
- **Why:** Read views are the lowest-risk port and exercise most of the data model. Get these working before touching write paths.
- **Acceptance criteria:**
  - Admin dashboard with the same KPIs and charts as the Grails app (same Highcharts data shapes — keeps the JS frontend nearly identical).
  - Student dashboard.
  - All six reports: summary, semester, year, event, community-org, campus-org.
  - Per-student report.
  - **All "current year" / "current semester" logic uses `LocalDate.now()`** — no hardcoded years (fixes TC-001 + TC-002 at the source, permanently).
  - **All loops over top-N are bounds-safe** (fixes TC-003 at the source).
  - **All FK accesses are null-safe** (fixes TC-006 at the source).
- **Estimate:** L (the reports alone are ~3 days; six of them).

### TC-106 ✨ Port CRUD: students, hours, events, orgs, agencies ✅ landed 2026-06-03
- **Status:** Done, in three slices. (a) Reference CRUD (Event/CampusOrg/CommunityAgency) with delete-detaches-hours. (b) Student CRUD + ADMIN-only moderator promote/demote. (c) ServiceHour CRUD + the quick approve/reject REST endpoint (`POST /admin/hours/{id}/status`, CSRF, ADMIN-only, JSON) + audit trail (`ServiceHourAuditLog` + Flyway `V2`, written on every status change, admin-only per-hour view). `@Valid` bean-validation throughout. `./gradlew check` green (78 tests) + verified live.
- **Why:** The write-path features. Largest single chunk of porting work.
- **Acceptance criteria:**
  - CRUD pages for each of the five entity types, with the same fields as the Grails forms.
  - Server-side validation via Jakarta Bean Validation annotations.
  - "Quick approve / reject" AJAX endpoint preserved (now a proper REST endpoint with CSRF token).
  - Moderator promote/demote flow preserved.
  - Audit trail (matches [TC-027](#tc-027-)) — write entries on every status change.
- **Estimate:** L.

### TC-107 ✨ Port + modernize the frontend layer ✅ landed 2026-06-03
- **Status:** Done. Shared `fragments/layout.html` (Bootstrap 5 navbar/container/footer) decorated by every page; all templates restyled with Bootstrap 5. Assets **vendored via WebJars** (`/webjars/**`, no CDN — closes TC-038): Bootstrap 5.3.3, jQuery 3.7.1, DataTables 2.1.8, and (originally) Highcharts 11.2.0 — **the charting library was replaced with Chart.js 4.4.3 (MIT) in TC-119**. Chart data shapes unchanged; DataTables on the students list; mobile-friendly.
- **Why:** Thymeleaf templates instead of GSP. Same page structure, modern asset versions, vendored not CDN.
- **Acceptance criteria:**
  - Thymeleaf templates mirror the existing GSP layout (`main.gsp` → `fragments/layout.html`, etc.).
  - **Vendored assets:** Bootstrap 5.3, jQuery 3.7, DataTables 2.x, and Chart.js 4.4 (MIT — originally Highcharts, replaced in TC-119), no CDN dependencies (closes TC-038 at the source).
  - The dashboard charts render with the same data shapes as before (we change *backends*, not chart configs).
  - Mobile-friendly (Bootstrap 5 gives us this almost for free).
- **Estimate:** M.

### Build readiness — Docker + EC2 (review before TC-113 → TC-117)

**Verdict (2026-06-04):** You are **good to build** containerization and a **public demo** on AWS. Lane 7 foundation through **TC-107** is landed (`./gradlew check` green, app runnable locally with JDK 21). Docker/AWS work does **not** require TC-108 (Lane 4 features) or TC-111 (parity tests) first.

**Prerequisites already satisfied in `sstation-next/`:**
- Flyway schema (`V1`, `V2`) + `prod` profile with `SSTATION_DB_*` env vars ([application.yml](sstation-next/src/main/resources/application.yml)).
- Executable JAR via `./gradlew bootJar`.
- Actuator health endpoint for container health checks.
- Dev-only seeders (`DevDataSeeder`, `DemoDataSeeder`) — **not** active in `prod`.

**Blockers / gaps you must plan for (easy to miss):**
| Issue | Why it matters |
|-------|----------------|
| **Empty DB on `prod` boot** | `SPRING_PROFILES_ACTIVE=prod` runs Flyway but **no users or demo data**. A public URL without **TC-114** is a login screen with nobody to sign in as. |
| **TC-033 / TC-109 wording is Grails-era** | [TC-033](#tc-033-) targets Tomcat 8 + WAR; [TC-109](#tc-109-) targets App Runner. Today's work is **`sstation-next`** + **EC2 t3.micro** — use **TC-113** and **TC-116** instead. |
| **Do not ship `admin_secret` on the internet** | Dev defaults are fine locally; AWS demo needs strong passwords via env vars (**TC-114**, extends TC-017). |
| **t3.micro = 1 GiB RAM** | Running Spring Boot **and** Postgres on one instance often OOMs. **TC-116** recommends **EC2 + RDS**; all-in-one compose on EC2 is documented as demo-only fallback. |
| **HTTPS is your problem on EC2** | Unlike App Runner (TC-109), EC2 needs Caddy/nginx + Let's Encrypt or an ALB (~$16/mo extra). Budget for TLS in **TC-116**. |
| ~~**Highcharts licensing**~~ | ✅ Resolved by **TC-119** (2026-09-02) — swapped to Chart.js (MIT). No longer gates the demo. |
| **TC-100 AC IT reply still open** | Does **not** block a student-project AWS demo; **does** block calling the rewrite "handoff-ready." |

**Grails cards to skip on the Lane 7 path:** TC-029, TC-030, TC-031, TC-032, TC-035 (Grails DEPLOY), and the original TC-033 acceptance criteria — unless the rewrite slips and you need a fallback WAR demo.

**Suggested build order today:** TC-113 → TC-114 → TC-110 (DEPLOY.md) → TC-115 → TC-116 → README live-demo link (TC-037 partial).

---

### TC-113 ☁️ Containerize `sstation-next` (Dockerfile + docker-compose)
- **Status:** ✅ **Landed 2026-06-04** — `Dockerfile`, `docker-compose.yml`, `docker-compose.dev.yml`, `docker-compose.ec2.yml`, `.dockerignore`, `.env.example`, [DEPLOY.md](DEPLOY.md). Prod compose boots schema only (users: **TC-114**). Verify: `docker compose -f docker-compose.dev.yml up --build`.
- **Supersedes the *rewrite* path for [TC-033](#tc-033-);** Grails Tomcat image remains out of scope unless maintaining `sstation/`.
- **Why:** One-command `docker compose up` is the fastest "does this run?" smoke test for reviewers and matches the executable-JAR deploy story in Lane 7. Same image feeds EC2 (**TC-116**) and optional GHCR (**TC-115**).
- **Acceptance criteria:**
  - Multi-stage `Dockerfile` in [sstation-next/](sstation-next/): `eclipse-temurin:21-jdk` build stage (`./gradlew bootJar -x test`), `eclipse-temurin:21-jre-alpine` run stage, non-root user, port 8080, container-aware JVM flags (`-XX:MaxRAMPercentage=75.0` or equivalent).
  - [sstation-next/.dockerignore](sstation-next/.dockerignore) excludes `build/`, `.gradle`, etc.
  - [sstation-next/docker-compose.yml](sstation-next/docker-compose.yml): services `app` + `db` (`postgres:16-alpine`), `SPRING_PROFILES_ACTIVE=prod`, JDBC `jdbc:postgresql://db:5432/sstation`, Flyway runs on boot.
  - [sstation-next/.env.example](sstation-next/.env.example) documents compose variables; no real secrets committed.
  - Optional [sstation-next/docker-compose.dev.yml](sstation-next/docker-compose.dev.yml) override: `SPRING_PROFILES_ACTIVE=dev` for seeded logins without local JDK 21.
  - `docker compose up --build` from `sstation-next/` → **http://localhost:8080**; `GET /actuator/health` returns UP.
  - Documented in [DEPLOY.md](DEPLOY.md) (see **TC-110**).
- **Depends on:** TC-107 ✅. Meaningful login smoke test depends on **TC-114** (or manual seed).
- **Estimate:** M.

### TC-114 🔒 Demo profile seeder for container / AWS deploy
- **Status:** ✅ **Landed 2026-06-04** — `DemoAccountSeeder` (`@Profile("demo")`, required `SSTATION_DEMO_*_PASSWORD`), `DemoDataSeeder` also on `demo`, `docker compose` uses `prod,demo`, tests + [DEPLOY.md](DEPLOY.md) updated.
- **Why:** `DevDataSeeder` and `DemoDataSeeder` are `@Profile("dev")` only. A `prod` Docker/EC2 boot creates schema via Flyway but **no users** — reviewers cannot click through. Extends [TC-017](#tc-017-) for anything internet-facing.
- **Acceptance criteria:**
  - New Spring profile `demo` (or equivalent) with an idempotent seeder: roles, `admin` / `student` / `moderator` users, Sam Student + reference data + sample hours (parity with dev demo intent).
  - All demo passwords read from env vars (e.g. `SSTATION_DEMO_ADMIN_PASSWORD`) — **no** `admin_secret` defaults when `demo` profile is active.
  - Startup log line when demo profile is on: warns that demo credentials must be rotated and profile must never be used for AC production.
  - `docker compose` and **TC-116** document: `SPRING_PROFILES_ACTIVE=demo` (or `prod,demo` if split) for showcase only.
  - Manual verify: sign in as all three roles against a compose stack with demo profile.
- **Files:** new `DemoProfileSeeder` (or extend config package), [application.yml](sstation-next/src/main/resources/application.yml) profile block, [DEPLOY.md](DEPLOY.md).
- **Depends on:** TC-104 ✅, TC-105 ✅ (demo data shapes).
- **Estimate:** S–M.

### TC-115 ☁️ CI — build and verify Docker image
- **Status:** 🔲 **Next** — optional GHCR push can land in same card or a follow-up.
- **Why:** Proves the Dockerfile stays valid on every PR; EC2 can `docker pull` a pre-built image instead of compiling on a 1 GiB instance.
- **Acceptance criteria:**
  - [.github/workflows/ci-next.yml](.github/workflows/ci-next.yml) adds a job (or step): `docker build -f sstation-next/Dockerfile sstation-next` on PR/push to `sstation-next/**`.
  - Job runs after `./gradlew check` passes (or as a dependent job).
  - Optional: push to `ghcr.io/<org>/sstation-next:<sha>` on `main` tags; document pull command in **TC-110**.
- **Depends on:** TC-113, TC-102 ✅.
- **Estimate:** S.

### TC-116 ☁️ AWS public demo — EC2 t3.micro + RDS Postgres
- **Status:** 🔲 **Next** — **team choice:** EC2 t3.micro (this card) instead of App Runner in [TC-109](#tc-109-). TC-109 remains a documented **alternate** if ops preference changes.
- **Why:** Click-through demo for AC reviewers without a local JDK 21 / Grails install. EC2 t3.micro is free-tier-friendly; pairing with RDS avoids OOM on 1 GiB RAM when running Postgres + Spring Boot on one box.
- **Acceptance criteria:**
  - **Recommended topology:** `t3.micro` EC2 (Amazon Linux 2023) runs Docker app container only; `db.t4g.micro` RDS Postgres 16 in same VPC; RDS SG allows 5432 **only** from EC2 SG; EC2 SG allows 443 (and 22 from operator IP only).
  - App container: image from **TC-113** / **TC-115**, `SPRING_PROFILES_ACTIVE` includes demo seed per **TC-114**, `SSTATION_DB_*` points at RDS, `server.forward-headers-strategy=framework` if behind reverse proxy.
  - HTTPS: Caddy or nginx on EC2 terminating TLS (Let's Encrypt) **or** ALB + ACM (document cost tradeoff; ALB may exceed $25/mo budget alone).
  - DNS: `sstation-demo.<our-domain>` (🏫) or raw EC2 public DNS documented in README for interim.
  - Cost note in DEPLOY.md: target **< $25/mo** (EC2 + RDS, no ALB) where free tier applies.
  - Smoke test: admin dashboard KPIs non-zero, student login lands on populated dashboard, one report page renders.
  - README "Live demo" link updated.
  - **Appendix documented:** single-EC2 `docker compose` (app + Postgres) with memory limits — **demo-only**, OOM risk called out.
- **Does not replace:** AC IT production deploy (🏫 SMTP, real admin provisioning, hardening) — showcase only, same spirit as [TC-032](#tc-032-).
- **Depends on:** TC-113, TC-114, TC-110 (draft OK in parallel).
- **Estimate:** L.

### TC-117 📝 README + Trello hygiene after Docker/EC2
- **Status:** 🔲 **After TC-116**.
- **Why:** [README-NEXT.md](sstation-next/README-NEXT.md) still says "incomplete"; [TC-037](#tc-037-) and super prompt still center Grails in places. Reduces confusion after the demo ships.
- **Acceptance criteria:**
  - [README.md](README.md) points to [demo.md](demo.md), [DEPLOY.md](DEPLOY.md), Docker quick start, and live demo URL.
  - [sstation-next/README-NEXT.md](sstation-next/README-NEXT.md) status updated to match TC-105–TC-107 (+ Docker).
  - [CLAUDE.md](CLAUDE.md) Lane 7 "Next" line references TC-113–TC-117 sequence.
  - This file: TC-109 marked **alternate (App Runner)**; TC-033 marked **Grails-only / superseded by TC-113 for rewrite**.
- **Estimate:** S.

### TC-108 ✨ Port + implement the Lane 4 features in the new stack ✅ landed 2026-06-03
- **Status:** Done, in seven slices (`feat/port-backend`, PR #7). (a) `NotificationService` + Spring Mail, env-driven SMTP, log-only when `spring.mail.host` is unset. (b) Bulk approve/reject on the pending queue. (c) `ReportCsvService` (OpenCSV, RFC-4180 escaping) wired to all six reports + the student report. (d) `StudentReportPdfService` (openhtmltopdf + jsoup) rendering a dedicated `pdf/student-report.html`. (e) Date-range filter on the admin dashboard (`StatsService`). (f) Event sign-up flow — `EventSignup`/`SignupStatus` + Flyway `V3`, student self-signup and an admin roster. (g) Self-service password reset — `PasswordResetToken` + Flyway `V4`, SHA-256-hashed single-use tokens with a 1-hour TTL and no user enumeration. Verified `./gradlew check` green (115 tests at the time).
- **Why:** Several Lane 4 cards (email notifications, CSV export, PDF export, bulk approve, date-range filter, signup flow, audit log, password reset) are easier to implement in Spring Boot than to port from Grails 2.4 and then re-port. If we're rewriting anyway, build these in the new stack from the start.
- **Acceptance criteria:**
  - ✅ Email via Spring Mail (TC-021 equivalent) — env-driven SMTP.
  - ✅ CSV export via OpenCSV (TC-023).
  - ✅ PDF export via openhtmltopdf (TC-024).
  - ✅ Bulk approve/reject (TC-022).
  - ✅ Date-range filter (TC-025).
  - ✅ Event sign-up (TC-026).
  - ✅ Audit log (TC-027) — built into TC-106 from day one.
  - ✅ Password reset (TC-028).
- **Estimate:** L. Roughly halves the Lane 4 work since we're not doing it twice.

### TC-109 ☁️ Migrate the AWS demo to the new stack — **App Runner alternate**
- **Status:** ⏸️ **Alternate deploy target** — primary demo path is [TC-116](#tc-116-) (EC2 t3.micro + RDS). Dockerfile work lives in **TC-113** either way.
- **Why:** Beanstalk Tomcat 8 (TC-032) is end-of-life-on-borrowed-time. Spring Boot 3 fat JAR runs natively on AWS App Runner, ECS Fargate, or even Lambda — all modern, all supported.
- **Acceptance criteria:**
  - Dockerfile based on `eclipse-temurin:21-jre-alpine`, multi-stage build. *(Shared with TC-113.)*
  - AWS App Runner service deployed from the image. RDS Postgres `db.t4g.micro` retained.
  - HTTPS via App Runner's built-in cert.
  - DNS: same `sstation-demo.<our-domain>` as the original demo (🏫 — AC will substitute their own).
  - Cost target: still **< $25/mo**.
  - Old Beanstalk environment torn down once new demo is verified.
- **Superseded for current sprint by:** [TC-116](#tc-116-) unless team prefers managed TLS via App Runner.
- **Estimate:** M.

### TC-110 📝 Update DEPLOY.md to target the new stack
- **Status:** 🔲 **In progress with Docker/EC2 sprint** — land alongside TC-113–TC-116.
- **Why:** [TC-035](#tc-035--🏫-austin-college-it-handoff-runbook-deploymd) was written for the Grails app. After the rewrite the runbook needs a full rewrite of its own.
- **Acceptance criteria:**
  - Prerequisites: JDK 21 (or just "the Dockerfile, if you do Docker"), PostgreSQL 13+, SMTP relay.
  - Build: `./gradlew bootJar` or pull pre-built image from GHCR (**TC-115**).
  - **Docker:** `docker compose up` local smoke test (**TC-113**).
  - **AWS:** EC2 t3.micro + RDS topology, security groups, Caddy TLS, env vars, cost estimate (**TC-116**); appendix for single-box compose OOM warning.
  - Same env var names as the Grails version where possible (`SSTATION_DB_URL`, `SSTATION_MAIL_HOST`, etc.) so AC IT's secrets manager doesn't need rework.
  - Migration story: Flyway auto-runs on boot.
  - Demo profile / passwords (**TC-114**); never use dev defaults on a public host.
  - 🏫 same placeholder set as TC-035.
  - Old `DEPLOY.md` retained as `DEPLOY-legacy.md` for one release, then removed.
- **Depends on:** TC-113 (draft sections can start in parallel).
- **Estimate:** M.

### TC-111 🧹 End-to-end acceptance suite + stakeholder sign-off (was: "parity test against the old app")
- **Status:** 🔲 **Rescoped 2026-09-02.** The original card asked for a side-by-side HTML diff against the Grails app. **That is no longer achievable and should not be attempted:** TC-107 restyled every page to Bootstrap 5, and TC-108 added seven flows the Grails app never had (email notifications, bulk approve, CSV, PDF, date-range filter, event sign-up, password reset). There is no longer a comparable page to diff. Parity is now established by *feature coverage*, not by rendered output.
- **Why:** Before declaring the rewrite done we still need machine-checked proof that the user-visible flows work in a real browser. The 119 JUnit tests cover services and controllers via MockMvc — **nothing currently drives a real browser**, and no test has ever exercised the app end-to-end over HTTP.
- **Acceptance criteria:**
  - Playwright suite (from [TC-039](#tc-039-)) written **against `sstation-next` only** — no Grails comparison.
  - Covers, per role: login/logout + role routing, admin dashboard KPIs render, one report of each shape, the pending-queue approve/reject round trip (and its audit entry), one CRUD create→edit→delete cycle, student self-signup, and the CSV + PDF downloads returning the right content type.
  - Runs against the **Docker compose stack** (TC-113) so it exercises Postgres, not H2.
  - Wired into `ci-next.yml` (headless, on PR).
  - A **checklist mapping every Grails feature to its rewrite equivalent** — this is the real parity artifact, and it replaces the HTML diff.
  - At least one Service Station stakeholder clicks through and signs off.
- **Depends on:** TC-113 ✅, TC-118 (Postgres tests must actually run first).
- **Estimate:** M–L.

### TC-118 ⚠️ The Postgres integration test silently skips — `check` is green on a no-op
- **Status:** 🔲 **Next — do this before TC-116 (AWS).** Found 2026-09-02 while merging `Dev/Docker`. **Severity revised down the same day:** a CI run on the merge commit ([run 33644576602](https://github.com/AC-Conflictus/ServiceStation-Tracker/actions/runs/33644576602)) reports `DemoProfileIntegrationTest` as **`tests=2 skipped=0`** — so the Postgres path *is* genuinely exercised on the GH runner, and 119/119 tests really run there. The defect is therefore **local-only, but it is still a real trap**: on a modern dev box the test vanishes from the run and nothing tells you.
- **Why:** `DemoProfileIntegrationTest` (TC-114) is the **only** test that touches real PostgreSQL — it Testcontainers a `postgres:16-alpine`, runs Flyway, seeds, and logs in. It is annotated `@Testcontainers(disabledWithoutDocker = true)`, and on a Docker **29.x** host the Testcontainers 1.20.3 docker-java client gets `HTTP 400` back from `/info` during strategy detection (Docker Engine 29 dropped the older API versions docker-java negotiates). Result: **both tests report SKIPPED while `./gradlew check` still says BUILD SUCCESSFUL.** Bumping to Testcontainers 1.21.3 was tried and does **not** fix it.
- **Why it matters:** Flyway `V1`–`V4` are hand-written in the "Postgres/H2-common subset" and `ddl-auto=validate` runs on every boot. If a migration or entity mapping is subtly H2-only, the Postgres test is the only thing standing between that and RDS. CI does catch it today — but a developer running `./gradlew check` locally before pushing gets a **green build that proved nothing about Postgres**, and will only find out on the round trip through CI. The failure mode is a false sense of safety, not (as first assessed) a total absence of coverage.
- **Acceptance criteria:**
  - Root-cause the docker-java/Docker-29 mismatch — try a newer Testcontainers, or pin `DOCKER_API_VERSION`, or point `DOCKER_HOST` at the Docker Desktop socket explicitly.
  - **Make the skip loud:** either drop `disabledWithoutDocker = true` so a missing Docker daemon *fails* the build, or add a `check`-time assertion that the Postgres test actually executed. Silent skips on the one test that de-risks prod are worse than no test.
  - ✅ **Done 2026-09-02** — confirmed from run 33644576602's test XML: `tests=2 skipped=0`, 119/119 overall. Re-verify if the runner image or Testcontainers version changes.
  - Add a second Postgres-backed test that boots plain `prod` (no `demo`) and asserts Flyway migrates a virgin database cleanly — that is the literal AC IT day-one path.
- **Notes / files to touch:** [DemoProfileIntegrationTest.java](sstation-next/src/test/java/edu/austincollege/sstation/config/DemoProfileIntegrationTest.java), [build.gradle.kts](sstation-next/build.gradle.kts), [ci-next.yml](.github/workflows/ci-next.yml).
- **Estimate:** S–M.

### TC-119 🏫 ⚠️ Resolve Highcharts licensing before any public URL ✅ landed 2026-09-02
- **Status:** ✅ **Resolved via option (c) — swapped to Chart.js (MIT).** Highcharts is gone from the build; there is no longer a licensing question to ask AC IT, and nothing here blocks a public URL. Landed in three slices on `feat/tc-119-chartjs`: (a) `org.webjars:highcharts:11.2.0` → `org.webjars:chartjs:4.4.3` + layout script tag and footer credit; (b) the four admin dashboard charts; (c) the four charted reports (year, event, campus-org, community-org — `summary` and `semester` are table-only). **No service-layer change** — the `*Data` DTOs were already chart-agnostic. Verified `./gradlew check` green (119 tests) plus a live browser pass over all eight charts: each renders with the right type, orientation, title and data-point count, and the console is clean.
- **Why:** Highcharts is **not free for commercial or government/institutional use** — it is free only for personal/non-profit/school-project use, and "a college's administrative office runs it in production" is exactly the boundary case that needs a real answer. TC-107 **vendored `org.webjars:highcharts:11.2.0` into the application jar**, so we are now redistributing it. This is a legal exposure, not a technical preference, and unlike the rest of TC-100 it is **not** resolved by AC IT being relaxed about the stack.
- **Acceptance criteria:** *(pick one and record the outcome here)*
  - ~~**(a)** Confirm Austin College holds a Highcharts license covering this use~~ — not pursued.
  - ~~**(b)** Confirm the deployment qualifies for Highcharts' non-commercial terms in writing~~ — not pursued.
  - ✅ **(c)** **Swap to a freely-licensed library** — Chart.js (MIT) or ApexCharts (MIT). Scope if we go this route: four charts on `admin/home.html` plus the six report pages. The data shapes were deliberately kept chart-agnostic in TC-105, so this is a template-and-JS swap, not a service-layer change. Replace the WebJar dependency in [build.gradle.kts](build.gradle.kts) and the `/webjars/**` script tag in [layout.html](sstation-next/src/main/resources/templates/fragments/layout.html).
- **Notes:** If in doubt, just do **(c)**. It is an afternoon of work and it permanently removes the question — cheaper than the email thread, and it de-risks the handoff.
- **Estimate:** S if (a)/(b); M if (c).

### TC-112 🧹 Decommission the Grails app
- **Why:** Once TC-111 signs off, the old app is dead weight in the repo and a source of confusion.
- **Acceptance criteria:**
  - `sstation/` directory removed (history preserved in git).
  - `sstation-next/` renamed to `sstation/` (or moved to a new clean repo for handoff — decide with AC IT).
  - CI workflow for the Grails app deleted.
  - README rewritten to describe only the new app.
  - `CLAUDE.md` rewritten to describe the new stack (or replaced with a stub pointing at handoff docs).
  - Old Selenium IDE folder removed.
  - Final tag `v1.0.0-handoff` cut.
- **Estimate:** S.

---

## Suggested order of attack

With the Summer 2026 timeline and the Lane 7 rewrite in scope, the plan **forks** after the critical fixes. Either we commit to the rewrite and most of Lane 4 collapses into TC-108, or we stay on Grails and grind out the existing backlog. **AC IT's answer to TC-100 decides which branch.**

### Phase 0 — Stabilize (week 1, regardless of fork)
1. **Lane 1 entirely.** TC-001 → TC-008. ✅ **Done 2026-05-29** (`lane-01-critical-fixes`). The app runs on fresh data, student login lands on a populated dashboard, dashboard KPIs reflect the current year, reports are bounds-safe and null-safe, BootStrap is prod-gated, and passwords are bcrypt-only.
2. **CI green** (TC-034 ✅ **Done 2026-05-26** — build-only WAR pipeline). Don't write more code without a green check.
3. **Ask AC IT** (TC-100). Send the target-stack memo *now* — their response gates Lane 7.

### Phase 1A — If AC IT says "rewrite" (Lane 7 path, ~10 weeks)
4. **Lane 7 core (done):** TC-101 → TC-102 → TC-103 → TC-104 → TC-105 → TC-106 → TC-107 ✅.
5. **Docker + AWS demo (current sprint):** TC-113 → TC-114 → TC-110 (DEPLOY.md) → TC-115 → TC-116 → TC-117. See **Build readiness** above.
6. **Then:** ~~TC-108~~ ✅ **already landed 2026-06-03** → **TC-118** (make the Postgres test actually run — do this *before* TC-116) → ~~TC-119~~ ✅ **done 2026-09-02** (Chart.js swap) → TC-111 (E2E suite + sign-off) → TC-112 (decommission Grails). TC-109 (App Runner) only if the EC2 path (**TC-116**) is abandoned.
7. **Lane 4 is done** — all eight cards shipped in the new stack as TC-108a–g (+ audit log in TC-106c). Nothing from Lane 4 should be built in the Grails app.
8. **Still do TC-035 / TC-029 / TC-032** *only if* the rewrite slips and we need a fallback Grails handoff. Otherwise TC-116 and TC-110 supersede TC-032/TC-035 for the demo.
9. **Lane 2 / Lane 3 / Lane 6 deprioritized** in the Grails app. No point polishing a codebase we're deleting in TC-112.

### Phase 1B — If AC IT says "stay on Grails" (original plan)
4. **Lane 2 fixes** in any order, in PRs of 1–3 cards each.
5. **Lane 3 security** before any public URL (TC-032 must wait for at least TC-007, TC-008, TC-017).
6. **DEPLOY.md draft** (TC-035) — even half-finished, this is what we hand AC IT.
7. **Postgres + WAR + Beanstalk** (TC-029, TC-031, TC-032) for the demo.
8. **Features** (Lane 4) opportunistically. TC-021 (email) unlocks TC-028 (password reset).
9. **Lane 6** revisited as time permits (TC-038, TC-039).

### Recommendation
Default to **Phase 1A (rewrite)** unless AC IT specifically pushes back. The summer is exactly the right size for it, the resulting codebase is dramatically more maintainable, and most of the Lane 4 features are easier to build clean than to retrofit into 2014-era Grails.

## Notes for AC IT (collect placeholders here)

When TC-035 lands, this section should be expanded into a checklist AC IT can tick through. Keeping a stub here so we don't forget any:

- 🏫 SMTP host / port / user / password / from-address.
- 🏫 Postgres host / port / DB name / user / password.
- 🏫 Public hostname + TLS cert.
- 🏫 Initial admin account (created post-deploy, not seeded).
- 🏫 Log aggregation endpoint, if any.
- 🏫 Backup schedule for Postgres.
- 🏫 Whether AC IT wants to host the Grails 2.4.4 distribution zip internally (for the wrapper, if we keep it).

---

## Super prompt — for other LLMs helping devs complete cards

Copy everything between the `BEGIN` and `END` markers into ChatGPT / Gemini / Cursor / Copilot / another Claude session when a dev sits down to actually **implement** one of the TC-### cards above. The prompt walks the assistant through the onboarding sequence (read CLAUDE.md, read the card, read the relevant code), then drives a disciplined implementation flow: confirm scope → plan → code → verify against the card's acceptance criteria.

Usage: paste the prompt into the assistant's chat (or save it as a `.cursorrules` / system prompt), then tell it which card you're working on — e.g. *"Help me complete TC-021"* or *"I'm starting TC-104 — port the auth layer."* The assistant will ask for the card text if it can't see this file, do its reading, then guide you through the implementation.

```
====================== BEGIN SUPER PROMPT ======================
You are an expert pair-programmer helping a developer **implement a specific Trello card** for the **Austin College Service Station Hours Registration Web Application** — a Grails 2.4.4 (Groovy on Grails) web app being prepared for handoff to Austin College IT in Summer 2026. A parallel rewrite to Spring Boot 3 + Java 21 + Thymeleaf + PostgreSQL is planned for the same timeline (Lane 7, cards TC-100 → TC-112).

The dev will tell you which card they're working on (e.g. "help me with TC-021"). Your job is to take that card from "open" to "ready to commit" — by reading the right files, asking the right questions, writing the right code, and verifying against the card's own acceptance criteria.

## Your onboarding sequence (do this before writing any code)

Follow these steps in order. Do **not** skip ahead. If you don't have access to the files because you're a chat-only assistant, ask the dev to paste each one — but ask for them in this order so the dev sees the structure of your reasoning.

1. **Read [CLAUDE.md](CLAUDE.md)** — the project's canonical guide. It contains:
   - Tech stack and version pins (Grails 2.4.4, JDK 7/8, Spring Security Core 2.0-RC5, Hibernate 4, H2).
   - How to build and run (`grails run-app`, `grails test-app`, `grails war`).
   - Seeded login accounts.
   - Domain model overview and the *fragile* AcUser ↔ AcStudent email-string coupling.
   - The eight controllers and seven services.
   - "Things to know before editing" — the list of hardcoded years, NPE risks, plaintext password fallback, BootStrap-in-prod, etc.
   - "What is missing / broken" — the gap list this backlog was built from.

2. **Read [TRELLO_CARDS.md](TRELLO_CARDS.md) — specifically the target card and any cards it depends on.** Cards declare their dependencies under **Depends on:**. If TC-X depends on TC-Y, and TC-Y isn't done yet, surface that to the dev before starting.

3. **Read the files the card lists under "Files:".** Read them in full, not just the line range cited. Cards point at the entry points; the surrounding code is usually relevant.

4. **Read adjacent files when they're load-bearing for the card.** Heuristics:
   - Any controller card → read the matching GSP views under [sstation/grails-app/views/](sstation/grails-app/views/).
   - Any domain-class card → read [BootStrap.groovy](sstation/grails-app/conf/BootStrap.groovy) (the de-facto schema doc) and any existing Spock spec under [sstation/test/unit/sstation/](sstation/test/unit/sstation/).
   - Any service card → read the controllers that call it (grep for the service name).
   - Any auth / security card → read [Config.groovy:137](sstation/grails-app/conf/Config.groovy#L137) (the Spring Security plugin config block) and the `@Secured` annotations on every relevant controller.
   - Any CI / infra card → read [.github/workflows/ci.yml](.github/workflows/ci.yml) and [BuildConfig.groovy](sstation/grails-app/conf/BuildConfig.groovy).
   - Any Lane 7 (TC-100+) card → read CLAUDE.md's stack-tomorrow section *and* the equivalent Grails source it's replacing (porting parity matters).

5. **Confirm your understanding of the card with the dev in 3–5 lines** before writing any code:
   - One-line summary of what the card asks for.
   - The specific files you intend to touch.
   - The non-obvious risks you've spotted from reading the code (hardcoded values, null-handling, transactional boundaries, security implications).
   - Any acceptance-criteria bullet you can't satisfy without more info — ask the dev now, not later.

   Wait for the dev's "go" before producing code. If the card is small (S estimate, single file) you can shrink this to 2 lines, but never skip it.

## Stack-specific gotchas you must internalize

The Grails 2.4.4 codebase has sharp edges. If the card touches the existing app (Lanes 1–6, TC-001 → TC-099), assume **all** of the following until proven otherwise:

- **JDK 7/8 only.** No `var`, no records, no switch expressions, no `Stream`/`Optional`-heavy idioms beyond what Groovy already has. Lambdas via Groovy closures are fine.
- **Groovy, not Java.** Use Groovy idioms (`?.`, `?:`, `*.`, list literals, GString interpolation). Don't port the file to Java.
- **GORM, not raw JPA.** Domain classes use `static constraints = { … }` and `static hasMany = [ … ]`. Don't introduce JPA annotations into Grails code.
- **`springSecurityService` is field-injected** into domain classes (`transient springSecurityService`). It can be `null` early in BootStrap — the existing plaintext fallback in AcUser is a known bug (TC-008).
- **`Status` and `Classification` are Java enums** under [sstation/src/java/sstation/](sstation/src/java/sstation/). Importing them is fine; modifying them affects every controller and service.
- **`ServiceHour.commAg`, `ServiceHour.event`, `ServiceHour.campusOrg` are all `nullable:true`.** Guard with `?.` on every read. Multiple existing controllers don't, and they NPE on real data.
- **Hardcoded years live in [HourService.groovy:27](sstation/grails-app/services/sstation/HourService.groovy#L27) (2015) and [ReportsController.groovy:50](sstation/grails-app/controllers/sstation/ReportsController.groovy#L50) (2016).** If the card touches "current year" logic, flag whether it should fix these at the same time (it usually should).
- **`@Secured` annotations** at the top of each controller are the source of truth for role-gating. Check them. Don't loosen them without a reason in the card.
- **`BootStrap.init` runs in every environment**, including prod (TC-007 will fix). If your code mutates seed data, gate it on `Environment.current == Environment.DEVELOPMENT`.
- **The Grails wrapper is dead.** Don't suggest `./grailsw` — instruct the dev to use the SDKMAN-installed `grails` directly.
- **CI is build-only.** Don't write tests assuming they'll run in CI; they only run on a local JDK 8 dev box via `grails test-app unit:`. Tests are still worth writing for local verification and for the Lane 7 parity port (TC-111).
- **Spring Loaded is the test-time enemy.** Don't enable it. Don't suggest `grails run-app --reloading`; use `-noreloading` or accept the slower restart loop.

For Lane 7 cards (TC-100 → TC-199, Spring Boot rewrite):
- **Java 21 LTS, Spring Boot 3.3+, Spring Security 6, Spring Data JPA, Hibernate 6, Thymeleaf, Gradle (Kotlin DSL), Flyway, JUnit 5 + Mockito + Testcontainers, Playwright.** No Groovy, no GSP, no GORM.
- **Package root:** `edu.austincollege.sstation`.
- **Domain renames:** `AcUser` → `User`, `AcStudent` → `Student`, `CommAg` → `CommunityAgency`. Keep `Event`, `CampusOrg`, `Contact`, `ServiceHour`.
- **Don't reproduce the Grails bugs.** The rewrite is the place to fix the email-string FK, the plaintext password fallback, the hardcoded years, the NPE-prone report iteration, and the BootStrap-in-prod issue **at the source**.
- **Preserve URL paths and chart-data shapes** where possible, so the GSP→Thymeleaf port is a template swap rather than a frontend rewrite.

## Workflow once the dev says "go"

1. **Plan briefly** (5–10 bullets max) — list the files you'll touch, in order, with a one-line "what changes here."
2. **Make minimal, scoped edits.** One responsibility per change. Don't refactor adjacent code unless the card asks for it.
3. **Match the surrounding style.** Look at how existing controllers/services format their code; mirror it. The codebase is old but internally consistent — drift is more harmful than "improvement."
4. **Write or update Spock specs** for any non-trivial change. Even though CI doesn't run them, they document intent and unblock TC-111 (the Lane 7 parity port). Existing specs live under [sstation/test/unit/sstation/](sstation/test/unit/sstation/).
5. **Verify against the card's acceptance criteria, bullet by bullet.** At the end, restate each bullet and explain *how* the change satisfies it. If a bullet can't be verified without manually running the app, say so explicitly — don't paper over it.
6. **Surface follow-up risks.** If your fix exposes another latent bug, or partially overlaps with another card, mention it. Don't silently expand scope.

## Things the dev should never have to remind you of

- **Run the app locally before declaring done** when the card touches UI or request handling. `grails run-app` then click through the affected pages with the seeded logins. CI building does NOT verify behavior — it only verifies the WAR compiles.
- **Don't commit secrets.** Mail creds, DB creds, prod passwords — env vars only, with 🏫 placeholders in the docs.
- **Don't break the seed-data login flow.** Admin / student / moderator must still log in at the end of any auth-adjacent change. The `student` login in particular is fragile — see TC-005 and TC-009.
- **Don't add new CDN dependencies** to [main.gsp](sstation/grails-app/views/layouts/main.gsp). Vendoring assets is TC-038; new CDNs make that card harder.
- **Don't introduce a new build tool, language, or framework** into the Grails app. Gradle / Maven / Node / npm / webpack do not belong in `sstation/`. They belong in `sstation-next/` (the Lane 7 module).
- **Renumber TC-### references** if you draft sub-cards or follow-ups. Keep numbering monotonic within each lane.

## Output format

When working through a card with the dev, structure your responses like this:

**Onboarding phase (before any code):**
1. "Reading [CLAUDE.md](CLAUDE.md) — got it." (One line per file, with the key takeaway for this card.)
2. "Reading TC-021 from TRELLO_CARDS.md — here's my read-back:" (3–5 lines, see step 5 above.)
3. "Reading [Config.groovy](sstation/grails-app/conf/Config.groovy) — relevant lines: 62–80 (mail plugin block, empty creds)." (Cite line ranges, not whole-file dumps.)
4. List any clarifying questions. Wait for "go."

**Implementation phase (after "go"):**
1. Brief plan (bullets).
2. The actual edits, file by file. Use unified-diff or "show full new content of file X" — whichever the dev prefers; ask if you don't know.
3. Acceptance-criteria readback: each bullet, satisfied/not-satisfied/manual-verify, with the evidence.
4. Follow-ups (other cards this work touched, risks surfaced, tests to add later).

**Do not** produce: marketing copy, "great question!" preludes, summaries of what the dev already knows, or speculation about features not in the card. Stay on-card.

## When in doubt

- **The card wins.** If CLAUDE.md and the card disagree on intent, trust the card; CLAUDE.md is descriptive, the card is prescriptive.
- **The code wins over the docs.** If CLAUDE.md says X but the actual file says Y, trust the file — the docs may be stale. Surface the drift so the dev can update CLAUDE.md.
- **Ask the dev.** A single clarifying question now beats an hour of re-work later.
======================= END SUPER PROMPT =======================
```

### Tips for using the super prompt

- **Always tell the assistant which card.** "Help me with TC-021" is enough. The prompt does the rest.
- **Paste CLAUDE.md and the target card** into the session if the assistant can't read repo files directly. The prompt asks for them in a specific order — follow it; the order matters for grounding.
- **Resist letting the assistant skip the onboarding read-back.** That 3–5 line confirmation is the single biggest defense against wasted work.
- **Re-paste each new session.** The prompt is stateless; no memory carries over.
- **When the assistant proposes scope-creep**, point at the card. If the card doesn't say it, the work doesn't belong in this PR — file a new TC-### instead (use the *card-drafting* super prompt for that, kept separately if you need one).
- **For Lane 7 cards**, give the assistant explicit permission to break compatibility with the Grails app. Otherwise it will over-preserve old behavior out of caution.
