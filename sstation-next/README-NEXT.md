# sstation-next — rewrite in progress ⚠️

This directory is the **Summer 2026 rewrite** of the Austin College Service Station Hours app,
moving off the end-of-life Grails 2.4.4 stack (`../sstation/`) to a modern, supported one.

> **Status: feature-complete, not yet deployed.** Every user-facing flow from the Grails app is
> ported, plus eight features it never had (TC-108) and container packaging (TC-113/TC-114). The
> Grails app in `../sstation/` remains the nominally shippable one until the E2E suite and
> stakeholder sign-off land (TC-111) and it is decommissioned (TC-112). Before any **public** URL,
> resolve Highcharts licensing (🏫 TC-119).

## Target stack (Lane 7 — pending AC IT confirmation, TC-100)

- **Java 21 LTS** + **Spring Boot 3.3** + **Spring Security 6**
- **Spring Data JPA** / **Hibernate 6**, **Flyway** migrations
- **Thymeleaf** server-rendered templates (1:1 port from GSP — no SPA)
- **PostgreSQL** in prod; **H2** (PostgreSQL-compatibility mode) for local dev/test
- **Gradle (Kotlin DSL)** build, single executable JAR
- **JUnit 5 + Spring Test + Testcontainers** for tests; **Spotless** (Google Java Format) for style

Package root: `edu.austincollege.sstation`.

## Prerequisites

- **JDK 21** (e.g. `sdk install java 21.0.5-tem`). The Gradle wrapper handles Gradle itself.

## Common commands (run from `sstation-next/`)

| Command | What it does |
|---------|--------------|
| `./gradlew bootRun` | Start the dev server on <http://localhost:8080> (H2 in-memory). |
| `docker compose -f docker-compose.dev.yml up --build` | Same seeded logins, no JDK — see [../DEPLOY.md](../DEPLOY.md). |
| `./gradlew check` | Run Spotless format check + all tests. CI gate (TC-102). |
| `./gradlew spotlessApply` | Auto-format the code to Google Java Format. |
| `./gradlew bootJar` | Build the executable JAR under `build/libs/`. |
| `./gradlew test` | Run the JUnit 5 tests only. |

## Configuration

- Local dev uses in-memory H2 — no database to install.
- The `prod` profile uses PostgreSQL via env vars: `SSTATION_DB_URL`, `SSTATION_DB_USER`,
  `SSTATION_DB_PASSWORD`. **Never commit credentials.**

## Lane 7 progress

- [x] TC-100 — stack memo to AC IT (`../docs/TC-100-stack-memo.md`)
- [x] TC-101 — this scaffold
- [x] TC-102 — parallel CI (`ci-next.yml`)
- [x] TC-103 — JPA domain model + Flyway `V1` (real `User→Student` FK; nullable `ServiceHour` org FKs)
- [x] TC-104 — Spring Security 6 auth (form login, BCrypt, `@PreAuthorize`, CSRF, dev-only env-var seed)
- [x] TC-105 — read-only views: admin dashboard, six reports, student dashboard + per-student report
- [x] TC-106 — CRUD + quick approve/reject REST + moderator promote/demote + audit trail
- [x] TC-107 — frontend: shared layout + Bootstrap 5, vendored WebJars (Bootstrap/jQuery/DataTables/Highcharts, no CDN)
- [x] TC-108 — all eight Lane 4 features (a–g): email notifications, bulk approve/reject, CSV export, PDF export, date-range filter, event sign-up, password reset
- [x] TC-113 / TC-114 — Dockerfile + compose + `demo` profile seeder (see [../DEPLOY.md](../DEPLOY.md))
- [ ] TC-118 — the Testcontainers Postgres test skips silently on Docker 29.x (CI runs it; local `check` does not)
- [ ] TC-119 — 🏫 Highcharts licensing, before any public URL
- [ ] TC-115 / TC-116 — CI image build; AWS demo on EC2 + RDS
- [ ] TC-110 / TC-111 / TC-112 — DEPLOY.md for AWS, E2E suite + sign-off, decommission Grails

See [../demo.md](../demo.md) for a full local run + click-through walkthrough.
