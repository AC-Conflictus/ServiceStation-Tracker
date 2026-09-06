# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

The **Austin College Service Station Hours Registration Web App** — a Grails 2.4.4 (Groovy on Grails) web application that replaces paper-based volunteer/service-hour tracking for the Austin College Service Station office.

It is a class project from around 2015–2016. The codebase is **frozen on a very old stack**: Grails 2.4.4, Spring Security Core 2.0‑RC5, Hibernate 4.3, jQuery 1.11, Bootstrap 3.3, Highcharts, H2 database. There is no Gradle build — Grails 2.x uses its own CLI/wrapper. JDK 7/8 era code (`source.level = 1.6` in [BuildConfig.groovy](sstation/grails-app/conf/BuildConfig.groovy)).

> **⚠️ There are now TWO apps in this repo.** The legacy Grails app lives in [sstation/](sstation/) and remains the shippable application. A **Spring Boot 3 / Java 21 rewrite** (Lane 7) is underway in [sstation-next/](sstation-next/) and is now **feature-complete**: full domain model, authentication, every read-only view, all CRUD, the Bootstrap 5 frontend, all eight Lane 4 features (TC-108), and container packaging (TC-113/TC-114). What remains is deployment, verification, and handoff — not features. See **[The Lane 7 rewrite module](#the-lane-7-rewrite-module-sstation-next--spring-boot-3--java-21)** below, and **[demo.md](demo.md)** to run either app locally. The Grails app stays the source of truth until the rewrite reaches parity (TC-111) and is decommissioned (TC-112).

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

## The Lane 7 rewrite module (`sstation-next/`) — Spring Boot 3 / Java 21

The parallel rewrite lives in [sstation-next/](sstation-next/). It is **not yet a replacement** for the Grails app — but as of **2026-06-03** the foundation and all read paths are done. See [demo.md](demo.md) for a click-through guide and [sstation-next/README-NEXT.md](sstation-next/README-NEXT.md) for the module's own readme.

### Stack
- **Java 21 LTS** + **Spring Boot 3.3.5** + **Spring Security 6** + **Spring Data JPA / Hibernate 6**.
- **Thymeleaf** server-rendered templates (1:1 port from GSP). **Flyway** migrations.
- **PostgreSQL** in prod; **H2 in PostgreSQL-compatibility mode** for local dev/test (no DB to install).
- **Gradle (Kotlin DSL)** wrapper build; **Spotless** (Google Java Format) wired into `check`.
- **JUnit 5 + Spring Test + spring-security-test + Testcontainers** for tests.
- Package root: `edu.austincollege.sstation`. Domain renames vs. Grails: `AcUser`→`User`, `AcStudent`→`Student`, `CommAg`→`CommunityAgency`; `Event`/`CampusOrg`/`Contact`/`ServiceHour` keep their names.

### How to run it (from [sstation-next/](sstation-next/))
- Needs **JDK 21** (e.g. `sdk install java 21.0.5-tem`). The Gradle wrapper handles Gradle.
- `./gradlew bootRun` — dev server on `http://localhost:8080` (the **`dev` profile is auto-activated**, so the seeded `admin` / `student` / `moderator` accounts and demo data exist). Same passwords as the Grails table above.
- `./gradlew check` — Spotless format check + all JUnit 5 tests. This is the CI gate ([.github/workflows/ci-next.yml](.github/workflows/ci-next.yml)).
- `./gradlew spotlessApply` — auto-format. `./gradlew bootJar` — build the executable JAR.

### Layout
- `src/main/java/edu/austincollege/sstation/`
  - `domain/` — **12 JPA entities** (`User`, `Role`, `UserRole`, `Student`, `ServiceHour`, `ServiceHourAuditLog`, `Event`, `EventSignup`, `CampusOrg`, `CommunityAgency`, `Contact`, `PasswordResetToken`) + 3 enums (`Status`, `Classification`, `SignupStatus`).
  - `repository/` — Spring Data repositories.
  - `service/` — read: `StatsService`, `ReportService`, `StudentStatsService` (+ their `*Data` record DTOs); write: `ReferenceCrudService` (detach-on-delete), `StudentCrudService`, `AuditService`; features: `NotificationService` (mail), `ReportCsvService` (OpenCSV), `StudentReportPdfService` (openhtmltopdf), `EventSignupService`, `PasswordResetService`.
  - `security/` — `SecurityConfig`, `CustomUserDetailsService`.
  - `config/` — `DevDataSeeder` (roles + 3 users from env vars) and `DemoDataSeeder` (orgs/events/agencies + random students/hours); plus `DemoAccountSeeder` (**`@Profile("demo")`**, for the container/AWS showcase — fails fast without `SSTATION_DEMO_*_PASSWORD`). `DevDataSeeder` is `dev`-only; `DemoDataSeeder` runs under **both** `dev` and `demo`.
  - `web/` — read: `HomeController` (role-routes `/`), `AdminController`, `ReportsController`, `StudentController`, `LoginController`. CRUD: `EventController`, `CampusOrgController`, `CommunityAgencyController`, `StudentAdminController`, `ModeratorController`, `HourController` (+ `ServiceHourForm`). Features: `EventSignupController`, `PasswordResetController`, `CsvDownloads`.
- `src/main/resources/` — `application.yml`, `templates/` (Thymeleaf; all pages decorate `templates/fragments/layout.html`), `db/migration/` **`V1__initial_schema.sql` → `V4__password_reset_tokens.sql`** (V2 audit log, V3 event signups, V4 reset tokens). Frontend assets are vendored via WebJars (no `static/` blobs), served at `/webjars/**`.
- `Dockerfile`, `docker-compose.yml` / `.dev.yml` / `.ec2.yml`, `.env.example` — container packaging (TC-113/TC-114). See [DEPLOY.md](DEPLOY.md).

### What's ported (Lane 7 progress, all verified `./gradlew check` green + live)
- **TC-100** — AC IT stack memo ([docs/TC-100-stack-memo.md](docs/TC-100-stack-memo.md)). **Closed 2026-09-02 as decided-by-default** — no written reply, but the Service Station office has agreed and the stack is settled. Its one residual item with teeth — Highcharts licensing — was **closed by TC-119** (swapped to Chart.js, MIT).
- **TC-101 / TC-102** — scaffold + parallel CI (`ci-next.yml`, path-filtered to `sstation-next/**`; the Grails `ci.yml` is untouched).
- **TC-103** — domain model. **Real `@ManyToOne` `User → Student` FK** (fixes the email-string hack at the source — TC-009). `User` no longer self-encodes its password (kills the TC-008 plaintext fallback at the source). `ServiceHour.{campusOrg, commAg, event}` kept **nullable** by decision; the owning `student` is required. Flyway `V1` is the schema source of truth (Hibernate runs `ddl-auto=validate`).
- **TC-104** — Spring Security 6: form login + logout, **BCrypt** (delegating `{bcrypt}` encoder), **CSRF on**, `@EnableMethodSecurity` + `@PreAuthorize` on controllers, dev-only seeded users with passwords from `SSTATION_DEV_*_PASSWORD` env vars.
- **TC-105** — all read-only views: admin dashboard (`StatsService`), the six reports (`ReportService`: summary, semester, year, event, community-org, campus-org), and the student dashboard + per-student report (`StudentStatsService`). **All "current year" logic uses `LocalDate.now()`** (TC-001/TC-002 at source), **top-N is bounds-safe** (`min(5, …)`, TC-003), **every nullable FK access is null-guarded** (TC-006/TC-010).
- **TC-106** — all write paths: CRUD for the five entity types (`/admin/{students,hours,events,campus-orgs,agencies}`), `@Valid` bean-validation re-rendering forms with field errors, the **quick approve/reject REST endpoint** `POST /admin/hours/{id}/status` (CSRF, ADMIN-only, returns JSON — replaces the Grails `ajaxUpdateStatus`), moderator promote/demote (`/admin/moderators`, ADMIN-only), and the **audit trail** (`ServiceHourAuditLog` + Flyway `V2`, written on every status change; admin-only per-hour view). Deleting a reference entity **detaches** it from its hours (nulls the FK) rather than cascade-deleting; deleting a student clears any linked `User.student` FK then cascade-removes their hours.
- **TC-107** — frontend: shared Thymeleaf layout ([templates/fragments/layout.html](sstation-next/src/main/resources/templates/fragments/layout.html)) decorated via `~{fragments/layout :: page(~{::title}, ~{::main})}`, **all** templates restyled with Bootstrap 5. **Assets are vendored via WebJars** (`/webjars/**`, no CDN — closes TC-038): Bootstrap 5.3.3, jQuery 3.7.1, DataTables 2.1.8, and originally Highcharts 11.2.0. Charts keep the same data shapes; DataTables powers the students list. **The charting library is now Chart.js 4.4.3 (MIT) — see TC-119.**

- **TC-108** — **all eight Lane 4 features**, in seven slices (a–g): (a) approve/reject **email notifications** via Spring Mail — `NotificationService`, env-driven SMTP, **log-only when `spring.mail.host` is empty**; (b) **bulk approve/reject** from the pending queue; (c) **CSV export** on all six reports + the student report (`ReportCsvService`, OpenCSV, RFC-4180 escaping); (d) **PDF export** of the per-student report (`StudentReportPdfService`, openhtmltopdf + jsoup, renders `templates/pdf/student-report.html`); (e) **date-range filter** on the admin dashboard; (f) **event sign-up flow** — `EventSignup` + Flyway `V3`, student `/student/events`, admin roster; (g) **self-service password reset** — `PasswordResetToken` + Flyway `V4`, SHA-256-hashed single-use tokens, 1-hour TTL, no user enumeration.
- **TC-113 / TC-114** — container packaging: multi-stage `Dockerfile` (JDK 21 build → JRE 21 Alpine run, non-root, healthcheck), three compose files, `.env.example`, and [DEPLOY.md](DEPLOY.md). `DemoAccountSeeder` (`@Profile("demo")`) **refuses to start without `SSTATION_DEMO_*_PASSWORD`**, so `admin_secret` can never reach a public host.
- **TC-120** — **error pages.** `templates/error/{403,404,500}.html` + a catch-all `templates/error.html`, all decorating the shared layout, plus `GlobalErrorAdvice` (uncaught exception → 500 view, logged at ERROR with method + path). API callers get JSON instead of a page: `JsonApiRequestMatcher` is the single definition of "this caller wants JSON", wired into the security `exceptionHandling` so unauthenticated JSON callers get **401 + JSON** and denied ones **403 + JSON**.

### Still to do in the rewrite
- **TC-121** 🔒 — no day-one admin bootstrap. A bare `prod` boot correctly seeds nothing, but nothing creates the first admin either, so AC IT's first login is against an empty user table. Blocks a real handoff.
- **TC-115 / TC-116** — CI Docker image build; AWS public demo on EC2 t3.micro + RDS (supersedes the App Runner plan in TC-109).
- **TC-110 / TC-111 / TC-112** — finish DEPLOY.md for AWS, E2E acceptance suite + stakeholder sign-off, decommission the Grails app.

### Rewrite gotchas to internalize
- **`bootRun` auto-activates the `dev` profile** (set in `build.gradle.kts`); prod runs with `-Dspring.profiles.active=prod` and **never seeds**. Prod needs `SSTATION_DB_URL` / `SSTATION_DB_USER` / `SSTATION_DB_PASSWORD`.
- The dev H2 mem-DB name is **randomized per application context** (`jdbc:h2:mem:sstation-${random.uuid}`) so a committing `@SpringBootTest` can't leak seed data into `@DataJpaTest` contexts. Don't "simplify" it back to a fixed name.
- Flyway `V1` is written in the **Postgres/H2-PG common subset**; keep new migrations in that subset so they run on both. Entities must stay in sync with the SQL because `ddl-auto=validate`.
- `@DataJpaTest`s use `@AutoConfigureTestDatabase(replace = NONE)` so they run against the real Flyway schema — a green run also proves entities ↔ migration agree.
- The current student is resolved via `UserRepository.findStudentByUsername` (a fetch query), **not** by touching the LAZY `User.student` proxy — doing the latter throws `LazyInitializationException`.
- **`open-in-view` is off.** Any view that walks a LAZY association (e.g. the hour list showing `student`/`event`/`org` names) must use a **fetch-join** repository query (`ServiceHourRepository.findAllWithRefs`, `…WithStudent`, audit `…WithActor`) — returning bare entities and dereferencing them in Thymeleaf throws `LazyInitializationException`.
- **Status changes are ADMIN-only and always audited.** Route every status mutation through `AuditService.record(...)`. The audit FK uses `ON DELETE CASCADE`, so deleting a hour (or its student) cleans up the audit rows without app code.
- CRUD forms bind to **form DTOs** where the entity shape doesn't fit a web form: `ServiceHourForm` (FK selects as ids + `datetime-local` → `LocalDateTime`). Controllers with optional text/number fields register a `StringTrimmerEditor(true)` via `@InitBinder` so empty inputs bind to `null`.
- **Every page decorates `fragments/layout.html`** via `th:replace="~{fragments/layout :: page(~{::title}, ~{::main})}"` — put the page's `<title>` in `<head>` and its content in `<main>`. The layout loads the **vendored** Bootstrap/jQuery/Chart.js/DataTables from `/webjars/**` (no CDN — TC-038/TC-107) and exposes the CSRF token as `<meta name="_csrf">` for JS (the hours quick-approve fetch reads it). Add new assets as **WebJar dependencies**, not `static/` files; confirm the exact in-jar path (`/webjars/<name>/<version>/…`) since it varies — Chart.js, for instance, needs the **UMD** build at `…/chartjs/4.4.3/dist/chart.umd.js`, because `dist/chart.js` is ESM and will not define the `Chart` global from a plain `<script>` tag.
- **The Postgres path is covered locally *and* in CI, and a skipped test now fails the build (TC-118).** Two tests touch real PostgreSQL via Testcontainers (`postgres:16-alpine`): `DemoProfileIntegrationTest` (profiles `prod,demo`) and `ProdProfileMigrationIntegrationTest` (bare `prod` on a **virgin** database — Flyway V1–V4 apply cleanly, `ddl-auto=validate` agrees, no seeder leaks into prod). They use separate containers on purpose; sharing one would make "virgin" false. Two things make this work and are easy to break: `build.gradle.kts` sets docker-java's **`api.version` system property to 1.41** on the test JVM (docker-java defaults to v1.32; Docker Engine 29's `MinAPIVersion` is 1.40, so `/v1.32/info` returns HTTP 400, every Testcontainers strategy fails, and `disabledWithoutDocker = true` turns that into a **silent skip under a BUILD SUCCESSFUL**), and Testcontainers is pinned via **`extra["testcontainers.version"]`** rather than explicit coordinates, because Spring Boot's BOM otherwise drags core back to 1.19.8 behind whatever version you declare. `check` also depends on **`verifyNoSkippedTests`**, which parses the JUnit XML and fails if *any* test class reports `skipped > 0` — the suite is **122/122 with zero skips** on both a dev box and the runner, so a skip is always signal. If that task fires, read its message before "fixing" the test.
- **There are three seeding profiles, not one.** `dev` (local H2, `DevDataSeeder` + `DemoDataSeeder`, passwords fall back to `admin_secret` etc.), `demo` (`DemoAccountSeeder` + `DemoDataSeeder`, **hard-fails without `SSTATION_DEMO_*_PASSWORD`** — this is what containers/AWS use, typically as `prod,demo`), and `prod` alone (**never seeds** — a bare `prod` boot gives you a login page with no accounts, which is the correct AC IT day-one behavior).
- **Email is log-only unless `spring.mail.host` is set.** `SSTATION_MAIL_HOST` defaults to empty so containers boot without SMTP, which means `JavaMailSender` is never auto-configured and `NotificationService` just logs. Approve/reject and password-reset mails will silently not send until AC IT supplies a relay — **the app gives no error**. Don't debug this as a mail bug.
- ⚠️ **Three sharp edges around error handling (TC-120) — all three were live bugs caught by tests, not review.**
  1. `@ExceptionHandler(Exception.class)` in `GlobalErrorAdvice` must **rethrow** Spring MVC's `ErrorResponse` family and Spring Security's exceptions. Swallowing `NoResourceFoundException` turns **every 404 into a 500**; swallowing `AccessDeniedException` breaks both the anonymous sign-in redirect and the 403 page, because `@ExceptionHandler` resolution runs inside the DispatcherServlet, *upstream* of `ExceptionTranslationFilter`. Rethrow the **same instance** — the resolver compares identity and leaves it unresolved without a spurious warning.
  2. `defaultAccessDeniedHandlerFor` / `defaultAuthenticationEntryPointFor` **ignore their matcher when they hold exactly one mapping** and apply the handler to every request. `SecurityConfig` therefore builds `DelegatingAuthenticationEntryPoint` / `RequestMatcherDelegatingAccessDeniedHandler` explicitly. Don't "simplify" it back.
  3. **`MockMvc` cannot test error pages.** It stops at the status code and never runs the ERROR dispatch, so it proves a request was rejected but not what the user sees. `ErrorPageIntegrationTest` uses a real server on a random port, driven by the **JDK HTTP client** — `HttpURLConnection` cannot complete a POST whose body was streamed when the server answers 401, which is one of the responses under test.
- **A browser `fetch()` sends `Accept: */*`, which counts as "wants HTML".** That is deliberate — only a client that explicitly asks for JSON gets a JSON error. So any new JS calling a JSON endpoint must set `Accept: application/json` itself (see `templates/hours/list.html`), or a signed-out session will hand it the sign-in page with status 200 and it will die inside `r.json()`.
- **Password-reset tokens are stored SHA-256-hashed, single-use, 1-hour TTL**, and `requestReset` deliberately reveals nothing about whether an account exists. Keep that property if you touch `PasswordResetService`.

## High-level architecture (the Grails app)

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
- **Mail credentials missing** in [Config.groovy](sstation/grails-app/conf/Config.groovy) — any feature that sends mail is non-functional. TC-021 (delivered in the rewrite as TC-108a; still blank here, and staying that way).
- ~~**NPE-prone report iteration**~~ **Fixed (TC-006 — 2026-05-29).** `semesterReport` uses `?.` for all nullable FK accesses. Note: the three `*ReportService` helpers still dereference `.name` unguarded — TC-010.
- ~~**`IndexOutOfBoundsException` risk** in `summaryReport`/`semesterReport`.~~ **Fixed (TC-003 — 2026-05-29).** `constant` is now `min(5, list sizes)`.
- **`selinium_tests/` is misspelled** and not wired into CI. TC-015 / TC-039.
- ~~**No CI/build pipeline.**~~ **Fixed (TC-034 — 2026-05-26).** Build-only WAR pipeline via GitHub Actions. Test execution still requires a local JDK 8 machine (see TC-034 scope reduction).
- **Stack is end-of-life.** Grails 2.4.4 is unsupported; Spring Security plugin 2.0‑RC5 is a release candidate; jQuery 1.11 / Bootstrap 3 are out of support; H2 versions in this era have known CVEs. Lane 7 (TC-100–TC-119) is the rewrite path.

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

**Lane 2 — Grails app, deprioritized:** TC-009 (AcUser↔AcStudent FK) and TC-010 (refactor report services) are both fixed **at the source** in the rewrite, and TC-033 (Dockerize) is superseded by TC-113. Don't spend time here — TC-112 deletes this app.
**Lane 5 — CI:** TC-034 build-only WAR pipeline ✅ (2026-05-26).

**Lane 7 — Rewrite** (Spring Boot 3 / Java 21 / Thymeleaf / Postgres, in [sstation-next/](sstation-next/)):
- TC-100 stack memo ✅ · TC-101 scaffold ✅ · TC-102 parallel CI ✅ — landed 2026-06-02
- TC-103 JPA domain model ✅ · TC-104 Spring Security 6 auth ✅ — landed 2026-06-02
- TC-105 read-only views (admin dashboard + 6 reports + student dashboard/report) ✅ — landed 2026-06-03
- TC-106 CRUD + quick approve/reject REST + moderator promote/demote + audit trail ✅ — landed 2026-06-03
- TC-107 Thymeleaf layout + Bootstrap 5 + vendored WebJars (no CDN) ✅ — landed 2026-06-03
- TC-108 all eight Lane 4 features in slices a–g (mail, bulk approve, CSV, PDF, date filter, event sign-up, password reset) ✅ — landed 2026-06-03
- TC-113 Docker packaging + TC-114 demo-profile seeder ✅ — landed 2026-06-04, merged to main 2026-09-02
- TC-108 Lane 4 features, all eight, in slices a–g ✅ — landed 2026-06-03
- TC-113 Docker packaging + TC-114 demo-profile seeder ✅ — landed 2026-06-04 (merged to main 2026-09-02)
- TC-119 Highcharts → Chart.js (MIT) swap ✅ — landed 2026-09-02
- TC-118 Postgres tests actually run + skips fail the build + bare-`prod` virgin-DB test ✅ — landed 2026-09-04
- TC-120 error pages (403/404/500 + catch-all, JSON stays JSON) ✅ — landed 2026-09-05
- **Next:** TC-121 (day-one admin bootstrap) → TC-115/TC-116 (CI image, AWS demo) → TC-110 → TC-111 → TC-112.

## Pointers for working in this repo

- The interesting business logic lives in **services**, not controllers. Start there when answering "how does X get computed".
- The interesting domain coupling lives in **`ServiceHour`** — it's the join table in spirit. Almost every report iterates `ServiceHour.list()`.
- **GSP partials** (`_foo.gsp`) are usually loaded by AJAX. Search the corresponding controller for `render view:"_foo"` to find the action that serves them.
- **`@Secured` annotations** at the top of each controller are the source of truth for which role can hit which endpoint — check them before assuming behavior.
- Use [BootStrap.groovy](sstation/grails-app/conf/BootStrap.groovy) as the de-facto schema documentation: it shows every domain object being instantiated with realistic fields.
