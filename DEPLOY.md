# Deploy — Service Station (`sstation-next`)

Spring Boot 3 / Java 21 rewrite. For local JDK development see [demo.md](demo.md).

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

**AWS / internet:** set strong `SSTATION_DEMO_*_PASSWORD` values — the Java app has **no** `admin_secret` fallback when `demo` is active.

Optional: adjust `POSTGRES_PASSWORD` in `.env` as well.

Stop: `Ctrl+C`, then `docker compose down` (add `-v` to drop the Postgres volume).

## Build the image manually

```bash
cd sstation-next
docker build -t sstation-next:local .
docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev sstation-next:local
```

## Environment variables

| Variable | Used when | Purpose |
|----------|-----------|---------|
| `SPRING_PROFILES_ACTIVE` | Always | `dev` (H2 + dev seed), `prod` (Postgres), `prod,demo` (Postgres + showcase seed, TC-114) |
| `SSTATION_DEMO_*_PASSWORD` | `demo` | **Required** when `demo` profile is on (no defaults in code) |
| `SSTATION_DB_URL` | `prod` | JDBC URL, e.g. `jdbc:postgresql://host:5432/sstation` |
| `SSTATION_DB_USER` | `prod` | DB user |
| `SSTATION_DB_PASSWORD` | `prod` | DB password |
| `SSTATION_MAIL_HOST` | `prod` | SMTP host; leave empty for log-only notifications |
| `SSTATION_MAIL_PORT` | `prod` | Default `587` |
| `SSTATION_MAIL_USER` / `SSTATION_MAIL_PASSWORD` | `prod` | SMTP credentials (AC IT) |
| `SSTATION_MAIL_FROM` | All | From address; default `no-reply@austincollege.edu` |
| `SSTATION_DEV_*_PASSWORD` | `dev` | Override seeded dev passwords |
| `SSTATION_BOOTSTRAP_ADMIN_PASSWORD` | bare `prod` | **Required on a first boot against an empty database** — creates the first administrator (TC-121). Minimum 8 characters. Ignored once any account exists. |
| `SSTATION_BOOTSTRAP_ADMIN_USERNAME` | bare `prod` | Username for that account; default `admin` |

Never commit real production secrets. Use a vault or AWS SSM on EC2.

## First login on a new production database (TC-121)

A bare `prod` boot **never seeds** — that is deliberate, and it means a brand-new database has
**no accounts at all**. Without the two variables below you get a sign-in form that rejects
every credential, which looks like a broken deployment rather than an empty one.

> The `demo` profile does not have this problem, because `DemoAccountSeeder` creates accounts.
> If you are running `prod,demo` for the showcase, skip this section.

**1. Set the bootstrap variables before the first start:**

```bash
SSTATION_BOOTSTRAP_ADMIN_USERNAME=ac-it-admin      # optional, defaults to "admin"
SSTATION_BOOTSTRAP_ADMIN_PASSWORD='<a strong one-time password>'
```

On startup you will see, at WARN level:

```
[bootstrap] created the first administrator 'ac-it-admin'. This password came from an
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

**4. Create the real accounts** from the admin UI (`/admin/students`, `/admin/moderators`).

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

## AWS EC2 demo (TC-116 — outline)

**Recommended:** `t3.micro` EC2 (app container only) + `db.t4g.micro` RDS Postgres 16 in the same VPC.

1. Create RDS; security group allows `5432` only from the EC2 security group.
2. On EC2 (Amazon Linux 2023): install Docker, clone/pull image, set `SSTATION_DB_*` in `.env`.
3. Run: `docker compose -f docker-compose.ec2.yml up -d` (see [sstation-next/docker-compose.ec2.yml](sstation-next/docker-compose.ec2.yml)).
4. Terminate TLS with Caddy or nginx on the host (`443` → `127.0.0.1:8080`).
5. Set `SPRING_PROFILES_ACTIVE=prod,demo` and strong `SSTATION_DEMO_*_PASSWORD` env vars (see `.env.example`).

**Budget fallback (demo only):** run `docker-compose.yml` on a single t3.micro — high OOM risk on 1 GiB RAM.

Target cost: under ~$25/month (EC2 + RDS, no ALB) where free tier applies.

## Build without Docker

```bash
cd sstation-next
./gradlew bootJar   # Windows: gradlew.bat bootJar
java -jar build/libs/sstation-next-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Requires JDK 21 only at build time if you use the Gradle wrapper on the host.

## Legacy Grails app

The Grails 2.4.4 WAR deploy path is unchanged under [sstation/](sstation/). See [CLAUDE.md](CLAUDE.md).
