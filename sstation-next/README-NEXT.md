# sstation-next — rewrite in progress ⚠️

This directory is the **Summer 2026 rewrite** of the Austin College Service Station Hours app,
moving off the end-of-life Grails 2.4.4 stack (`../sstation/`) to a modern, supported one.

> **Status: incomplete.** This is not yet a working replacement for the Grails app. The Grails app
> in `../sstation/` remains the shippable application until the rewrite reaches parity (TC-111) and
> is decommissioned (TC-112). Do not deploy this module to production yet.

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
- [ ] TC-102 — parallel CI (`ci-next.yml`)
- [ ] TC-103 — JPA domain model + Flyway `V1`
- [ ] TC-104 — Spring Security 6 auth
- [ ] TC-105 — read-only views (dashboards, reports)
- [ ] TC-106 — CRUD
- [ ] TC-107 — frontend (Thymeleaf + vendored assets)
- [ ] TC-108 → TC-112 — features, deploy, parity, decommission
