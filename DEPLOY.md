# Deploy — Service Station (`sstation-next`)

> **Who this is for.** Austin College IT runs this application on **their own servers**. This
> document is the runbook for doing that — start at [Docker quick start](#docker-quick-start-tc-113)
> and [First login](#first-login-on-a-new-production-database-tc-121).
>
> Nobody on the project operates a service that the college then uses. Any public demo link
> (see [Public demo](#public-demo--vercel-tc-122)) exists only so the Service Station office and
> AC IT can click through a working copy before deciding to host it. It holds throwaway seed data,
> is expected to be slow on first load, and is not something to depend on.
>
> **A single container is a valid deployment.** For an office this size, one instance of the app
> is the expected shape — one host, one app process (container or `java -jar`), one Postgres. The
> app keeps its HTTP sessions in memory, which is fine for that; there is nothing to cluster and no
> shared session store to stand up. The horizontal-scaling work mentioned in
> [TC-122](#public-demo--vercel-tc-122) (`spring-session-jdbc`) is a constraint of the Vercel
> showcase, **not** something AC IT's deployment needs.

Spring Boot 3 / Java 21 rewrite. For local JDK development see [demo.md](demo.md).

## Prerequisites

What the host running the app needs:

- **Either Docker** (Engine + Compose v2, or Docker Desktop) — the container path needs no JDK on
  the host at all.
- **Or JDK 21** — only for the no-Docker path ([Build without Docker](#build-without-docker)):
  JDK 21 once to build the JAR with the Gradle wrapper, then a JRE 21 to run it.
- **PostgreSQL 13+** 🏫 — the app's only datastore. The compose stack can run it for you (Postgres
  16, see below); otherwise AC IT supplies the server, database name, user and password via the
  `SSTATION_DB_*` variables.
- **An SMTP relay (optional)** 🏫 — the app runs fine with none configured: approve/reject
  notifications and password-reset mail are written to the log instead of sent. Add a relay
  (host, port, credentials) when real mail is wanted.
- **A reverse proxy terminating TLS (optional, recommended)** 🏫 — see
  [Reverse proxy and TLS termination](#reverse-proxy-and-tls-termination). A plain-HTTP host on a
  network you control also works, but anything internet-facing should sit behind your proxy.

Three ways to run it — pick whatever matches AC IT's existing setup; each links to its section:

- **Docker on a VM** — [Docker quick start](#docker-quick-start-tc-113), ideally pulling the
  pre-built image ([Pull a pre-built image](#pull-a-pre-built-image-tc-115)) rather than compiling.
- **A plain VM without Docker** — [Build without Docker](#build-without-docker) (`java -jar`)
  against AC IT's own Postgres.
- **Behind an existing reverse proxy** — any of the above, with TLS terminated at the
  nginx/Apache/IIS and the standard `X-Forwarded-*` headers set
  ([Reverse proxy and TLS termination](#reverse-proxy-and-tls-termination)).

## Docker quick start (TC-113)

**Prerequisites:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) (or Docker Engine + Compose v2).

### Option A — Seeded demo (H2, same logins as `gradlew bootRun`)

```bash
cd sstation-next
docker compose -f docker-compose.dev.yml up --build
```

Open **http://localhost:8080** — `admin` / `admin_secret`, `student` / `student_secret`, `moderator` / `moderator_secret`.

### Option B — Production-shaped stack (Postgres + demo seeders, TC-114)

```bash
cd sstation-next
copy .env.example .env    # Windows; use cp on Linux/Mac
docker compose up --build
```

- Postgres 16 + `SPRING_PROFILES_ACTIVE=prod,demo`.
- Default demo logins (override in `.env` before any public deploy):
  - `admin` / `changeme-demo-admin`
  - `student` / `changeme-demo-student`
  - `moderator` / `changeme-demo-moderator`
- Health: **http://localhost:8080/actuator/health**

**Any public host / internet:** set strong `SSTATION_DEMO_*_PASSWORD` values — the Java app has **no** `admin_secret` fallback when `demo` is active.

Optional: adjust `POSTGRES_PASSWORD` in `.env` as well.

Stop: `Ctrl+C`, then `docker compose down` (add `-v` to drop the Postgres volume).

## Build the image manually

```bash
cd sstation-next
docker build -t sstation-next:local .
docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev sstation-next:local
```

## Pull a pre-built image (TC-115)

CI builds the image on every change under `sstation-next/`, runs it against Postgres, smoke-tests
the running container, and publishes it from `main`. On a small host (1 GiB of RAM is plenty to
*run* the app but not to compile it) pull it rather than compiling a Spring Boot app locally:

```bash
docker pull ghcr.io/ac-conflictus/sstation-next:latest
# or pin a commit
docker pull ghcr.io/ac-conflictus/sstation-next:<git-sha>
```

To run that image with the compose stack instead of building, override the `app` service's image —
`docker-compose.ci.yml` is a working example of exactly that override.

The published image is only ever one that passed the smoke test: the publish step runs after it.

## Environment variables

| Variable | Used when | Purpose |
|----------|-----------|---------|
| `SPRING_PROFILES_ACTIVE` | Always | `dev` (H2 + dev seed), `prod` (Postgres), `prod,demo` (Postgres + showcase seed, TC-114) |
| `SSTATION_DEMO_*_PASSWORD` | `demo` | **Required** when `demo` profile is on (no defaults in code) |
| `SSTATION_DB_URL` | `prod` | JDBC URL, e.g. `jdbc:postgresql://host:5432/sstation` 🏫 |
| `SSTATION_DB_USER` | `prod` | DB user 🏫 |
| `SSTATION_DB_PASSWORD` | `prod` | DB password 🏫 |
| `SSTATION_MAIL_HOST` | `prod` | SMTP host; leave empty for log-only notifications 🏫 |
| `SSTATION_MAIL_PORT` | `prod` | Default `587` |
| `SSTATION_MAIL_USER` / `SSTATION_MAIL_PASSWORD` | `prod` | SMTP credentials 🏫 |
| `SSTATION_MAIL_HEALTH_ENABLED` | `prod` | Default `false`. Leave off until SMTP is real — the mail health check fails against an empty host and takes `/actuator/health` DOWN, which stops the container ever reporting healthy (TC-115). Set `true` once a relay is configured. |
| `SSTATION_MAIL_FROM` | All | From address; default `no-reply@austincollege.edu` |
| `SSTATION_DEV_*_PASSWORD` | `dev` | Override seeded dev passwords |
| `SSTATION_BOOTSTRAP_ADMIN_PASSWORD` | bare `prod` | **Required on a first boot against an empty database** — creates the first administrator (TC-121). Minimum 8 characters. Ignored once any account exists. |
| `SSTATION_BOOTSTRAP_ADMIN_USERNAME` | bare `prod` | Username for that account; default `admin`. In directory mode, pick one that is **not** an AC user name (e.g. `sstation-admin`) — see below |
| `SSTATION_AUTH_MODE` | All | `local` (default — accounts stored in this app; the placeholder) or `directory` (AC user names and passwords). See [Signing in with AC credentials](#signing-in-with-ac-credentials-tc-124) |
| `SSTATION_AUTH_PASSWORD_HELP_URL` | `directory` | Where "Forgot your AC password?" links to 🏫. Blank shows "Contact Austin College IT" |
| `SSTATION_DIRECTORY_TYPE` | `directory` | `active-directory` (default) or `ldap` |
| `SSTATION_DIRECTORY_URL` | `directory` | e.g. `ldaps://dc1.austincollege.edu:636` 🏫. Space-separate several for failover |
| `SSTATION_DIRECTORY_DOMAIN` | `active-directory` | UPN suffix, e.g. `austincollege.edu` 🏫 |
| `SSTATION_DIRECTORY_SEARCH_BASE` | `directory` | Where user entries live 🏫. Required for `ldap`; optional for AD |
| `SSTATION_DIRECTORY_USER_SEARCH_FILTER` | `ldap` | Default `(uid={0})`; `{0}` is the user name |
| `SSTATION_DIRECTORY_MANAGER_DN` / `_PASSWORD` | `ldap` | Optional service account to search as 🏫 |
| `SSTATION_DIRECTORY_EMAIL_DOMAIN` | `directory` | Default `austincollege.edu`. Used to find a student record when the directory entry has no `mail` |
| `SSTATION_DIRECTORY_ADMIN_GROUP` / `_MODERATOR_GROUP` | `directory` | Optional group DNs (as they appear in `memberOf`) that grant ADMIN / MODERATOR 🏫 |
| `SSTATION_SIGNIN_CAPTCHA_AFTER` | All | Default `3`. Failed sign-ins for one user name before a CAPTCHA is required (or, without Turnstile keys, before the name is blocked). See [Sign-in limits](#sign-in-limits-tc-125) |
| `SSTATION_SIGNIN_BLOCK_AFTER` | All | Default `5`. Failures before the name is blocked even with a solved CAPTCHA. 🏫 Keep it **below AC's Active Directory lockout threshold** |
| `SSTATION_SIGNIN_WINDOW` | All | Default `15m`. Failures older than this stop counting, and a block lasts this long |
| `SSTATION_TURNSTILE_SITE_KEY` / `_SECRET_KEY` | All | Optional Cloudflare Turnstile keys 🏫. Both or neither — half a pair fails at startup |

🏫 = a value **AC IT supplies**; there is no default for it anywhere. Anything without a 🏫 is
either optional or has a safe default.

The variable names match the original Grails handoff plan (TC-035) so an existing secrets manager
carries over unchanged, with two differences worth knowing:

- The mail username variable is `SSTATION_MAIL_USER` here (the Grails plan said
  `SSTATION_MAIL_USERNAME`).
- There is **no `SSTATION_SERVER_URL`**. The app derives its public URL from the request it
  receives — which is exactly why the reverse-proxy section insists on the `X-Forwarded-*` headers.

Never commit real production secrets. Use whatever secrets manager your infrastructure already has
or, at minimum, a root-only `.env` file with restrictive permissions — this runbook deliberately
prescribes neither (same stance as the original Grails handoff plan, TC-035).

## First login on a new production database (TC-121)

A bare `prod` boot **never seeds** — that is deliberate, and it means a brand-new database has
**no accounts at all**. Without the two variables below you get a sign-in form that rejects
every credential, which looks like a broken deployment rather than an empty one.

> The `demo` profile does not have this problem, because `DemoAccountSeeder` creates accounts.
> If you are running `prod,demo` for the showcase, skip this section.

**1. Set the bootstrap variables before the first start:**

```bash
SSTATION_BOOTSTRAP_ADMIN_USERNAME=sstation-admin   # optional, defaults to "admin" — not an AC user name
SSTATION_BOOTSTRAP_ADMIN_PASSWORD='<a strong one-time password>'
```

On startup you will see, at WARN level:

```
[bootstrap] created the first administrator 'sstation-admin'. This password came from an
environment variable and is single-use: you will be required to change it at first sign-in.
```

If instead you see `[bootstrap] this database has no user accounts and
SSTATION_BOOTSTRAP_ADMIN_PASSWORD is not set, so nobody can sign in.` — that is this section
telling you it was skipped.

**2. Sign in and change the password.** The account is created flagged, so it is confined to
`/change-password` until it has a password of its own. Re-entering the bootstrap password is
refused; the whole point is retiring the value that lives in your deployment config. Changing it
ends the session, so you sign in again with the new one.

**3. Remove `SSTATION_BOOTSTRAP_ADMIN_PASSWORD` from the deployment configuration.** It has done
its job. Leaving it set is not dangerous — the runner is gated on the `users` table being empty,
so it will never run again or resurrect a deleted admin — but there is no reason to keep a
credential in a compose file or unit file.

**4. Turn on AC sign-in** ([next section](#signing-in-with-ac-credentials-tc-124)). That is how
everyone else gets an account: students on their first sign-in, staff through a directory group.
In `local` mode the app has no screen for creating accounts — local accounts are for the
bootstrap admin and break-glass access, not for the student body.

### Break-glass: creating an admin by SQL

If the app is already running with accounts and you have locked yourself out, the bootstrap
runner will not help — it only acts on an empty table. Insert directly instead, with a BCrypt
hash you generate yourself:

Generate a BCrypt hash. This one-liner needs nothing but Docker, which you already have:

```bash
docker run --rm httpd:2.4-alpine htpasswd -bnBC 10 "" '<new password>' \
  | tr -d ':\n' | sed 's/^\$2y/\$2a/'
```

(If `htpasswd` is installed on the host, drop the `docker run --rm httpd:2.4-alpine` prefix. The
`sed` rewrites Apache's `$2y` prefix to the `$2a` that Spring Security expects.)

```sql
INSERT INTO users (username, password, enabled, account_expired, account_locked,
                   password_expired, must_change_password)
VALUES ('recovery-admin', '{bcrypt}<hash from above>', TRUE, FALSE, FALSE, FALSE, TRUE);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = 'recovery-admin' AND r.authority = 'ROLE_ADMIN';
```

The `{bcrypt}` prefix is required — the app uses a delegating password encoder and reads the
algorithm from it. Setting `must_change_password` to `TRUE` gives the same forced change as the
bootstrap path.

The second statement assumes `ROLE_ADMIN` already exists in `roles`, which it will on any
database the app has created an account on. On a truly empty one, insert it first with
`INSERT INTO roles (authority) VALUES ('ROLE_ADMIN');` — or just use the bootstrap variables
above, which is what they are for.

*Verified against PostgreSQL 16 on 2026-09-05: hash generated with the command above, both
statements applied, and the resulting account signed in and was sent to `/change-password`.*

## Signing in with AC credentials (TC-124)

**The login the app ships with is a placeholder.** Students and staff are meant to sign in the
way they already do at Austin College — the same user name and password as AC Self-Service — and
that is AC IT's to connect. It is configuration, not code: no rebuild, no fork.

The sign-in page already follows Self-Service's flow (one card, **User name** / **Password**, one
button). Switching to `directory` mode changes what checks the password, not what people see.

### What AC IT decides

| Question | Set |
|---|---|
| Active Directory, or another LDAP server? | `SSTATION_DIRECTORY_TYPE=active-directory` (default) or `ldap` |
| Which server? Use `ldaps://` (port 636) — the password crosses the wire. | `SSTATION_DIRECTORY_URL` 🏫 |
| AD: the UPN suffix people sign in with | `SSTATION_DIRECTORY_DOMAIN` 🏫 |
| LDAP: where users live, and how to find one | `SSTATION_DIRECTORY_SEARCH_BASE`, `SSTATION_DIRECTORY_USER_SEARCH_FILTER` 🏫 |
| LDAP: does searching need a service account? | `SSTATION_DIRECTORY_MANAGER_DN` / `_PASSWORD` 🏫 |
| Which groups are Service Station admins / moderators? (optional) | `SSTATION_DIRECTORY_ADMIN_GROUP` / `_MODERATOR_GROUP` 🏫 |
| Where should "Forgot your AC password?" go? | `SSTATION_AUTH_PASSWORD_HELP_URL` 🏫 |

Then set `SSTATION_AUTH_MODE=directory` and restart. A minimal Active Directory setup is three
lines:

```bash
SSTATION_AUTH_MODE=directory
SSTATION_DIRECTORY_URL=ldaps://dc1.austincollege.edu:636
SSTATION_DIRECTORY_DOMAIN=austincollege.edu
```

Active Directory needs **no service account**: the app binds as `user@domain` with the password
the person typed, and reads only their own entry. If a required value is missing the app **refuses
to start** and names the variable, rather than starting and rejecting every password.

**LDAPS and your certificate.** If the directory's certificate comes from AC's internal CA, Java
will not trust it and every sign-in will report *temporarily unavailable*. Give the JVM a
truststore containing that CA — for the container, mount it and point at it:

```bash
JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStore=/certs/ac-truststore.jks -Djavax.net.ssl.trustStorePassword=<...>
```

### What happens when someone signs in

The directory only answers *"is this really jdoe?"*. Everything else stays in Service Station, so
AC IT never has to model its roles unless they want to:

- **First sign-in creates the account.** Nobody pre-creates student logins. The person's student
  record is found by email — the directory's `mail` attribute, or `<user name>@austincollege.edu`
  — and linked to the account. Import students first (`/admin/students/import`); a student whose
  record is imported later is linked on their next sign-in.
- **Roles** are: STUDENT if linked to a student record; MODERATOR if an admin promoted that
  student on `/admin/moderators`; ADMIN / MODERATOR from the optional groups, re-checked at every
  sign-in, so removing someone from the group takes effect the next time they sign in.
- **A valid AC account with no student record and no group is refused**, with *"Your AC account
  worked, but it isn't set up in Service Station yet. Ask the Service Station office to add
  you."* No account is created for them.
- `jdoe`, `JDoe` and `jdoe@austincollege.edu` are the same person and the same account.

**Local accounts keep working alongside**, and are checked *first* — so the bootstrap admin and
any break-glass account still sign in while the directory is down. Two rules keep the two kinds
apart:

- An AC account can **never** be opened with a password stored in this app, and the reset and
  change-password pages refuse AC accounts (their password is AC IT's).
- An AC sign-in **never takes over** a local account with the same user name — it is refused
  instead. That is why the bootstrap admin should not be named after a real AC user.

### Checking it works

1. Sign in as a student whose record has been imported → their dashboard, with their hours.
2. Sign in as a member of the admin group (if you set one) → the admin dashboard.
3. Sign in as an AC account that is neither → the *"isn't set up in Service Station yet"* message.
4. Sign in with a wrong password → *"Sign in failed. Please check your user name and password."*
5. Sign in as the local bootstrap admin → still works.

The log records each first sign-in (`Created Service Station account for AC user 'jdoe' linked to
student AC50000`) and each refusal, with the reason. *Sign-in is temporarily unavailable* means the
directory could not be reached; the log line `Sign-in unavailable` has the cause — almost always
the URL, a firewall, or the certificate above.

### If AC IT would rather use single sign-on

If AC prefers a redirect to a Microsoft (Entra ID) or SAML sign-in page instead of a form, that is
**not built** — but it is a contained change: a second filter chain in `SecurityConfig` using
Spring Security's OAuth2/SAML support, reusing `DirectoryAccountMapper`'s first-sign-in rules.
Self-Service itself uses a form, which is why this does too.

*Verified 2026-10-08 with the `ldap` type against a stand-in LDAP server — in the test suite
(`DirectoryLoginIntegrationTest`, including a directory outage) and live: student first sign-in
and dashboard, mixed-case and `@austincollege.edu` user names, admin group, unknown AC account, and
the local break-glass admin.* **Not yet verified against a real Active Directory.** The
`active-directory` type is Spring Security's own provider, configured but never pointed at a real
domain controller — the first real test of it is AC IT's, using the checklist above.

## Sign-in limits (TC-125)

Every failed sign-in is counted against the **user name** that was typed. That matters most in
directory mode: each wrong password here is also a wrong password at AC's Active Directory, which
locks the account after its own number of failures. Without a limit, anyone could lock a student
out of every AC system by typing their user name on this sign-in page a few times. The limit is
checked **before** the password, so an attempt it refuses never reaches the directory.

With the defaults:

| Failures for one user name within 15 minutes | With Turnstile keys | Without keys |
|---|---|---|
| 1–2 | Normal sign-in | Normal sign-in |
| 3 | A CAPTCHA appears, and is required before the password is checked | **Blocked for 15 minutes** |
| 5 | **Blocked for 15 minutes**, even with the CAPTCHA solved | — |

- A successful sign-in clears the count. A block simply runs out; there is nothing to unlock.
- Only **wrong passwords** count. An AC account that isn't registered here, a disabled account, or
  the directory being unreachable does not push anyone toward a block.
- The same counter covers every spelling of a name: `JDoe`, `jdoe` and `jdoe@austincollege.edu`.
- Local accounts, including the bootstrap admin, are limited the same way.
- Counters are kept **in memory**: they reset when the app restarts, and if you ever run more
  than one copy of the app, each copy counts separately. A single container is the intended setup.

🏫 **Two things to decide:**

1. **Your AD lockout threshold.** Set `SSTATION_SIGNIN_BLOCK_AFTER` below it (with Turnstile), or
   `SSTATION_SIGNIN_CAPTCHA_AFTER` below it (without). The default of 5 is under almost any
   policy; Microsoft's security baseline is 10.
2. **Whether to use the CAPTCHA.** It needs a free Cloudflare account: in the Cloudflare dashboard,
   open **Turnstile**, add a widget for the app's hostname, and copy its **site key** and **secret
   key** into `SSTATION_TURNSTILE_SITE_KEY` / `SSTATION_TURNSTILE_SECRET_KEY`. Things to know:
   - The app must be able to make outbound HTTPS calls to `challenges.cloudflare.com` to confirm
     each solved CAPTCHA. If it can't reach Cloudflare, the attempt is **refused** with "the
     security check is temporarily unavailable" — so a Cloudflare outage can't become a way
     around the CAPTCHA. Only user names that already have three failures are affected.
   - The browser loads Cloudflare's script **only** on the sign-in page shown after a third failure.
     An ordinary sign-in never contacts Cloudflare. It is the one third-party script in the app,
     which otherwise serves every asset itself.
   - Without keys, the app logs `no Turnstile keys, so a user name is blocked after 3 failures` at
     startup. With them, it logs `Turnstile CAPTCHA after 3 failures, blocked after 5`.

### Checking it works

1. Sign in with a wrong password three times for one user name.
   - Without keys: the third try says *"Too many sign-in attempts for that user name"*, and even
     the right password is refused until the 15 minutes are up.
   - With keys: the third try shows the CAPTCHA. Solve it and the right password works.
2. Another user name is unaffected throughout.
3. The log has `Sign-in for 'jdoe' blocked for 15 minutes after 3 failed attempts`.

*Verified 2026-10-09: in the test suite (counting rules against a controllable clock, the CAPTCHA
gate, a stand-in for Cloudflare including an outage, and the real filter chain), and live against
Cloudflare's real verification endpoint with its official test keys, including the widget in a
browser.*

## Backups and upgrades

### Where the data lives

Every piece of state is in PostgreSQL — the app container is stateless. With the compose stack
([docker-compose.yml](sstation-next/docker-compose.yml)) Postgres runs in the `db` service and
keeps its data in the **named volume `sstation_pg`**; that volume *is* the database. If instead you
point `SSTATION_DB_URL` at an external Postgres, the data lives wherever that server keeps it, and
back it up with that server's normal procedure 🏫.

### Backing up

A plain SQL dump from `pg_dump` — nothing else to coordinate (no file storage, no queues). Against
the running compose stack:

```bash
docker compose -f docker-compose.yml exec db pg_dump -U sstation -d sstation \
  > sstation-backup.sql
```

or from any host that can reach the database:

```bash
pg_dump "postgresql://<user>:<password>@<host>:5432/sstation" > sstation-backup.sql
```

The dump is a plain SQL script and includes Flyway's schema-history table, so it recreates the
schema and the data together. Schedule it however AC IT already backs up Postgres — the app has no
opinion and no built-in backup mechanism.

### Restoring

```bash
docker compose -f docker-compose.yml exec -T db psql -U sstation -d sstation < sstation-backup.sql
```

(or `psql "postgresql://…" < sstation-backup.sql` from a host with a Postgres client.)

Restore into an **empty** database — dumping over an existing one with data collides on primary
keys. To roll a broken server back: stop the app, drop and recreate the database (or wipe the
volume with `docker compose down -v`, which deletes *all* of it), restore the backup, start the
app. Because the Flyway history rides in the dump, the restored database is exactly at the version
the dump was taken from; there is no manual migration step on restore.

### Upgrading

Flyway migrations are the app's business: **pending migrations apply automatically on boot** — the
schema history ships in the app (`V1`–`V5` today; future changes just add `V6`, `V7`, …). There is
no manual migration command to run.

To upgrade:

1. **Back up first** (above) — a failed migration is the one failure a restore is for.
2. Pull the new image (`docker compose pull`, or rebuild with `docker compose up --build`) or
   replace the JAR.
3. Restart the app: `docker compose up -d app` picks up the new image; on the no-Docker path,
   restart the service.

That is the whole upgrade. The honest version: a single-container deployment goes down for the
seconds the new process takes to boot and run pending migrations. This shape does not give
zero-downtime, and at this scale it doesn't need to. If a migration fails, the app refuses to
start and the log names the failed migration — restore the backup and investigate.

## Reverse proxy and TLS termination

AC IT almost certainly already runs a reverse proxy (nginx, Apache, IIS ARR — the app does not care
which) that terminates TLS and proxies plain HTTP to the app. That is a supported shape; the app
asks two things of it.

**1. Terminate TLS at the proxy, not in the app.** The app listens for plain HTTP (port `8080`)
and has no TLS configuration of its own — do not put a certificate on the container or JAR.

**2. Forward the standard `X-Forwarded-*` headers.** The `prod` profile sets
`server.forward-headers-strategy: framework`, so Spring reads these headers and uses them to know
the app is being served over HTTPS. This matters for every absolute URL the app builds — most
visibly the password-reset link emailed to users, which is derived from the request's scheme and
host. Without the headers, a TLS-terminating proxy makes the app think every request arrived over
plain HTTP, and the links come out `http://…` — unusable from outside the network. Set at minimum:

| Header | Value |
|---|---|
| `X-Forwarded-Proto` | `https` when the client connection used TLS |
| `X-Forwarded-Host` | the public hostname the user typed |
| `X-Forwarded-For` | the client IP (not used for anything security-relevant today, but standard) |

There is no `SSTATION_SERVER_URL` and no other app-level URL setting — the app always derives its
public URL from the request it actually receives, which is exactly why these headers are required
rather than optional.

A minimal nginx `server` block that terminates TLS and proxies to the app on the same host:

```nginx
server {
    listen 443 ssl;
    server_name service-station.austincollege.edu;            # 🏫 your public hostname

    ssl_certificate     /etc/nginx/tls/service-station.crt;   # 🏫 your certificate
    ssl_certificate_key /etc/nginx/tls/service-station.key;   # 🏫 your private key

    location / {
        proxy_pass http://127.0.0.1:8080;                     # the app (container or java -jar)
        proxy_http_version 1.1;
        proxy_set_header Host              $host;
        proxy_set_header X-Forwarded-Proto $scheme;           # https, because nginx terminated TLS
        proxy_set_header X-Forwarded-Host  $host;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
    }
}
```

(`proxy_pass http://127.0.0.1:8080` assumes the app publishes port 8080 on the host — the compose
stack's `app` service does. For Apache the equivalent is `RequestHeader set X-Forwarded-Proto
"https"` plus `ProxyPass`/`ProxyPassReverse`; for IIS ARR the built-in server-name and protocol
forwarding do the same. Any proxy that sends these headers works.)

A quick way to confirm the headers are honoured: request a password-reset link through the public
URL and check the emailed link is `https://…`. On a mail-disabled install the app logs it:
`[mail disabled] password reset link for <email>: <url>`.

*Verified 2026-09-08 against a locally-built image (TC-110b): with `X-Forwarded-Proto: https` and
`X-Forwarded-Host: service-station.austincollege.edu`, the app logs the reset link as
`https://service-station.austincollege.edu/reset-password?token=…` and redirects to the same host;
without the headers, the same request produces `http://localhost:8080/…`.*

## Build without Docker

```bash
cd sstation-next
./gradlew bootJar   # Windows: gradlew.bat bootJar
java -jar build/libs/sstation-next-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Requires JDK 21 only at build time if you use the Gradle wrapper on the host.

## Showcase deployments — not the AC IT path

The two sections that follow describe throwaway demo deployments, not the production runbook. The
deliverable for AC IT is everything above this point; these exist only to show a working copy
before AC IT decides to host the app themselves.

### Public demo — Vercel (TC-122)

**Decided 2026-09-07: the public demo goes to Vercel, not AWS.** The AWS EC2 + RDS outline below is
kept as a documented alternate and is **not** the current plan.

**This section is not for AC IT.** It describes the throwaway showcase deployment. If you are
setting the application up for Austin College, everything you need is above this section.

Vercel runs [OCI container images as Functions on Fluid compute](https://vercel.com/docs/functions/container-images),
so the image CI already builds and smoke-tests (TC-115) is what gets deployed — this is a hosting
swap, not a re-architecture. Compared with the EC2 plan it drops the VPC, security groups, manual
TLS and a separately-billed RDS instance, and gives the JVM **1 vCPU / 2 GB** instead of a
t3.micro's 1 GB.

**Not yet implemented — see [TC-122](TRELLO_CARDS.md) for the full card.** Three items are real
work rather than configuration, and the first one bites silently:

1. **Sessions must leave instance memory.** This app is session-based (Spring Security form login)
   and currently uses in-memory Tomcat sessions. On Fluid compute any instance may serve any
   request, so with more than one instance live a signed-in user is randomly returned to the login
   page. Fix is `spring-session-jdbc` against the Postgres we already use.
   **This is a Vercel constraint, not a requirement for AC IT** — a single container on a single
   host, which is the expected shape for an office this size, is perfectly fine as-is.
2. **`server.port` is hardcoded to `8080`** and must become `${PORT:8080}`; Vercel routes to
   `$PORT`.
3. **A `Dockerfile.vercel` or `vercel.json` `services` entrypoint** pointing at
   `sstation-next/Dockerfile`, so there is no second Dockerfile to drift from the tested one.

Other things worth knowing before relying on it:

- **Scale to zero.** Production instances shut down after 5 minutes without traffic, so the next
  visitor pays a full Spring Boot cold start. Fine for a demo; measure it and put the number next
  to the demo link.
- **No Static IPs or Secure Compute** for container-image functions. Irrelevant for a public demo,
  but it rules Vercel out if AC IT ever needs IP allowlisting to reach an internal SMTP relay or
  database.
- **Database — Supabase free tier works, with three specifics** (checked against Supabase's docs on
  2026-09-07). 500 MB is far more than demo seed data needs, but:
  - **Use the Supavisor _shared pooler_ connection string, in _session_ mode.** Two independent
    reasons, either of which alone breaks the connection: free-tier **direct** connections are
    **IPv6-only** without the paid IPv4 add-on, while the shared pooler is IPv4 on every tier; and
    **transaction mode does not support prepared statements**, which Hibernate/JDBC uses on
    practically every query. Session mode supports them.
  - **A free project pauses after 7 days with no database activity**, and needs a manual unpause.
    For a link someone opens two weeks after you send it, that means it is down exactly when it
    matters. Add a **Vercel Cron** (a daily schedule is available on Hobby) hitting
    `/actuator/health` — Boot's `db` health indicator issues a real query, which resets Supabase's
    idle timer and wakes the scaled-to-zero function at the same time. No application code needed.
  - Neon via the Vercel Marketplace is the alternative if the pausing proves annoying.
- **Vercel Hobby is "non-commercial personal use only".** A student project demoed to a college IT
  department, with no payments and nobody paid to build it, reads as non-commercial — but it is the
  same class of question TC-119 raised about Highcharts, so it is written down rather than assumed.
- **Demo passwords** go in Vercel environment variables, never the repo. `DemoAccountSeeder` still
  refuses to start without them (TC-114).

### AWS EC2 demo (TC-116 — ⏸️ superseded, kept as a documented alternate)

The retired AWS-based showcase plan, kept because its topology and cost notes stay accurate if AWS
is ever revisited. This is **not** the AC IT self-hosting path — that is everything above the
Showcase heading.

**Recommended:** `t3.micro` EC2 (app container only) + `db.t4g.micro` RDS Postgres 16 in the same VPC.

1. Create RDS; security group allows `5432` only from the EC2 security group.
2. On EC2 (Amazon Linux 2023): install Docker, clone/pull image, set `SSTATION_DB_*` in `.env`.
3. Run: `docker compose -f docker-compose.ec2.yml up -d` (see [sstation-next/docker-compose.ec2.yml](sstation-next/docker-compose.ec2.yml)).
4. Terminate TLS with Caddy or nginx on the host (`443` → `127.0.0.1:8080`).
5. Set `SPRING_PROFILES_ACTIVE=prod,demo` and strong `SSTATION_DEMO_*_PASSWORD` env vars (see `.env.example`).

**Budget fallback (demo only):** run `docker-compose.yml` on a single t3.micro — high OOM risk on 1 GiB RAM.

Target cost: under ~$25/month (EC2 + RDS, no ALB) where free tier applies.

## Legacy Grails app

The Grails 2.4.4 WAR deploy path is unchanged under [sstation/](sstation/). See [CLAUDE.md](CLAUDE.md).
