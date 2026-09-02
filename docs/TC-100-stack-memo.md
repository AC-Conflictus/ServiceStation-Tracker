# Memo: Target Stack for the Service Station Hours App Rewrite (TC-100)

**To:** Austin College IT 🏫 *(addressee TBD — replace with the named contact)*
**From:** Service Station Hours App project team
**Date:** 2026-06-02
**Re:** Recommended technology stack for the Summer 2026 rewrite — request for confirmation or counter-proposal

---

## TL;DR — the ask

We are rewriting the Service Station volunteer-hours web app this summer before handing
it to you for long-term ownership. Before we write a line of the new code, we want your
sign-off on the **target stack** and your **deploy preferences**, because they shape
everything that follows.

**Our recommendation:** **Java 21 (LTS) + Spring Boot 3.3 + Spring Security 6 +
Spring Data JPA / Hibernate 6 + Thymeleaf + PostgreSQL + Gradle**, packaged as a single
executable JAR (with a Docker image option).

Please reply with **one of**:
1. "Approved — build on that stack," or
2. A counter-proposal naming your house standard (e.g. .NET, Django/Python, a specific
   Java/Spring version, a required app server).

We'll record your answer in `TRELLO_CARDS.md` and proceed accordingly. **This decision
blocks all other rewrite work (Lane 7, cards TC-101 → TC-112).**

---

## Why we're rewriting (context)

The current app is **Grails 2.4.4** (Groovy on Grails), built ~2015–2016 as a class
project. The stack is end-of-life on every axis:

| Component | Version in use | Status |
|-----------|----------------|--------|
| Grails | 2.4.4 | Unsupported; no security patches |
| Spring Security plugin | 2.0-RC5 | A *release candidate*, never GA |
| Hibernate | 4.3 | EOL |
| JDK | 7/8-era (`source.level = 1.6`) | Won't run on modern JVMs |
| jQuery / Bootstrap | 1.11 / 3.3 | Out of support |
| H2 | 2015-era | Known CVEs |

It cannot be safely maintained or extended as-is. We have ~12 weeks of runway, which is
enough for a tightly-scoped rewrite that **preserves the existing UX** (no redesign) while
moving to a supported, hireable, patchable platform.

## What the recommended stack buys you

- **Java + Spring Boot is the de-facto enterprise-Java standard.** Documentation, tooling,
  and people who know it are abundant — easier for you to hire for and maintain long-term
  than Grails/Groovy, which has lost mindshare.
- **Java 21 is LTS** with support into the early 2030s. Spring Boot 3.3 + Spring Security 6
  are current and actively patched.
- **Thymeleaf is server-rendered, like the current GSP templates.** The port is mechanical
  (template-for-template), not a frontend rewrite. No SPA, no Node/npm in your deploy story.
- **Single executable JAR (or Docker image)** is simpler to deploy than the current
  WAR-into-Tomcat dance. Runs anywhere a JRE or container runs.
- **PostgreSQL** is open-source, ubiquitous, and what we'd target for the AWS demo anyway.
- **Flyway migrations** give you a versioned, auditable schema history.

## What we're trading off / what you should weigh in on

- **It's still Java.** If your team's house standard is .NET or Python/Django, say so now —
  the *sequence* of rewrite work is identical; only the destination language/framework
  changes. Changing it after TC-101 is expensive.
- **Thymeleaf, not a modern JS framework.** Deliberate: it keeps the deploy story simple and
  matches the current server-rendered model. If you specifically want a React/Angular SPA,
  that's a larger effort and a different conversation.
- **Highcharts** (the dashboard charting library) is **non-free for commercial/government
  use.** The current app uses it. We need to know whether AC holds a Highcharts license or
  whether we should swap to a freely-licensed alternative (e.g. Chart.js or ApexCharts)
  during the frontend port (TC-107). **Please advise.**
- **We are not redesigning the UX.** Same pages, same flows. If you want UX changes, those
  are post-handoff enhancements, not part of this rewrite.

## Questions we need answered to proceed

1. **Stack approval** — recommended stack as above, or a counter-proposal?
2. **Deploy target** — Tomcat? Bare executable JAR on a VM? Docker? Kubernetes? AWS
   (App Runner / ECS / Beanstalk)? On-prem vs. cloud?
3. **Operating system** of the production host (Linux distro? Windows Server?).
4. **Existing Java version** on your prod box, if any (so we target a compatible JRE).
5. **Database** — is PostgreSQL acceptable, or do you have a standard (MS SQL, MySQL,
   Oracle)?
6. **SMTP relay** — host/port we should target for email notifications, or do you run an
   internal relay?
7. **Identity** — do you run an SSO IdP (SAML / OIDC, e.g. Azure AD / Shibboleth)? If so we
   can leave a config seam for it (TC-104) instead of app-local accounts.
8. ~~**Highcharts licensing**~~ — **withdrawn 2026-09-02.** We resolved this ourselves rather than wait:
   the app now uses Chart.js (MIT), so there is no license for you to check or buy. No answer needed.

---

*Reply by email; we will transcribe your decision into the "Working assumption" block of
`TRELLO_CARDS.md` (Lane 7) and into `CLAUDE.md`, then begin TC-101.*
