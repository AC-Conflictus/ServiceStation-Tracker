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

Login redirects through Spring Security's auth controller. The `student` user only works if a matching `AcStudent` row exists whose `acEmail` is `student@austincollege.edu` — see the "smelly code" note in [HomeController.groovy:41](sstation/grails-app/controllers/sstation/HomeController.groovy#L41). With the current random seed, the student dashboard will likely fail to resolve the student record; admin/moderator land correctly.

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

- **`AcUser` / `AcRole` / `AcUserAcRole`** — Spring Security auth tables. Username + bcrypt-encoded password (with a plaintext fallback in [AcUser.groovy:52](sstation/grails-app/domain/sstation/AcUser.groovy#L52) if `springSecurityService` is null — a small risk).
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

- **`HourService.init()` hardcodes `currentYear = 2015`** ([HourService.groovy:27](sstation/grails-app/services/sstation/HourService.groovy#L27)). All "current year" KPIs are frozen there.
- **`ReportsController.summaryReport` hardcodes `year = 2016`** ([ReportsController.groovy:50](sstation/grails-app/controllers/sstation/ReportsController.groovy#L50)).
- **`ReportsController.summaryReport` / `semesterReport` assume `allAgs/allOrgs/allEvs` each have ≥ `constant` (5) entries** — `allAgs.get(i)` will throw IndexOutOfBounds if seed data shrinks.
- **`semesterReport` dereferences `s.commAg.name`, `s.event.name`, `s.campusOrg.name`** unconditionally even though all three are declared `nullable:true` in `ServiceHour` constraints. NPE risk with real data.
- **`BootStrap.init` runs in every environment**, including production. It will try to recreate roles/users on every boot.
- **`AcUser.encodePassword` silently falls back to plaintext** when `springSecurityService` is unavailable.
- **Mail plugin credentials in [Config.groovy:62](sstation/grails-app/conf/Config.groovy#L62) are blank** — email actions will fail until populated.

## What the app already does (today)

Working flows you can demo against the seed data:

- Three-role authentication (admin / moderator / student) via Spring Security.
- Admin dashboard with KPIs, charts (by classification, by status, by year), and a pending-hours queue with inline approve/reject.
- CRUD for: students, service hours, events, community agencies, campus orgs.
- Promote/demote students to moderator.
- Quick status updates via AJAX dialog (`ajaxUpdateStatus`).
- Reports: summary, by year (5-year window), by semester (Fall/Janterm/Spring/Summer), per event, per community agency, per campus org — rendered as Highcharts.
- Per-student report view with their own hours.
- Plugin scaffolding for CSV upload (`StudentService`) and email (`mail` plugin).
- Spock unit-test specs for most domain classes and several controllers under [sstation/test/unit/sstation/](sstation/test/unit/sstation/).
- Selenium IDE regression scripts checked in under [selinium_tests/](selinium_tests/).

## What is missing / broken / "TODO"

Concrete gaps you can confirm by reading the code:

- **Hardcoded years.** `HourService` (2015) and `ReportsController.summaryReport` (2016) — the dashboard and summary will look broken on any fresh run.
- **Student login is fragile.** `HomeController` matches student email by concatenating `username + "@austincollege.edu"`. There is no `AcUser → AcStudent` FK; the comment in [HomeController.groovy:41](sstation/grails-app/controllers/sstation/HomeController.groovy#L41) explicitly calls this out as a TODO.
- **No `AcStudent` is created for the seeded `student` user**, so logging in as `student` likely lands on an empty/error page.
- **README workflow section is empty**, and the README description of `CampusOrg` is truncated (`A CampusOrg class includes`).
- **`BootStrap.init` runs in production** — it should be gated by `Environment.current`.
- **`AcUser` plaintext password fallback** ([AcUser.groovy:52](sstation/grails-app/domain/sstation/AcUser.groovy#L52)).
- **Mail credentials missing** in [Config.groovy](sstation/grails-app/conf/Config.groovy) — any feature that sends mail (e.g. approval notifications) is non-functional.
- **NPE-prone report iteration** when `ServiceHour.commAg/event/campusOrg` is null (allowed by constraints).
- **`IndexOutOfBoundsException` risk** in `summaryReport`/`semesterReport` when there are fewer than 5 agencies/orgs/events.
- **`selinium_tests/` is misspelled** and not wired into CI.
- **No CI/build pipeline files** (no `.github/`, no `azure-pipelines.yml`, no `Jenkinsfile`).
- **Stack is end-of-life.** Grails 2.4.4 is unsupported; Spring Security plugin 2.0‑RC5 is a release candidate; jQuery 1.11 / Bootstrap 3 are out of support; H2 versions in this era have known CVEs.

## Suggested Trello cards / features to add

These are *suggestions only* derived from reading the code — confirm with the user before working on any.

**Stability / bug-fix cards**
1. Replace hardcoded years in `HourService.init` and `ReportsController.summaryReport` with `Calendar.getInstance().get(Calendar.YEAR)`.
2. Add a real FK between `AcUser` and `AcStudent` (e.g. `AcStudent acStudent` on `AcUser`) and remove the email-suffix hack in `HomeController`.
3. Guard `BootStrap.init` with `if (Environment.current == Environment.DEVELOPMENT)`; provide a separate prod seeder for roles only.
4. Null-safe access to `commAg/event/campusOrg.name` in `ReportsController.semesterReport` / `summaryReport`.
5. Defensive bounds-checking around `constant = 5` loops in `ReportsController`.
6. Wire `student@austincollege.edu` seed `AcStudent` so the `student` test login actually lands somewhere useful.
7. Remove the plaintext password fallback in `AcUser.encodePassword`.

**Feature cards**
8. CSV export for each report page (the `csv` plugin is already in `BuildConfig.groovy`).
9. PDF export of a student's per-semester report (for actual paper-form replacement).
10. Email notifications on hour approval/rejection (fill in `Config.groovy` mail creds + wire `mailService` calls).
11. Bulk approve/reject from the pending queue.
12. Student self-service password reset.
13. Service-event sign-up flow (the README promises "recruit students to participate" but no controller does this today).
14. Date-range filter on the dashboard instead of "current year" only.
15. Audit log of who approved/rejected which `ServiceHour`.

**Infra / modernization cards**
16. Fill in the README "Workflow" section and finish the truncated `CampusOrg` description.
17. Set up GitHub Actions to run `grails test-app` on push (requires pinning JDK 8).
18. Replace the dead Spring S3 wrapper URL in [wrapper/grails-wrapper.properties](sstation/wrapper/grails-wrapper.properties) or remove the wrapper entirely and document SDKMAN.
19. Migrate H2 to PostgreSQL for prod (add dependency, swap `DataSource.groovy` prod block).
20. Rename `selinium_tests/` → `selenium_tests/` and either modernize to Selenium WebDriver or delete.
21. Long-term: port to Grails 5/6 (or Spring Boot + a modern frontend) — non-trivial; Spring Security plugin API and GSP both changed significantly.

## Pointers for working in this repo

- The interesting business logic lives in **services**, not controllers. Start there when answering "how does X get computed".
- The interesting domain coupling lives in **`ServiceHour`** — it's the join table in spirit. Almost every report iterates `ServiceHour.list()`.
- **GSP partials** (`_foo.gsp`) are usually loaded by AJAX. Search the corresponding controller for `render view:"_foo"` to find the action that serves them.
- **`@Secured` annotations** at the top of each controller are the source of truth for which role can hit which endpoint — check them before assuming behavior.
- Use [BootStrap.groovy](sstation/grails-app/conf/BootStrap.groovy) as the de-facto schema documentation: it shows every domain object being instantiated with realistic fields.
