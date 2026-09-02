# Demo — running the Service Station Hours app locally

This repo has **two** apps. This guide focuses on the **Spring Boot rewrite** in
[sstation-next/](sstation-next/) (the actively developed one). The legacy Grails app in
[sstation/](sstation/) is covered at the bottom.

---

## A. The rewrite (`sstation-next/`) — Spring Boot 3 / Java 21 ⭐

As of **2026-09-02** this is **feature-complete** and runs end-to-end: the read flows (login, admin
dashboard, six reports, student dashboard/report), the write paths (TC-106: CRUD for
students/hours/events/campus orgs/community agencies, quick approve/reject, moderator
promote/demote, per-hour audit trail), a styled UI (TC-107: Bootstrap 5, role-aware navbar,
**vendored assets** at `/webjars/**`, no CDN), **all eight Lane 4 features** (TC-108: email
notifications, bulk approve/reject, CSV export, PDF export, date-range filter, event sign-up,
password reset), and **Docker packaging** (TC-113/TC-114).

What's left is deployment and handoff — plus one open item: AC IT confirming **Highcharts
licensing** (🏫 TC-119).

### 1. Prerequisites

Pick one path:

- **JDK 21** (for `gradlew bootRun`) — easiest via [SDKMAN](https://sdkman.io/):
  ```bash
  sdk install java 21.0.5-tem
  sdk use java 21.0.5-tem      # for this shell; `sdk default` to make it permanent
  java -version                # should report 21.x
  ```
- **Docker only** (no JDK) — see [DEPLOY.md](DEPLOY.md): from `sstation-next/`, run `docker compose -f docker-compose.dev.yml up --build`.

- **No Gradle install needed** (JDK path) — the project ships a Gradle wrapper (`./gradlew`).
- **No database to install** (JDK path) — dev mode uses an in-memory H2 database (PostgreSQL-compat mode).

### 2. Run it

```bash
cd sstation-next
./gradlew bootRun
```

`bootRun` auto-activates the **`dev` profile**, which:
- creates the schema via Flyway (`V1__initial_schema.sql`),
- seeds the three accounts and demo data (`DevDataSeeder` + `DemoDataSeeder`).

When you see `Started SstationNextApplication`, open **<http://localhost:8080>**.

> **The Gradle progress bar will sit at `<====---> 80% EXECUTING ... > :bootRun` and never finish —
> that is normal.** `bootRun` is a long-running task; it stays at ~80% for the whole time the server
> is up. The app is ready the moment you see `Started SstationNextApplication` / `Tomcat started on
> port 8080`. It is *not* stuck.
>
> First run downloads Gradle + dependencies and may take a couple of minutes. Stop the server with
> `Ctrl+C`. Each restart is a **fresh** in-memory DB (data does not persist).

### 3. Seeded logins (dev profile)

| Username    | Password           | Role             | Lands on |
|-------------|--------------------|------------------|----------|
| `admin`     | `admin_secret`     | `ROLE_ADMIN`     | `/admin` (KPI dashboard) |
| `student`   | `student_secret`   | `ROLE_STUDENT`   | `/student` (their own hours) |
| `moderator` | `moderator_secret` | `ROLE_MODERATOR` | landing page → Reports |

The passwords come from `SSTATION_DEV_*_PASSWORD` env vars and fall back to the values above for
local convenience. Override them by exporting e.g. `SSTATION_DEV_ADMIN_PASSWORD=… ` before `bootRun`.

### 4. Click-through (what works today)

- **Sign in as `admin`** → you're redirected to **`/admin`**:
  - KPI cards: students, approved hours (all-time + this year), avg hours/student, pending count.
  - Four Highcharts: hours by year (last 5), by month (this year), by classification, by status.
  - The year axis is **dynamic** — it always ends on the current year (no hardcoded 2015/2016).
- **Admin → Reports** (`/reports`) — all six render with selectors:
  - By year (5-year KPI table + chart), by event, by community agency, by campus organization.
  - **Summary** (pick a year) and **Semester** (pick year + Fall/Janterm/Spring/Summer).
- **Sign out, sign in as `student`** → **`/student`**:
  - Sam Student's totals (approved / pending / rejected) and a table of their service hours.
  - **My report** (`/student/report`) — approved hours bucketed by semester and by campus org.
- **Admin management (TC-106):** from the dashboard's *Manage* bar — create/edit/delete students,
  service hours, events, campus orgs, community agencies (with validation). On the **Service hours**
  page, an admin can **Approve/Reject** a row inline (writes an audit entry) and view its **Audit**
  trail; **Moderators** can be promoted/demoted (admin-only).
- **Role gating** is real: a student visiting `/admin` or `/reports` gets **403**; a moderator can
  reach `/reports` and entity CRUD but not status changes or moderator management.

#### The TC-108 features (added 2026-06-03)

- **Bulk approve/reject** — on the Service hours page, tick several pending rows and approve or
  reject the lot in one POST. Each row still gets its own audit entry.
- **CSV export** — every one of the six reports has a download link, as does the student's own
  report. Escaping is RFC-4180, so an agency named `Habitat, Inc. "North"` won't corrupt the file.
- **PDF export** — from **`/student/report`**, *Download PDF* renders the per-semester report
  through a dedicated print template. This is the artifact a student attaches to an application.
- **Date-range filter** — the admin dashboard takes a from/to range and recomputes every KPI and
  chart against it, instead of only ever showing "this year."
- **Event sign-up** — as `student`, visit **`/student/events`** to sign up for an upcoming event.
  As an admin, open an event's **Roster** to see who signed up.
- **Password reset** — *Forgot password?* on the login page. Tokens are single-use, hashed with
  SHA-256 before storage, and expire in an hour. The confirmation message is identical whether or
  not the account exists, so it can't be used to enumerate users.
- **Email notifications** — approve or reject an hour and the student is emailed. ⚠️ **In local dev
  no mail is actually sent**: `spring.mail.host` is empty, so `JavaMailSender` is never configured
  and `NotificationService` writes the message to the log instead. Watch the console. Nothing is
  broken; there's just no SMTP relay until AC IT supplies one.

### 5. Run the tests / format

```bash
cd sstation-next
./gradlew check          # Spotless format check + all JUnit 5 tests (the CI gate)
./gradlew spotlessApply  # auto-format to Google Java Format
./gradlew bootJar        # build an executable JAR under build/libs/
```

### 6. Production mode (for reference)

Prod uses **PostgreSQL** and **never seeds**. Supply credentials via env vars and activate the
`prod` profile:

```bash
SSTATION_DB_URL=jdbc:postgresql://HOST:5432/sstation \
SSTATION_DB_USER=sstation \
SSTATION_DB_PASSWORD=… \
java -jar build/libs/sstation-next-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Troubleshooting

| Symptom | Fix |
|---------|-----|
| `bootRun` fails with an old-Java error | You're not on JDK 21. `sdk use java 21.0.5-tem`, re-check `java -version`. |
| Port 8080 already in use | Stop the other process, or run `./gradlew bootRun --args='--server.port=8081'`. |
| Login page loops / 403 on POST | CSRF is on; use the real login form (the seeded creds above), not a raw POST. |
| Dashboard is empty | You're not on the `dev` profile. `bootRun` sets it automatically; a plain `java -jar` does not. |
| Approve/reject sends no email | Expected in dev — `spring.mail.host` is empty, so notifications are written to the log instead of sent. Set `SSTATION_MAIL_HOST` to use a real relay. |
| `docker compose` app exits complaining about a password | The `demo` profile has **no** password fallback by design. Set `SSTATION_DEMO_ADMIN_PASSWORD` / `_STUDENT_` / `_MODERATOR_` (copy `.env.example` → `.env`). |
| `./gradlew check` green but Postgres test showed `SKIPPED` | Known: Testcontainers can't reach Docker 29.x, so the one real-Postgres test opts out silently. CI runs it properly. See TC-118. |
| Gradle stuck at `80% EXECUTING > :bootRun` | Not stuck — that's how a running server looks. The app is up once you see `Started SstationNextApplication`. `Ctrl+C` to stop. |

---

## B. The legacy Grails app (`sstation/`)

The original Grails 2.4.4 app. **Needs JDK 7/8 and Grails 2.4.4** (it will *not* run on JDK 11+).

```bash
sdk install java 8.0.452-tem   # JDK 8
sdk install grails 2.4.4
cd sstation
grails run-app                 # http://localhost:8080/sstation
```

Same three seeded logins as above. Full details and gotchas (the dead Grails wrapper, the
Spring-Loaded test issue, etc.) are in [CLAUDE.md](CLAUDE.md). This app remains the shippable one
until the rewrite reaches parity (TC-111) and it is decommissioned (TC-112).
