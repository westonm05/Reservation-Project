# Containerizing LabLink

CS450 Week 14 — Deployment & Operations. Files: [`Dockerfile`](../Dockerfile),
[`docker-compose.yml`](../docker-compose.yml), [`docker-compose.prod.yml`](../docker-compose.prod.yml),
[`.dockerignore`](../.dockerignore), [`.env.example`](../.env.example).

## Why containers

Before this week, running LabLink meant installing the right Java version and Maven, and running it
against MySQL meant installing and configuring MySQL by hand. A container image packages the app with
the exact Java runtime it needs, so it runs the same way on a teammate's laptop, in GitHub Actions and
on a production server. Docker Compose starts the app and its database together with one command.

LabLink has two services, so it uses both a Dockerfile (how to build the app image) and a
`docker-compose.yml` (how to run the app and MySQL together).

## 1. The Dockerfile: a two-stage build

| Stage | Base image | What happens | Kept in final image? |
|---|---|---|---|
| `build` | `maven:3.9-eclipse-temurin-17` | Copy `pom.xml`, download dependencies, copy `src/`, run `mvn package` | No |
| runtime | `eclipse-temurin:17-jre` | Copy only `lablink-*.jar` from the build stage; run it | Yes |

Decisions and why:

- **Two stages.** Maven, the JDK compiler and the source code are only needed to build. Leaving them
  out makes the final image smaller and gives an attacker less to work with.
- **`pom.xml` copied before `src/`.** Docker caches each step. Dependencies change rarely, so downloading
  them in their own layer means a code-only change rebuilds in seconds instead of re-downloading everything.
- **Tests skipped inside the image build** (`-DskipTests -Dcheckstyle.skip=true`). The CI pipeline runs lint
  and every test on the same commit before an image is ever built, so running them twice only slows builds.
- **Runs as a non-root user** (`lablink`). If the app were ever compromised, the attacker would not be root
  inside the container.
- **`TZ=America/New_York`.** LabLink checks "a reservation must start in the future" with
  `LocalDateTime.now()`. Containers default to UTC, which is 5 hours ahead of campus time, so without this
  a student could not book a lab for 3 pm at 11 am. The entrypoint passes `-Duser.timezone=$TZ` to Java.
  (Found while containerizing; see corrective-maintenance item C1 in [maintenance.md](maintenance.md).)
- **`-XX:MaxRAMPercentage=75`** sizes the Java heap from the container's memory limit instead of the host's,
  and **`-XX:+ExitOnOutOfMemoryError`** makes the JVM exit (so Docker restarts it) instead of limping along.
- **`HEALTHCHECK`** calls `/actuator/health/liveness` every 30 s. Docker marks the container *unhealthy*
  after 3 failures.
- **`exec java ...` in the entrypoint** makes Java process 1, so `docker stop` reaches the JVM directly and
  Spring's graceful shutdown (`server.shutdown=graceful`) finishes in-flight requests.

`.dockerignore` keeps `.git`, `target/`, `docs/`, IDE folders and `.env` out of the build context, so
local secrets can never be copied into an image.

## 2. docker-compose.yml: app + MySQL

```
          your browser
               │  http://localhost:8080
┌──────────────┼──────────────────────────── docker compose ─┐
│        ┌─────▼─────┐   JDBC (db:3306)   ┌───────────────┐  │
│        │    app    │ ─────────────────▶ │      db       │  │
│        │  LabLink  │                    │   MySQL 8.4   │  │
│        │   :8080   │                    │ no host port  │  │
│        └───────────┘                    └──────┬────────┘  │
│                                         volume: db-data    │
└────────────────────────────────────────────────────────────┘
```

| Setting | Why |
|---|---|
| `db` has **no `ports:`** | Only the app container can reach MySQL; it is not exposed on the host or the internet |
| `db-data` **named volume** | The database survives `docker compose down` and container rebuilds. `down -v` deletes it on purpose |
| `db` **health check** (`mysqladmin ping`) + `app` **`depends_on: condition: service_healthy`** | MySQL takes ~15 s to start the first time. Without this the app would start first, fail to connect, and crash |
| `app` **health check** on `/actuator/health/readiness` | `docker compose up --wait` (used in CI) and the production proxy only send traffic once the app is ready |
| `SPRING_PROFILES_ACTIVE=mysql,seed` by default | Local runs and demos get the demo accounts. Production overrides this to `mysql` only |
| Every value has a default (`${DB_PASSWORD:-...}`) | The stack starts with no setup; a `.env` file overrides the defaults |
| `restart: unless-stopped` | Docker restarts a crashed container automatically |
| `json-file` log rotation (10 MB × 5) | Logs can't fill the disk |

Flyway runs the migrations in `db/migration` on startup, so the first `docker compose up` builds the
whole schema in the empty MySQL database. No manual SQL is needed.

## 3. Using it

```bash
cp .env.example .env            # optional: change the passwords
docker compose up --build       # build the image and start both containers
# open http://localhost:8080   (manager@lablink.edu / manager123)
docker compose logs -f app      # follow the app's logs
docker compose ps               # both should say "healthy"
docker compose down             # stop (data kept)
docker compose down -v          # stop and delete the database
```

## 4. Verification

The **Docker Smoke Test** job in [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) runs on every
push and pull request:

1. `docker compose up -d --build --wait` — builds the image and waits until both containers are healthy
2. `GET /actuator/health` must return `"status":"UP"`
3. `GET /login` must return 200
4. `GET /api/labs` must return the seeded labs (proves the app reached MySQL and Flyway + seed data ran)
5. Prints `docker compose ps` and the app logs, then `docker compose down -v`

So every change is proven to build into a working container against real MySQL, not just H2.

## 5. Production variant

[`docker-compose.prod.yml`](../docker-compose.prod.yml) is layered on top of the base file and changes
four things: no demo data (`mysql` profile only), a pinned image tag, memory limits, and a Caddy reverse
proxy in front that serves HTTPS and is the only published port. See [deployment.md](deployment.md).
