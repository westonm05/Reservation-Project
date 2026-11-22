# LabLink Anticipated Maintenance

CS450 Week 14. Software maintenance is usually split into four types (ISO/IEC 14764):

| Type | Question it answers | Trigger |
|---|---|---|
| **Corrective** | "What's broken?" | A defect: the software does something wrong |
| **Adaptive** | "What changed around us?" | The environment changes: platform versions, hosting, campus systems |
| **Perfective** | "What would make it better?" | New or improved features, usability, performance users ask for |
| **Preventive** | "What will break later if we do nothing?" | Risks found before they turn into failures |

Every example below comes from LabLink's actual code, design decisions (ADRs) or what we saw while
containerizing it this week, not a generic list.

## Corrective

### C1. Reservation times off by five hours in containers — *found and fixed this week*

- **Problem.** `ReservationService` rejects reservations that start in the past by comparing with
  `LocalDateTime.now()`. That uses the server's time zone, and Docker containers default to UTC. On a
  container, "now" was five hours ahead of Kentucky, so a student at 11 a.m. could not book a lab for 3 p.m.
  Timestamps like `created_at` and `reviewed_at` were also wrong.
- **Fix (done).** Set `TZ=America/New_York` in the Dockerfile and both compose services and pass
  `-Duser.timezone` to Java. The CI smoke test's logs now show `-05:00` timestamps.
- **Follow-up.** The lasting fix is to inject a `java.time.Clock` into the services instead of calling
  `LocalDateTime.now()` directly, so tests can pin the time and the zone is set in one place.

### C2. Two simultaneous requests can double-book a lab

- **Problem.** ADR-0004 accepted a known race: the overlap check and the insert are separate statements, so
  two requests for the same lab and time arriving within milliseconds can both pass the check. It is rare at
  campus scale but most likely exactly when it hurts: the first minutes after a popular lab's schedule opens.
- **Fix.** In `requestLab` / `requestEquipment` / `reschedule`, lock the lab or equipment row first
  (`@Lock(PESSIMISTIC_WRITE)` repository method, i.e. `SELECT ... FOR UPDATE`) so the second request waits and
  then sees the first booking. Add an integration test that fires two requests from two threads against MySQL.
- **Detection.** The runbook row "Two reservations for the same lab and time" in [monitoring.md](monitoring.md).

## Adaptive

### A1. Upgrade Flyway for MySQL 8.4 — *seen in this week's container logs*

- **Change in environment.** We run MySQL **8.4** (the current long-term-support release) in Docker. On startup
  Flyway logs: *"MySQL 8.4 is newer than this version of Flyway and support has not been tested. The latest
  supported version of MySQL is 8.1."* Migrations work today, but a future MySQL change could break them with
  no warning from our tests, which run on H2.
- **Work.** Override the Flyway version in `pom.xml` (or move to the next Spring Boot release that ships a newer
  Flyway), confirm the warning is gone in the Docker Smoke Test log, and add a MySQL-based integration test
  (Testcontainers) so migrations are tested against the real production database on every PR.

### A2. Sign in with campus accounts (single sign-on)

- **Change in environment.** LabLink keeps its own passwords. If Campbellsville University adopts LabLink for
  real, IT will require sign-in through the university's identity provider (e.g. Microsoft Entra ID, which
  student email already uses), and accounts should disappear when someone leaves the university.
- **Work.** Add `spring-boot-starter-oauth2-client` and configure the campus provider in `SecurityConfig`;
  map university groups (faculty, staff, lab managers) to LabLink's `Role`; keep local login only for a
  break-glass admin. Registration (`/register`) goes away because accounts come from the university.

## Perfective

### PF1. Email notifications

- **Why.** Today a student only learns their request was approved or rejected by logging in and checking
  *My Reservations*. Lab managers don't know a request is waiting unless they look. This was listed as a
  possible feature in the architecture document and is the most obvious usability gap.
- **Work.** `spring-boot-starter-mail`; send an email from `ReservationService` when a reservation changes
  status (the `reservation.status` log line already marks every such point), a daily digest of pending
  requests to lab managers, and a reminder the day before a reservation. Send asynchronously so a slow mail
  server never slows the page.

### PF2. Recurring reservations and calendar export

- **Why.** Faculty teach the same lab section every week, so today they submit the same request 15 times a
  semester. Users also want their bookings in Outlook or Google Calendar.
- **Work.** A "repeat weekly until" option on the reservation form that creates the series in one transaction
  and reports which dates conflict; an `.ics` export of *My Reservations* (and per-lab feeds through the
  existing public API at `/api/labs/{id}/availability`).

## Preventive

### PV1. Rate-limit logins and add automated security scanning

- **Risk.** Nothing currently slows down password guessing on `/login`, and dependencies (Spring, MySQL
  driver, the base Docker images) get security fixes constantly; nobody is watching for them.
- **Work.**
  - Lock an account for 15 minutes after 10 failed logins (the `auth.login.failure` events built this week
    already count them), or rate-limit `/login` per IP at Caddy.
  - Turn on **Dependabot** for Maven, GitHub Actions and Docker base images, so upgrade PRs appear by
    themselves and run through CI.
  - Add an image vulnerability scan (e.g. Trivy) to the Docker Smoke Test job that fails on critical issues.

### PV2. Prove backups restore, and test against the real database

- **Risk.** Two things could fail silently until the day they're needed: backups that can't actually be
  restored, and SQL that works on H2 (where all 163 tests run) but not on MySQL. The seed data script
  already had to be written carefully for both.
- **Work.**
  - Automate the monthly restore test from the deployment guide (§8) as a scheduled job that restores the
    newest backup into a scratch database and checks row counts, alerting if it fails.
  - Run the repository and service integration tests against MySQL with Testcontainers in CI (shared with A1).
  - Raise JaCoCo coverage beyond the service layer (currently enforced at 70 % only for `service`).

## Summary

| # | Type | Item | Priority | Effort | Status |
|---|---|---|---|---|---|
| C1 | Corrective | Container time zone made reservation times wrong | High | S | ✅ Fixed (Week 14) |
| C2 | Corrective | Race condition can double-book (ADR-0004) | High | M | Backlog |
| A1 | Adaptive | Flyway version behind MySQL 8.4 | Medium | S | Backlog |
| A2 | Adaptive | Campus single sign-on | Low (needed only for real adoption) | L | Backlog |
| PF1 | Perfective | Email notifications | High | M | Backlog |
| PF2 | Perfective | Recurring reservations, calendar export | Medium | M | Backlog |
| PV1 | Preventive | Login rate limiting, Dependabot, image scanning | High | S–M | Backlog |
| PV2 | Preventive | Automated restore tests, MySQL integration tests | Medium | M | Backlog |

Effort: S = under a day, M = a few days, L = a sprint or more.
