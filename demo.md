# Demo — running the Service Station Hours app locally

This repo has **two** apps. This guide focuses on the **Spring Boot rewrite** in
[sstation-next/](sstation-next/) (the actively developed one). The legacy Grails app in
[sstation/](sstation/) is covered at the bottom.

---

## A. The rewrite (`sstation-next/`) — Spring Boot 3 / Java 21 ⭐

As of **2026-06-03** this runs end-to-end for all **read-only** flows: login, the admin dashboard,
the six reports, and the student dashboard/report. Write paths (CRUD) are TC-106, not done yet.

### 1. Prerequisites

- **JDK 21** — the only thing you must install. Easiest via [SDKMAN](https://sdkman.io/):
  ```bash
  sdk install java 21.0.5-tem
  sdk use java 21.0.5-tem      # for this shell; `sdk default` to make it permanent
  java -version                # should report 21.x
  ```
- **No Gradle install needed** — the project ships a Gradle wrapper (`./gradlew`).
- **No database to install** — dev mode uses an in-memory H2 database (PostgreSQL-compat mode).

### 2. Run it

```bash
cd sstation-next
./gradlew bootRun
```

`bootRun` auto-activates the **`dev` profile**, which:
- creates the schema via Flyway (`V1__initial_schema.sql`),
- seeds the three accounts and demo data (`DevDataSeeder` + `DemoDataSeeder`).

When you see `Started SstationNextApplication`, open **<http://localhost:8080>**.

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
- **Role gating** is real: a student visiting `/admin` or `/reports` gets **403**; a moderator can
  reach `/reports`.

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
