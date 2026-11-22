# LabLink Release Notes

Versions follow [Semantic Versioning 2.0.0](https://semver.org/): **MAJOR.MINOR.PATCH**. MAJOR changes break
existing users or data, MINOR adds features in a backward-compatible way, and PATCH fixes bugs.

---

## v1.0.0 — December 2026

**First production release** of LabLink, the lab and equipment reservation system for Campbellsville
University (CS450 Software Engineering team project). Version 1.0.0 means the feature set planned for
the course is complete, tested, documented, and deployable, and that later 1.x releases will keep the
URLs, the public API and the database schema backward compatible.

Git tag: `v1.0.0` · Build: `lablink-1.0.0.jar` / Docker image built from the tag.

### Features

**Accounts and security**
- Student self-registration; login and logout with Spring Security; BCrypt password hashing
- Three roles (student, faculty, lab manager) with role-based page access and CSRF protection on every form
- Account page with password change (backlog 3.5)
- Admin **Users** page: lab managers create faculty and lab-manager accounts and change roles (3.4)
- First lab-manager account can be created from environment variables on an empty production database

**Inventory**
- Create, edit and delete labs and equipment (lab managers)
- Departments and equipment categories; equipment status (available, maintenance, retired)
- Search and filter labs and equipment by text, department, category and status (3.8)

**Reservations**
- Request a lab or a piece of equipment; **double-booking prevention** against pending and approved bookings
- Faculty and lab-manager requests are approved automatically; lab managers approve or reject student requests
- Reschedule pending requests (3.3); cancel pending or approved reservations
- Equipment loan-length limits per category (for example 8 h for 3D printers)
- Full status history: who changed each reservation and when
- Dashboard with upcoming reservations and, for managers, the number of requests waiting

**Availability and integration**
- Week view of each lab's bookings (3.2)
- **Public read-only REST API**: `GET /api/labs`, `GET /api/labs/{id}/availability` with JSON errors (3.6, advanced feature)

**User interface**
- Refreshed responsive layout, navigation with current-page highlighting, accessible form feedback (3.7)

**Data, operations and delivery**
- MySQL 8.4 in production, H2 for development and tests; schema managed by Flyway migrations V1–V4 (8 entities) with demo seed data
- Docker image, Docker Compose for local and production use, Caddy HTTPS reverse proxy, nightly backup script
- Actuator health, liveness and readiness checks; metrics restricted to lab managers; structured security event logs
- GitHub Actions CI: Checkstyle lint, build with all tests and coverage gate, Docker smoke test

### Quality at release

- **176 automated tests, all passing**: unit, integration, and 13 end-to-end system tests that drive the running
  app over HTTP ([system test report](docs/system-test-report.md))
- **85.9 % line / 77.9 % branch coverage** (JaCoCo); 10 SonarQube issues resolved in Week 12
- Documentation: [README](README.md), [user guide](docs/user-guide.md), [architecture](docs/architecture.md),
  [API reference](docs/api.md), [deployment](docs/deployment.md), [monitoring](docs/monitoring.md)

### Known issues

| # | Issue | Workaround |
|---|---|---|
| 1 | Saving a lab whose **building + room number** already exists shows *"A lab with that name already exists"* instead of a room-number error (found by system testing, D-01). Nothing is saved, so data stays correct. | Check that the room isn't already used by another lab |
| 2 | **No self-service password reset.** Users who forget their password can't recover the account themselves. | A system administrator resets it in the database |
| 3 | **No notifications.** Students are not emailed when a request is approved or rejected. | Check **My reservations** or the Dashboard |
| 4 | The demo seed data uses fixed dates in fall 2026, so the sample bookings move into the past over time. | Seed data is for demos only and is never loaded in production |
| 5 | The availability API has no rate limiting. | Caddy in front of the app; add limits before wide public use |

### Future enhancements

- **v1.0.1 (patch):** correct the lab location error message (known issue 1)
- **v1.1 (minor):** email notifications for approvals, rejections and reminders; password reset by email link
- **Later:** recurring reservations (e.g. every Tuesday for a semester); check-in/check-out for equipment loans;
  calendar export (iCal) and campus single sign-on; usage reports for lab managers; API rate limiting and keys

### Upgrading

This is the first release, so there is nothing to upgrade from. Deploy it by following
[docs/deployment.md](docs/deployment.md). Flyway creates the schema on first start.
