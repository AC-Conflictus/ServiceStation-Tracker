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

### Option B — Production-shaped stack (Postgres + Flyway, no seed users)

```bash
cd sstation-next
docker compose up --build
```

- Postgres 16 on port 5432 (internal network only).
- App on **http://localhost:8080** with `SPRING_PROFILES_ACTIVE=prod`.
- Flyway creates schema; **no login accounts** until [TC-114](TRELLO_CARDS.md) demo profile lands.
- Health: **http://localhost:8080/actuator/health**

Optional: copy [.env.example](sstation-next/.env.example) to `sstation-next/.env` and adjust `POSTGRES_PASSWORD`.

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
| `SPRING_PROFILES_ACTIVE` | Always | `dev` (H2 + seed), `prod` (Postgres, no seed). `demo` — TC-114. |
| `SSTATION_DB_URL` | `prod` | JDBC URL, e.g. `jdbc:postgresql://host:5432/sstation` |
| `SSTATION_DB_USER` | `prod` | DB user |
| `SSTATION_DB_PASSWORD` | `prod` | DB password |
| `SSTATION_MAIL_HOST` | `prod` | SMTP host; leave empty for log-only notifications |
| `SSTATION_MAIL_PORT` | `prod` | Default `587` |
| `SSTATION_MAIL_USER` / `SSTATION_MAIL_PASSWORD` | `prod` | SMTP credentials (AC IT) |
| `SSTATION_MAIL_FROM` | All | From address; default `no-reply@austincollege.edu` |
| `SSTATION_DEV_*_PASSWORD` | `dev` | Override seeded dev passwords |

Never commit real production secrets. Use a vault or AWS SSM on EC2.

## AWS EC2 demo (TC-116 — outline)

**Recommended:** `t3.micro` EC2 (app container only) + `db.t4g.micro` RDS Postgres 16 in the same VPC.

1. Create RDS; security group allows `5432` only from the EC2 security group.
2. On EC2 (Amazon Linux 2023): install Docker, clone/pull image, set `SSTATION_DB_*` in `.env`.
3. Run: `docker compose -f docker-compose.ec2.yml up -d` (see [sstation-next/docker-compose.ec2.yml](sstation-next/docker-compose.ec2.yml)).
4. Terminate TLS with Caddy or nginx on the host (`443` → `127.0.0.1:8080`).
5. Activate **TC-114** `demo` profile for showcase logins; do not expose `admin_secret` on the public internet.

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
