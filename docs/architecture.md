# LabLink Architecture Document

CS450 Software Engineering · originally written for Milestone 2, **updated for the v1.0.0 release (Week 15)**
Team: Weston Mattingly, Isaiah Bain, Dawson Brown, PJ

## 1. System overview

LabLink is a web application that lets students and faculty reserve lab rooms and equipment, and lets lab managers maintain the inventory and approve reservation requests. Users interact through a browser; all pages are rendered on the server and data is stored in a relational database.

**Actors**

| Actor | Can do |
|---|---|
| Student | Register, log in, search labs and equipment, view lab availability, request reservations (need approval), reschedule pending requests, cancel own reservations, change password |
| Faculty | Everything a student can; reservations are approved automatically. Accounts are created by a lab manager |
| Lab Manager | Everything above, plus create/edit/delete labs and equipment, approve or reject pending requests, create faculty/lab-manager accounts and change roles |
| Campus system | Reads lab lists and availability from the public JSON API (no login) |

**Core business rules**

1. A reservation is for exactly one lab **or** one piece of equipment.
2. A reservation must start in the future and end after it starts.
3. A lab or piece of equipment cannot be double-booked: a new request is refused if it overlaps any PENDING or APPROVED reservation for the same resource.
4. Inactive labs and equipment that is in MAINTENANCE or RETIRED cannot be reserved.
5. Only the person who made a reservation can reschedule (while PENDING) or cancel it; only a lab manager can approve or reject it.
6. An equipment reservation can't be longer than its category's maximum loan length.
7. Every status change is recorded with who made it and when (`reservation_status_changes`).

## 2. Architectural pattern: Layered architecture with MVC

### Selection

LabLink is a **single deployable application (a "modular monolith") organized into four layers**, with the web layer following the **Model–View–Controller (MVC)** pattern:

| Layer | Responsibility | LabLink packages |
|---|---|---|
| Presentation (View + Controller) | Receive HTTP requests, validate form input, choose a page to render | `web`, `web.form`, `templates/` |
| Business (Service) | Enforce the business rules above; coordinate transactions | `service` |
| Persistence (Repository) | Read and write entities; no business logic | `repository` |
| Domain (Model) | Entities that map to database tables | `model` |
| Cross-cutting | Authentication and authorization applied to every request | `security`, `config` |

Each layer may only call the layer directly below it: controllers call services, services call repositories, repositories talk to the database. Controllers never touch repositories for reservations, and templates never contain business logic.

### Justification

- **Fits the problem size.** LabLink has four entities and three roles. A layered monolith gives clear structure without the overhead of distributed services.
- **Fits the team.** Every team member is new to building applications. Layers give each person an obvious place to put code and map directly onto the team roles: authentication (`security`, `AuthController`), reservations (`ReservationService`), front end (`templates/`), and database/inventory (`repository`, `InventoryService`, `db/migration`).
- **Fits the framework.** Spring Boot is built around this pattern (`@Controller`, `@Service`, `@Repository`), so we follow the framework instead of fighting it.
- **Testability.** Business rules live in one class (`ReservationService`) and can be tested against a real in-memory database without a browser.
- **One deployment.** A single JAR is easy to run locally and deploy for the final demo.

### Alternatives considered

| Alternative | Why not chosen |
|---|---|
| Microservices (separate auth, inventory, reservation services) | Network calls, multiple deployments and distributed data are far more work than the course timeline allows and add no value at this scale. |
| REST API + single-page JavaScript front end (React) | Requires two codebases and two skill sets; server-rendered Thymeleaf lets the whole team work in Java. A REST endpoint can still be added later for the API/advanced-feature requirement. |
| Unstructured "controllers do everything" | Fast at first, but business rules end up duplicated across pages and are hard to test. |

See ADR-0001 for the recorded decision.

## 3. Component diagram

![LabLink component diagram](diagrams/component.svg)

Source: [`diagrams/component.dot`](diagrams/component.dot) (Graphviz). Every controller also renders a Thymeleaf template from `src/main/resources/templates`.

**Which component calls which**

| Controller | Services it uses |
|---|---|
| AuthController, AccountController, AdminController | UserService |
| HomeController | UserService, ReservationService |
| LabController, EquipmentController | InventoryService |
| AvailabilityController | AvailabilityService |
| ReservationController | ReservationService, InventoryService, UserService |
| AvailabilityApiController (REST) | AvailabilityService, InventoryService |

| Service | Repositories it uses |
|---|---|
| UserService | UserRepository (also used by LabLinkUserDetailsService at login) |
| InventoryService | LabRepository, EquipmentRepository, DepartmentRepository, EquipmentCategoryRepository |
| ReservationService | ReservationRepository, LabRepository, EquipmentRepository, ReservationStatusChangeRepository |
| AvailabilityService | LabRepository, ReservationRepository |

**How a request flows:** the browser sends a request → the security filter chain checks that the user is logged in and has the right role → a controller validates the form and calls a service → the service applies the business rules and uses repositories to read/write data inside a transaction → the controller picks a Thymeleaf template, which renders HTML back to the browser.

## 4. Technology stack

| Concern | Technology | Version | Why |
|---|---|---|---|
| Language | Java | 17 (LTS) | Taught in our courses; every team member already knows it |
| Application framework | Spring Boot | 3.5 | Industry standard; auto-configuration removes most setup work |
| Web / MVC | Spring MVC | (via Boot) | Controllers, form binding, validation |
| Views | Thymeleaf + Spring Security extras | (via Boot) | Server-rendered HTML in plain Java; show/hide by role |
| Security | Spring Security, BCrypt | (via Boot) | Form login, sessions, role-based access, CSRF protection, password hashing |
| Persistence | Spring Data JPA / Hibernate | (via Boot) | Repositories generate queries from method names |
| Validation | Jakarta Bean Validation | (via Boot) | `@NotBlank`, `@Email`, etc. on forms |
| Database (dev/test) | H2, in memory, MySQL mode | 2.x | Zero install; fresh data on every run |
| Database (production) | MySQL | 8.4 | Free, widely hosted, supports CHECK constraints |
| Schema management | Flyway migrations (`db/migration`) | — | Versioned, repeatable schema setup; Hibernate validates against it (ADR-0005) |
| Build | Maven | 3.9 | Standard for Spring; dependency management |
| Testing | JUnit 5, Spring Boot Test, MockMvc, AssertJ | (via Boot) | Service tests against H2; full HTTP tests through security |
| Code quality | Checkstyle, JaCoCo, SonarQube (Week 12) | — | Lint on every PR; coverage gate on the service layer |
| CI | GitHub Actions | — | Lint, build + all tests, Docker smoke test on every push and pull request |
| Containers | Docker, Docker Compose, Caddy | — | Same image locally and in production; Caddy terminates HTTPS |
| Version control | Git + GitHub, feature-branch workflow | — | Reviewed pull requests into `main` |
| Hosting | Linux server with Docker: app container + MySQL container behind Caddy | — | See §7 and [deployment.md](deployment.md) |

## 5. Security design

- Passwords are hashed with BCrypt; plain-text passwords are never stored or logged.
- Self-registration always creates a **STUDENT** account. Faculty and lab manager accounts are created by an administrator (seeded in development), so users cannot grant themselves privileges.
- URL rules in `SecurityConfig` restrict inventory changes and request review to `ROLE_LAB_MANAGER`. The service layer re-checks the role for approve/reject and ownership for cancel, so a rule cannot be bypassed by calling a URL directly.
- All state-changing actions are HTTP POST requests protected by Spring Security's CSRF token, which Thymeleaf adds to every form automatically.
- Database credentials for production come from environment variables, not from the repository.

## 6. Implementation status (v1.0.0)

Everything planned for the course is implemented and covered by automated tests:

| Requirement | Where |
|---|---|
| Front end | Thymeleaf pages, responsive layout (backlog 3.7) |
| Database | 8 entities, Flyway migrations V1–V4, seed data ([database-design.md](database-design.md)) |
| Authentication | Spring Security form login, BCrypt, three roles, admin account management |
| CRUD on 2+ entities | Labs, equipment, reservations, users |
| Advanced feature | Public REST availability API ([api.md](api.md)) and lab availability week view |

The Milestone 2 prototype had 18 tests; v1.0.0 has **176** (unit, integration and 13 end-to-end system
tests), with 85.9 % line coverage. See [system-test-report.md](system-test-report.md).

Not in v1.0.0 (see the release notes): email notifications, self-service password reset, recurring reservations.

## 7. Deployment view

```
 Internet ──443──▶ Caddy (HTTPS, Let's Encrypt) ──8080──▶ lablink app container (Spring Boot JAR, Java 17)
                    blocks /actuator/* except health          │ JDBC (private Docker network)
                                                               ▼
                                                     MySQL 8.4 container ── db-data volume, nightly backup.sh
```

The same Docker image runs locally (`docker compose up`), in CI (smoke test) and in production
(`docker-compose.prod.yml`). Configuration comes only from environment variables. Details:
[containerization.md](containerization.md), [deployment.md](deployment.md), [monitoring.md](monitoring.md).

## 8. Testing strategy

| Level | What | Tools |
|---|---|---|
| Unit | Business rules in services (conflicts, approval, loan limits, reschedule) | JUnit 5, Mockito |
| Integration | Controllers through security to H2; repositories and migrations | Spring Boot Test, MockMvc |
| System (end-to-end) | 13 user workflows against the running app over HTTP | `EndToEndSystemTest` (real port, real login) |
| Smoke | Built Docker image + MySQL: health, login page, API | GitHub Actions + Docker Compose |
