# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

The **Austin College Service Station Hours Registration Web App** — a Grails 2.4.4 (Groovy on Grails) web application that replaces paper-based volunteer/service-hour tracking for the Austin College Service Station office.

It is a class project from around 2015–2016. The codebase is **frozen on a very old stack**: Grails 2.4.4, Spring Security Core 2.0‑RC5, Hibernate 4.3, jQuery 1.11, Bootstrap 3.3, Highcharts, H2 database. There is no Gradle build — Grails 2.x uses its own CLI/wrapper. JDK 7/8 era code (`source.level = 1.6` in [BuildConfig.groovy](sstation/grails-app/conf/BuildConfig.groovy)).

## How to run it right now

There is **no Gradle/Maven setup** — this is a Grails 2.4.4 app. To bring it up locally:

### Prerequisites
- **JDK 7 or 8** (JDK 11+ will not work with Grails 2.4.4; the Groovy/Spring versions bundled here are incompatible with later JVMs).
- **Grails 2.4.4** installed. SDKMAN is the easiest path: `sdk install grails 2.4.4`. Do **not** rely on the bundled wrapper ([grailsw.bat](sstation/grailsw.bat)) — [wrapper/grails-wrapper.properties](sstation/wrapper/grails-wrapper.properties) points at `dist.springframework.org.s3.amazonaws.com` which no longer serves these artifacts.
- `JAVA_HOME` set to the JDK 7/8 install; `grails -version` should report 2.4.4.

### Commands (run from the [sstation/](sstation/) directory)
- `grails run-app` — start the dev server (defaults to `http://localhost:8080/sstation`).
- `grails test-app` — run all unit tests under [sstation/test/unit/](sstation/test/unit/).
- `grails test-app unit:` — only unit phase.
- `grails test-app sstation.HomeControllerSpec` — run a single Spock spec.
- `grails clean` — wipe `target/` build output.
- `grails war` — produce a deployable WAR.

### Seeded login accounts
[BootStrap.groovy](sstation/grails-app/conf/BootStrap.groovy) populates the in‑memory H2 dev DB on every startup with three users and ~10 random students, each with ~10 randomized service hours:

| Username    | Password           | Role            |
|-------------|--------------------|-----------------|
| `admin`     | `admin_secret`     | `ROLE_ADMIN`    |
| `student`   | `student_secret`   | `ROLE_STUDENT`  |
| `moderator` | `moderator_secret` | `ROLE_MODERATOR`|

Login redirects through Spring Security's auth controller. All three logins work against the seed data. The `student` user resolves via `AcStudent.findByAcEmail("student@austincollege.edu")` — a deterministic `AcStudent` (Sam Student, ACID AC50000) with 5 seeded `ServiceHour` records is created by BootStrap (TC-005). The email-suffix lookup is still a string match with no FK — see TC-009 for the full fix.

### Selenium tests
[selinium_tests/](selinium_tests/) (note the typo) holds Selenium IDE `.html`/folder exports — they require Selenium IDE (legacy Firefox) and a running app. They are not wired into `grails test-app`.

## High-level architecture

### Tech stack at a glance
- **Framework:** Grails 2.4.4 on Spring MVC (Groovy on Grails — the older, pre-Spring-Boot incarnation).
- **Persistence:** GORM over Hibernate 4, against H2. Dev/test are in‑memory (`create-drop` / `update`); production writes to file `prodDb` ([DataSource.groovy](sstation/grails-app/conf/DataSource.groovy)).
- **Security:** `spring-security-core` plugin. Auth uses `AcUser` / `AcRole` / `AcUserAcRole` join (see [Config.groovy:137](sstation/grails-app/conf/Config.groovy#L137)). Controllers gate access via `@Secured(['ROLE_...'])` annotations.
- **Frontend:** GSP server-rendered views + jQuery, jQuery UI, Bootstrap 3, DataTables, Highcharts — all loaded via CDN from [main.gsp](sstation/grails-app/views/layouts/main.gsp). The asset pipeline plugin is enabled but most assets are CDN.
- **Other plugins of note:** `mail` (config in [Config.groovy:62](sstation/grails-app/conf/Config.groovy#L62) — credentials blank), `csv` (for report export), `database-migration`, `cache`.

### Domain model — the conceptual core
All in [grails-app/domain/sstation/](sstation/grails-app/domain/sstation/). Read [BootStrap.groovy](sstation/grails-app/conf/BootStrap.groovy) for how these relate in practice.

- **`AcUser` / `AcRole` / `AcUserAcRole`** — Spring Security auth tables. Username + bcrypt-encoded password. `encodePassword()` now throws `IllegalStateException` if `springSecurityService` is null instead of silently storing plaintext (TC-008).
- **`AcStudent`** — the student profile (separate from `AcUser` — they are *not* linked by FK, only loosely by email). `hasMany serviceHours`. Carries `Classification` enum (`FR/SO/JR/SR/OTHER`) and an `isModerator` flag that intersects with the role system.
- **`ServiceHour`** — the central transactional record. `belongsTo AcStudent`, references one `Event`, one `CampusOrg`, one `CommAg`, and has a `Status` enum (`PENDING/APPROVED/REJECTED` — defined as a Java enum in [src/java/sstation/Status.java](sstation/src/java/sstation/Status.java)). Most relations are nullable (see constraints).
- **`Event`** — service event (e.g. "Great Day of Service", "JanServe").
- **`CampusOrg`** — Austin College student organization that ran the event (THINK, BIG, APO, etc.).
- **`CommAg`** — community/non-profit agency the service was performed for.
- **`Contact`** — generic contact info value object.

The student–record relationship is fragile: there is no direct FK from `AcUser` to `AcStudent`, only a string-match on email. The student dashboard depends on this match. Be careful when touching login flow.

### Controllers and the request map
URL routing is the default `"/$controller/$action?/$id?"` mapping plus `"/"` → `home/index` ([UrlMappings.groovy](sstation/grails-app/conf/UrlMappings.groovy)). The eight controllers in [grails-app/controllers/sstation/](sstation/grails-app/controllers/sstation/) are:

- **`HomeController`** — dashboard. Redirects students to `acStudent/home`, admins to `home/adminHome`. Hosts the AJAX endpoint `ajaxUpdateStatus` used by the quick-approve dialog.
- **`AcStudentController`** — the largest controller. CRUD on students, CRUD on a student's service hours, plus the student-facing report view. Uses `studentService` for CSV upload, `mailService` for notifications, `springSecurityService` to identify the current user.
- **`HourController`** — admin-facing hour list (overall + pending), edit/create hour fragments.
- **`EventController`, `CommOrgController`, `ACGroupController`** — standard CRUD on `Event`, `CommAg`, `CampusOrg`.
- **`ModeratorController`** — admin-only (`@Secured(['ROLE_ADMIN'])`). Promotes/demotes students to moderator by toggling `AcStudent.isModerator`.
- **`ReportsController`** — admin/moderator dashboards. Generates Highcharts data for summary, semester (Fall/Janterm/Spring/Summer), year, event, community-org, and campus-org reports.

GSP views mirror the controller names under [grails-app/views/](sstation/grails-app/views/). Filenames starting with `_` are partials/fragments rendered into a parent page (typically via AJAX from a button or jQuery dialog).

### Services and where logic actually lives
Services in [grails-app/services/sstation/](sstation/grails-app/services/sstation/) hold the statistics math; controllers mostly assemble models for views.

- **`HourService`** — global service-hour KPIs (totals, 5-year trend, by classification, by status). Powers the dashboard cards.
- **`StationReportService`** — aggregates by year/event used by `ReportsController`.
- **`ReportService`** — per-student summaries.
- **`CampusOrgReportService`, `CommAgReportService`, `EventReportService`** — `new`’d directly inside `ReportsController` (not injected); each holds a target entity + the full `ServiceHour` list and yields totals by year. They are stateful, **not** Spring-managed in the usual sense — keep that in mind when refactoring.
- **`StudentService`** — CSV student upload helper.

### Things to know before editing

- **`HourService.init()` derives `currentYear` from `Calendar.getInstance()`** — fixed TC-001 (2026-05-29). KPIs now reflect the actual current year.
- **`ReportsController.summaryReport` defaults to the current year** and accepts a `year` param — fixed TC-002 (2026-05-29). A year selector is rendered on the GSP.
- **`ReportsController.summaryReport` / `semesterReport` use `constant = [5, allAgs.size(), allOrgs.size(), allEvs.size()].min()`** — fixed TC-003 (2026-05-29). No more IndexOutOfBounds with sparse data.
- **`semesterReport` uses `?.` on `s.commAg?.name`, `s.event?.name`, `s.campusOrg?.name`** — fixed TC-006 (2026-05-29). Still, all three fields remain `nullable:true` in `ServiceHour` constraints — always use `?.` in new code.
- **`BootStrap.init` is gated on `Environment.current != Environment.PRODUCTION`** — fixed TC-007 (2026-05-29). Roles are created idempotently in every environment; test users and random data are dev/test only.
- **`AcUser.encodePassword` throws `IllegalStateException` if `springSecurityService` is null** — fixed TC-008 (2026-05-29). No plaintext fallback.
- **Mail plugin credentials in [Config.groovy:62](sstation/grails-app/conf/Config.groovy#L62) are blank** — email actions will fail until populated (TC-021).
- **`CampusOrgReportService`, `CommAgReportService`, `EventReportService`** still dereference `s.commAg.name` unguarded internally — TC-010 covers the refactor. Don't call `ServiceHour.list()` inside their loops without checking for nulls.

## What the app already does (today)

Working flows you can demo against the seed data:

- Three-role authentication (admin / moderator / student) via Spring Security. All three seeded logins work on a fresh run.
- Admin dashboard with KPIs, charts (by classification, by status, by year) reflecting the **current calendar year** — seed dates are spread across the last 5 years so data is always visible (TC-001, TC-004).
- Pending-hours queue with inline approve/reject.
- CRUD for: students, service hours, events, community agencies, campus orgs.
- Promote/demote students to moderator.
- Quick status updates via AJAX dialog (`ajaxUpdateStatus`).
- Reports: summary (with a year selector), by year (5-year window), by semester (Fall/Janterm/Spring/Summer), per event, per community agency, per campus org — rendered as Highcharts. Reports are bounds-safe and null-safe (TC-002, TC-003, TC-006).
- Per-student report view with their own hours. The `student` / `student_secret` login lands on Sam Student's dashboard with 5 seeded hours (TC-005).
- Plugin scaffolding for CSV upload (`StudentService`) and email (`mail` plugin).
- GitHub Actions build-only WAR pipeline (TC-034, landed 2026-05-26).
- Spock unit-test specs for most domain classes and several controllers under [sstation/test/unit/sstation/](sstation/test/unit/sstation/).
- Selenium IDE regression scripts checked in under [selinium_tests/](selinium_tests/).

## What is missing / broken / "TODO"

Concrete gaps you can confirm by reading the code:

- ~~**Hardcoded years.**~~ **Fixed (TC-001, TC-002, TC-004 — 2026-05-29).** `HourService` and `ReportsController.summaryReport` now use the current year; seed dates span the last 5 years.
- **Student login uses an email-string match** (`username + "@austincollege.edu"`) — no `AcUser → AcStudent` FK. The `student` login now lands correctly (TC-005), but the underlying coupling is fragile. TC-009 adds a real FK.
- ~~**No `AcStudent` for the seeded `student` user.**~~ **Fixed (TC-005 — 2026-05-29).** Sam Student (AC50000) is seeded with 5 service hours.
- **README workflow section is empty**, and the README description of `CampusOrg` is truncated (`A CampusOrg class includes`). See TC-037.
- ~~**`BootStrap.init` runs in production.**~~ **Fixed (TC-007 — 2026-05-29).** Seed data is gated on non-production; roles are created idempotently everywhere.
- ~~**`AcUser` plaintext password fallback.**~~ **Fixed (TC-008 — 2026-05-29).** `encodePassword` now fails fast with `IllegalStateException`.
- **Mail credentials missing** in [Config.groovy](sstation/grails-app/conf/Config.groovy) — any feature that sends mail is non-functional. TC-021.
- ~~**NPE-prone report iteration**~~ **Fixed (TC-006 — 2026-05-29).** `semesterReport` uses `?.` for all nullable FK accesses. Note: the three `*ReportService` helpers still dereference `.name` unguarded — TC-010.
- ~~**`IndexOutOfBoundsException` risk** in `summaryReport`/`semesterReport`.~~ **Fixed (TC-003 — 2026-05-29).** `constant` is now `min(5, list sizes)`.
- **`selinium_tests/` is misspelled** and not wired into CI. TC-015 / TC-039.
- ~~**No CI/build pipeline.**~~ **Fixed (TC-034 — 2026-05-26).** Build-only WAR pipeline via GitHub Actions. Test execution still requires a local JDK 8 machine (see TC-034 scope reduction).
- **Stack is end-of-life.** Grails 2.4.4 is unsupported; Spring Security plugin 2.0‑RC5 is a release candidate; jQuery 1.11 / Bootstrap 3 are out of support; H2 versions in this era have known CVEs. Lane 7 (TC-100–TC-112) is the rewrite path.

## Trello backlog (see TRELLO_CARDS.md for full details)

The full prioritized backlog lives in [TRELLO_CARDS.md](TRELLO_CARDS.md). Quick reference:

**Lane 1 — Critical fixes** ✅ All landed 2026-05-29 (`lane-01-critical-fixes`)
- TC-001 Hardcoded `currentYear = 2015` in HourService ✅
- TC-002 Hardcoded `year = 2016` in ReportsController.summaryReport ✅
- TC-003 IndexOutOfBounds risk in summaryReport/semesterReport ✅
- TC-004 Seed dates in years 2011–2015 due to legacy Date constructor ✅
- TC-005 No AcStudent for the `student` test login ✅
- TC-006 NPE on nullable commAg/event/campusOrg in semesterReport ✅
- TC-007 BootStrap.init runs in production ✅
- TC-008 Plaintext password fallback in AcUser ✅

**Lane 2 — Next up:** TC-009 (AcUser↔AcStudent FK), TC-010 (refactor report services), TC-033 (Dockerize).
**Lane 5 — CI:** TC-034 build-only WAR pipeline ✅ (2026-05-26).
**Lane 7 — Rewrite:** Spring Boot 3 / Java 21 / Thymeleaf / Postgres (TC-100–TC-112, Summer 2026).

## Pointers for working in this repo

- The interesting business logic lives in **services**, not controllers. Start there when answering "how does X get computed".
- The interesting domain coupling lives in **`ServiceHour`** — it's the join table in spirit. Almost every report iterates `ServiceHour.list()`.
- **GSP partials** (`_foo.gsp`) are usually loaded by AJAX. Search the corresponding controller for `render view:"_foo"` to find the action that serves them.
- **`@Secured` annotations** at the top of each controller are the source of truth for which role can hit which endpoint — check them before assuming behavior.
- Use [BootStrap.groovy](sstation/grails-app/conf/BootStrap.groovy) as the de-facto schema documentation: it shows every domain object being instantiated with realistic fields.
