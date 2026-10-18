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

**You need:** Java 17 or newer, and either IntelliJ IDEA (includes Maven) or Maven 3.9+.

**Run in IntelliJ:** File → Open → choose this folder. Wait for Maven to download
dependencies, then open `src/main/java/edu/campbellsville/lablink/LabLinkApplication.java`
and click the green ▶ next to `main`.

**Run from a terminal:**

```
mvn spring-boot:run
```

Then open http://localhost:8080 and log in with a demo account:

| Role | Email | Password |
|---|---|---|
| Lab Manager | manager@lablink.edu | manager123 |
| Faculty | faculty@lablink.edu | faculty123 |
| Student | student@lablink.edu | student123 |

The development database (H2) lives in memory, so it resets every time the app restarts.

**Run the tests:** `mvn verify` (GitHub Actions also runs them on every push).

**Use MySQL instead of H2:** create an empty `lablink` database, set `DB_URL`,
`DB_USERNAME`, `DB_PASSWORD`, and run `mvn spring-boot:run -Dspring-boot.run.profiles=mysql`.

## Project Layout

```
src/main/java/edu/campbellsville/lablink/
  model/        JPA entities (User, Lab, Equipment, Reservation) and enums
  repository/   Spring Data repositories (database access)
  service/      Business rules (UserService, InventoryService, ReservationService)
  web/          Controllers and form classes
  security/     Spring Security configuration and user lookup
  config/       Demo data loaded at startup
src/main/resources/
  db/schema.sql DDL that creates the tables
  templates/    Thymeleaf pages
docs/           Milestone 2 design documents, UML, ADRs, project plan
```

## Design Documents
See [docs/README.md](docs/README.md) for the architecture document, UML, database
design, ADRs and project plan.

## Branching
Work on feature/<name> branches and merge into main through
reviewed pull requests. Never commit directly to main.

## License
MIT — see LICENSE.
