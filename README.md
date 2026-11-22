# LabLink — Lab & Equipment Reservation System

[![CI](https://github.com/westonm05/Reservation-Project/actions/workflows/ci.yml/badge.svg)](https://github.com/westonm05/Reservation-Project/actions/workflows/ci.yml)

CS450 Software Engineering, Campbellsville University

LabLink lets students and faculty reserve lab rooms and equipment online, and lets lab managers keep
the inventory up to date and approve requests. It replaces sign-up sheets and email threads with one
place that shows what is free, prevents double bookings, and keeps a history of every reservation.

## Contents

- [Features](#features)
- [Architecture overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [Installation and setup](#installation-and-setup)
- [Running the application](#running-the-application)
- [Running the tests](#running-the-tests)
- [API documentation](#api-documentation)
- [Configuration](#configuration)
- [Deployment and operations](#deployment-and-operations)
- [Project layout](#project-layout)
- [Contributing](#contributing)
- [Team](#team)

## Features

- Registration and login with three roles: **student**, **faculty**, **lab manager**
- Lab and equipment management (create, edit, delete) for lab managers
- Search and filter labs and equipment
- Reservation requests with **double-booking prevention** and equipment loan-length limits
- Faculty requests are approved automatically; lab managers approve or reject student requests
- Reschedule or cancel your own reservations; full status history for every reservation
- Week view of each lab's availability
- Admin page: add faculty and lab-manager accounts, change roles
- Account page with password change
- **Public REST API** for lab availability ([docs/api.md](docs/api.md))
- Health checks, metrics and structured event logs for operations ([docs/monitoring.md](docs/monitoring.md))

## Architecture overview

LabLink is a single Spring Boot application (a modular monolith, [ADR-0001](docs/adr/0001-layered-mvc-monolith.md))
organized into layers. Each layer only calls the one below it.

```
 Browser ──HTML──▶ ┌────────────────────────────────────────────────────────┐
                   │ Security filter chain (Spring Security: login, roles)  │
 Campus systems ─▶ ├────────────────────────────────────────────────────────┤
   (JSON API)      │ web/  controllers + Thymeleaf      api/  REST JSON     │  Presentation
                   ├────────────────────────────────────────────────────────┤
                   │ service/  business rules: reservations, inventory,     │  Business
                   │           users, availability                          │
                   ├────────────────────────────────────────────────────────┤
                   │ repository/  Spring Data JPA                           │  Persistence
                   ├────────────────────────────────────────────────────────┤
                   │ model/  JPA entities                                   │  Domain
                   └────────────────────────────┬───────────────────────────┘
                                                │ JDBC (Flyway runs migrations at startup)
                      H2 (development, tests) / MySQL 8.4 (production)
```

- **Server-side rendering** with Thymeleaf and session-based login ([ADR-0002](docs/adr/0002-server-side-rendering-and-session-auth.md)).
- **Database schema** is built by Flyway migration scripts; Hibernate only validates it
  ([ADR-0003](docs/adr/0003-h2-dev-mysql-prod-handwritten-ddl.md), [ADR-0005](docs/adr/0005-flyway-migrations-and-sql-seed-data.md)).
- **Double booking** is prevented in the service layer with an interval-overlap query
  ([ADR-0004](docs/adr/0004-prevent-double-booking-in-service-layer.md)).
- In production it runs as a Docker container next to MySQL, behind a Caddy HTTPS reverse proxy
  ([docs/deployment.md](docs/deployment.md)).

Full details: [architecture document](docs/architecture.md), [UML](docs/uml.md),
[database design](docs/database-design.md), and the [docs index](docs/README.md).

**Tech stack:** Java 17, Spring Boot 3.5 (Web, Security, Data JPA, Validation, Actuator), Thymeleaf,
Flyway, MySQL 8.4 / H2, Maven, JUnit 5 + Mockito, JaCoCo, Checkstyle, Docker, GitHub Actions.

## Prerequisites

Pick **one** way to run it:

| To... | You need |
|---|---|
| Run everything in containers (closest to production, uses MySQL) | [Docker Desktop](https://www.docker.com/products/docker-desktop/) (or Docker Engine 24+ with the Compose plugin) |
| Develop and run tests on your machine | **Java 17+** and **Maven 3.9+** (IntelliJ IDEA includes Maven), and Git |

## Installation and setup

```bash
git clone https://github.com/westonm05/Reservation-Project.git
cd Reservation-Project
```

- **Docker:** nothing else to install. Optionally `cp .env.example .env` to change passwords or the port.
- **Local Java:** open the folder in IntelliJ (File → Open) and let Maven download the dependencies, or run
  `mvn -q dependency:resolve` once from a terminal.

## Running the application

### Option 1: Docker Compose (app + MySQL)

```bash
docker compose up --build
```

The first build takes a few minutes (it downloads Maven dependencies); later builds are fast.
When both containers show `healthy` (`docker compose ps`), open **http://localhost:8080**.
Data is stored in MySQL in a Docker volume, so it survives restarts.

```bash
docker compose logs -f app      # follow the logs
docker compose down             # stop (keeps the data)
docker compose down -v          # stop and delete the database
```

How the image is built and why: [docs/containerization.md](docs/containerization.md).

### Option 2: Local Java with the in-memory database

```bash
mvn spring-boot:run
```

Or in IntelliJ, open `src/main/java/edu/campbellsville/lablink/LabLinkApplication.java` and click the green ▶.
Open **http://localhost:8080**. The H2 database lives in memory and resets every time the app restarts.

To use your own MySQL instead: create an empty `lablink` database, set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`,
and run `mvn spring-boot:run -Dspring-boot.run.profiles=mysql,seed`.

### Demo accounts

Both options load demo data (the `seed` profile), including these accounts:

| Role | Email | Password |
|---|---|---|
| Lab Manager | manager@lablink.edu | manager123 |
| Faculty | faculty@lablink.edu | faculty123 |
| Student | student@lablink.edu | student123 |

Production never loads the demo data; see [docs/deployment.md](docs/deployment.md).

## Running the tests

```bash
mvn verify
```

This runs, in order: **Checkstyle** lint → **163 unit and integration tests** (JUnit 5, Mockito, Spring Boot
test slices against H2) → packages the `.jar` → **JaCoCo** coverage check (fails below 70 % line coverage in
the `service` package).

| Command | What it does |
|---|---|
| `mvn test` | Tests only |
| `mvn test -Dtest=ReservationServiceTest` | One test class |
| `mvn checkstyle:check` | Lint only (run before pushing) |
| `mvn verify` then open `target/site/jacoco/index.html` | Coverage report |

GitHub Actions runs three checks on every push and pull request: **Lint**, **Build & Test**, and
**Docker Smoke Test** (builds the image, starts it with MySQL, and checks the health endpoint, login page and API).

## API documentation

LabLink has a small public, read-only JSON API so other campus systems can show lab availability.
No login is needed for `GET` requests. Full reference with examples and error codes: **[docs/api.md](docs/api.md)**.

| Method | Path | Returns |
|---|---|---|
| `GET` | `/api/labs` | Every lab that can be reserved |
| `GET` | `/api/labs/{id}/availability?from=...&to=...` | Busy time slots for one lab in a window (max 31 days), and `free: true/false` |

```bash
curl http://localhost:8080/api/labs
curl "http://localhost:8080/api/labs/5/availability?from=2026-12-01T08:00&to=2026-12-01T18:00"
```

Operations endpoints (Spring Boot Actuator):

| Path | Access | Purpose |
|---|---|---|
| `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` | Public | Health checks for Docker, the load balancer and uptime monitors |
| `/actuator/metrics`, `/actuator/info` | Lab managers only (blocked from the internet in production) | Request counts, response times, JVM and database-pool metrics |

## Configuration

All settings come from environment variables (or a `.env` file for Docker Compose). Full list with
production values: [docs/deployment.md §4](docs/deployment.md#4-environment-variables).

| Variable | Default | Purpose |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | none (H2) locally; `mysql,seed` in Compose | `mysql` = MySQL; `seed` = demo data |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Compose's `db` container | MySQL connection |
| `LABLINK_ADMIN_EMAIL`, `LABLINK_ADMIN_PASSWORD` | empty | Create the first lab manager on an empty production database |
| `TZ` | `America/New_York` | Campus time zone; reservation rules use local time |
| `APP_PORT` | `8080` | Host port in Docker Compose |

## Deployment and operations

| Document | Covers |
|---|---|
| [docs/containerization.md](docs/containerization.md) | Dockerfile and Compose design, how it's verified in CI |
| [docs/deployment.md](docs/deployment.md) | Deploying to a production server: infrastructure, environment variables, database setup, HTTPS, backups, releases, rollback |
| [docs/monitoring.md](docs/monitoring.md) | Metrics, logged events, alerting, incident response and runbook |
| [docs/maintenance.md](docs/maintenance.md) | Anticipated corrective, adaptive, perfective and preventive maintenance |

## Project layout

```
src/main/java/edu/campbellsville/lablink/
  model/        JPA entities (User, Lab, Equipment, Reservation, Department, ...) and enums
  repository/   Spring Data repositories (database access)
  service/      Business rules (UserService, InventoryService, ReservationService, AvailabilityService)
  web/          Controllers and form classes for the HTML pages
  api/          REST API controller and its JSON records
  security/     Spring Security configuration, user lookup, login event logging
src/main/resources/
  db/migration/ Flyway migration scripts (V1__..V4__) that build the schema
  db/seed/      Demo/test data (R__seed_test_data.sql)
  templates/    Thymeleaf pages
src/test/java/  Unit and integration tests
deploy/         Production files: Caddyfile (HTTPS proxy), backup.sh
docs/           Design documents, UML, ADRs, project plan, API, deployment and operations
Dockerfile, docker-compose.yml, docker-compose.prod.yml, .env.example
```

## Contributing

We use **GitHub Flow**. Full rules: [docs/git-workflow.md](docs/git-workflow.md).

1. **Branch** from the latest `main`: `git switch -c <type>/<short-description>`
   where type is `feature`, `fix`, `test`, `docs`, `ci` or `refactor` (e.g. `feature/email-notifications`).
2. **Commit** with [Conventional Commits](https://www.conventionalcommits.org/): `feat: ...`, `fix: ...`,
   `test: ...`, `docs: ...`, `ci: ...`. One logical change per commit; never commit passwords or `.env`.
3. **Check locally** before pushing: `mvn verify` (lint + tests + coverage).
4. **Open a pull request** into `main`. Fill in the template: what changed, why, how you tested it,
   screenshots for UI changes, and the backlog item.
5. **CI must be green** (Lint, Build & Test, Docker Smoke Test). Request a review from a teammate
   (Isaiah Bain is Code Review Lead) and answer every comment.
6. **Squash and merge**; the branch is deleted automatically. `main` is protected: no direct pushes.

Code conventions:

- Keep the layers: controllers call services, services call repositories. Business rules go in `service/`
  with unit tests.
- Schema changes are a new Flyway file (`V5__describe_change.sql`); never edit a migration that has already run.
  Prefer additive changes (new columns/tables) so releases can be rolled back.
- New features need tests; the service layer must stay above 70 % line coverage.
- Log important events as `event.name key=value` and never log passwords
  (see [docs/monitoring.md §4](docs/monitoring.md#4-events-we-log)).

## Team

- Weston Mattingly — Project Coordinator, Authentication
- Isaiah Bain — Reservations, Code Review Lead
- Dawson Brown — Front End, UI/UX Lead
- PJ — Database, Inventory, QA/DevOps

## License

MIT — see [LICENSE](LICENSE).
