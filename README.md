# Austin College Service Station Hours Registration Web Application

[![CI](../../actions/workflows/ci.yml/badge.svg)](../../actions/workflows/ci.yml)
[![CI (next)](../../actions/workflows/ci-next.yml/badge.svg)](../../actions/workflows/ci-next.yml)

> **Note:** The `CI` badge tracks the legacy Grails 2.4.4 app under [sstation/](sstation/) (build-only WAR pipeline). The `CI (next)` badge tracks the Spring Boot 3 / Java 21 rewrite under [sstation-next/](sstation-next/) (Lane 7 — `./gradlew check`). See [TRELLO_CARDS.md](TRELLO_CARDS.md) Lane 7.

> **This repo contains two apps:**
> - [sstation/](sstation/) — the original **Grails 2.4.4** app (currently shippable; needs JDK 7/8).
> - [sstation-next/](sstation-next/) — the **Spring Boot 3 / Java 21** rewrite (Lane 7), now **feature-complete**: domain model, auth, all views, full CRUD, a Bootstrap 5 frontend, all eight Lane 4 features (email, bulk approve, CSV/PDF export, date filter, event sign-up, password reset), and Docker packaging. What's left is deployment and handoff, not features.
>
> **To run either locally, see [demo.md](demo.md).** Docker and self-hosting: [DEPLOY.md](DEPLOY.md). Guidance for contributors/AI assistants is in [CLAUDE.md](CLAUDE.md).
>
> **On hosting:** this project is handed to Austin College IT to run on **their own servers** — see the self-hosting runbook in [DEPLOY.md](DEPLOY.md). Any public demo link is a showcase with throwaway data, not the service the college would use.

### Backgrounds
[Austin College Service Station](https://www.austincollege.edu/campus-life/service-station/) is a student-run office which organizes volunteer and community service events. Every year, Service Station organizes huge volunteer events focusing on diverse purposes, such as *Great Day of Service*, *GreenServe*, and *JanServe* and corporates with student service organizations and local non-profit agencys to hold these events. 

Many students participte in these events and use paper forms to record the number of hours participated in volunteer activities. These service hour records are helpful for students' future application to non-profit organizations or social work related positions. Nonetheless, keeping and organizing a large stack of paper forms is not easy. We aim to design a platform in order to help students keep track of their service hours and replace the tedious and error-prone paperwork. The platform has the following specific goals:

For the Service Station office: 
- post service events and recruit students to participate
- approve students' pariticpation and service hour records
- get useful statistics about student participation in service events to improve future events

For students: 
- keep a digital record of all pariticipated service events and service hours and generate a summary report
- get useful statistics about their participation in service events and plan for future involvement in service activities

### Structure of the Web App
#### Tech Stack
The original app used Groovy-on-Grails (Spring MVC). The rewrite keeps the server-rendered model but on a supported stack:

| | Original ([sstation/](sstation/)) | Rewrite ([sstation-next/](sstation-next/)) |
|---|---|---|
| Language / runtime | Groovy, JDK 7/8 | **Java 21 LTS** |
| Framework | Grails 2.4.4 | **Spring Boot 3.3** |
| Security | Spring Security plugin 2.0-RC5 | **Spring Security 6** (BCrypt, CSRF on) |
| Persistence | GORM / Hibernate 4 | **Spring Data JPA / Hibernate 6**, Flyway migrations |
| Database | H2 | **PostgreSQL** (H2 in PG-compat mode for dev) |
| Views | GSP | **Thymeleaf** |
| Front-end | jQuery 1.11, Bootstrap 3, CDN-loaded | **Bootstrap 5, jQuery 3.7**, vendored via WebJars |
| Build | Grails CLI | **Gradle** (Kotlin DSL) wrapper |
| Tests | Spock unit specs, Selenium IDE | **JUnit 5 + Spring Test + Testcontainers** |
| Packaging | WAR into Tomcat | **Executable JAR / Docker image** |

#### Users 
There are three types of users:
- Super Users: Service Station supervisor who could create and update service events and non-profit organizations and approve students service hours records.
- Admin Users: Service Station student workers. Admin users could approve or reject service hour records submitted by students.
- Students: Students could create, modify or delete service hour records for themselves and view report about their service hours.

#### Service Hour Related Classes
- Service Hour Records: An *ServiceHour* class documents a student's ID and name, the number of hours participated in a particular service event and some additional information about this event. After a service event, student users will create their hour records and admin users will review and approve these records. 

- Service Events: A *Event* class includes related information of a service event, such as event name, pariticpated non-profit organizations, and event date. Super users will create and update these events.

- Campus Organizations: A *CampusOrg* class includes information about an Austin College student organization that runs or co-sponsors service events — THINK, BIG, APO and the like. Service hour records reference the campus organization a student served with, which is what drives the per-organization reports. Super users create and update these.

- Non-profit Agencys: An *CommAg* class includes related information about a local non-profit agency, such as agency name, agency mission, and agency contact. Super users will create and update these organizations.

### Workflow

The core loop the app replaces is the paper hour-form:

1. **Service Station posts an event.** A super user creates an *Event*, linking the *CommAg* (non-profit) it serves and the *CampusOrg* co-sponsoring it.
2. **Students sign up.** A student browses upcoming events and signs up; staff can view the roster for an event.
3. **The event happens.**
4. **The student logs their hours.** They create a *ServiceHour* record against that event — hours served, date, and which organization/agency it was for. It starts as `PENDING`.
5. **Staff review the queue.** Admins and moderators work the pending queue and approve or reject each record, individually or in bulk. Every status change is written to an audit log, and the student is emailed the outcome.
6. **Everyone gets their numbers.** The student sees running totals and a per-semester report they can export as PDF for an application. The office gets dashboards and six reports (by year, semester, event, community agency, campus org, plus a summary), each exportable as CSV, to see which events and partners actually drew participation.

Password resets are self-service; promoting a student to moderator is an admin action.

> Steps 1–6 describe [sstation-next/](sstation-next/). The legacy Grails app supports the core loop (events, hour logging, approve/reject, reports) but **not** event sign-up, bulk approval, CSV/PDF export, email notification, or password reset — those were built only in the rewrite (TC-108).


