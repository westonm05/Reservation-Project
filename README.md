# LabLink — Lab & Equipment Reservation System

CS450 Software Engineering, Campbellsville University

LabLink lets students and faculty reserve lab spaces and equipment online,
and lets lab managers manage inventory and approve requests.

## Team
- Weston Mattingly — Project Coordinator, Authentication
- Isaiah Bain — Reservations, Code Review Lead
- Dawson Brown — Front End, UI/UX Lead
- PJ — Database, Inventory, QA/DevOps

## Tech Stack
Java 17, Spring Boot 3, Spring Security, Spring Data JPA,
Thymeleaf, MySQL (H2 for development), Maven, GitHub Actions

## Getting Started

**You need:** Java 17 or newer, and either IntelliJ IDEA (has Maven built in) or Maven 3.9+.
No database install is needed for development — the app uses an in-memory H2 database.

**IntelliJ:** File → Open → choose this folder → wait for Maven to load →
open `LabLinkApplication.java` → click the green ▶ next to `main`.

**Command line:**
```bash
mvn spring-boot:run     # start the app at http://localhost:8080
mvn test                # run all tests
```

**Demo accounts** (password for all: `Password123!`)

| Email | Role |
|---|---|
| student@lablink.edu | Student |
| faculty@lablink.edu | Faculty |
| manager@lablink.edu | Lab Manager (sees Approvals + Add equipment) |

Data resets every time the app restarts. To run against MySQL instead, see
[docs/database-design.md](docs/database-design.md#4-ddl-script).

## Project Structure
```
src/main/java/edu/campbellsville/lablink/
  config/      security rules (who can see which pages)
  web/         controllers - handle each URL, pick a page to show
  web/dto/     form objects - data typed into forms, with validation
  service/     business rules - double booking, approvals, registration
  repository/  database access (Spring Data JPA)
  model/       entities - one class per database table
src/main/resources/
  templates/   Thymeleaf HTML pages
  static/css/  stylesheet
  schema.sql   database tables (DDL)
  data.sql     demo data
```

## Documentation
- [Architecture](docs/architecture.md) — pattern, component diagram, tech stack
- [UML models](docs/uml.md) — use case, class, sequence, activity diagrams
- [Database design](docs/database-design.md) — ER diagram, 3NF, DDL
- [Architecture Decision Records](docs/adr/)
- [Project plan](docs/project-plan.md)

## Branching
Work on feature/<name> branches and merge into main through
reviewed pull requests. Never commit directly to main.
GitHub Actions builds and tests every pull request; merge only when it is green.

## License
MIT — see LICENSE.
