# Sprint 3 Progress Tracking (Milestone 3)

Sprint window: **October 19 – November 15, 2026** (Milestone 3 due Nov 15).
Scope: the 10 backlog items in [project-plan.md §4](project-plan.md#4-milestone-3-work-breakdown), **45 estimated hours**.
Data as of **November 11, 2026**.

![Sprint 3 burndown](diagrams/sprint3-burndown.png)

## How the chart is built

- The scope is the sum of the hour estimates in the project plan (45 h).
- A backlog item counts as burned on the date of the commit that finished it (from `git log`).
  Partial work is not counted.
- The ideal line burns the 45 h evenly over the 28-day sprint.

## Backlog status

| # | Task | Owner (plan) | Estimate | Status | Completed | Evidence |
|---|---|---|---|---|---|---|
| 3.1 | MySQL + Flyway migrations | PJ | 4 h | ✅ Done | Nov 3 | `feature/week11-database` (Flyway V1–V4, 8-table schema) |
| 3.2 | Reservation calendar/availability view | Isaiah | 6 h | ⬜ Not started | — | — |
| 3.3 | Reschedule a pending reservation | Isaiah | 3 h | ✅ Done | Oct 19 | PR #2 (TDD: RED → GREEN → REFACTOR commits) |
| 3.4 | Admin page for accounts and roles | Weston | 4 h | ⬜ Not started | — | — |
| 3.5 | Password change and account page | Weston | 3 h | ✅ Done | Oct 28 | `feature/account-password-change` |
| 3.6 | Advanced feature (email notifications or REST API) | Weston + Isaiah | 8 h | ⬜ Not started | — | — |
| 3.7 | UI redesign | Dawson | 8 h | ⬜ Not started | — | — |
| 3.8 | Search/filter labs and equipment | Dawson | 3 h | ⬜ Not started | — | — |
| 3.9 | Tests for inventory CRUD and new features | PJ | 4 h | ✅ Done | Nov 3 | Mockito unit tests, `DatabaseMigrationIntegrationTest` |
| 3.10 | Update docs/diagrams | All | 2 h | ✅ Done | Nov 11 | `docs/uml.md`, this file |

**Burned: 16 h of 45 h (36%) — 5 of 10 items done. Remaining: 29 h.**

## Burndown data

| Date | Item(s) finished | Hours burned | Remaining |
|---|---|---|---|
| Oct 19 | Sprint start | — | 45 |
| Oct 19 | 3.3 | 3 | 42 |
| Oct 28 | 3.5 | 3 | 39 |
| Nov 3 | 3.1, 3.9 | 8 | 31 |
| Nov 11 | 3.10 | 2 | 29 |

## Unplanned work done in this sprint

These were not in the Sprint 3 backlog, so they are not in the burndown, but they took real effort:

| Work | Date | Where |
|---|---|---|
| JaCoCo coverage reporting and CI test/coverage summaries | Oct 19 | PR #2 |
| Checkstyle lint job in CI | Oct 28 | PR #3 |
| Team Git workflow doc, PR template, `protect-main` ruleset | Oct 28 – Nov 1 | PR #4 |
| Fixes for 10 SonarQube issues | Nov 11 | PR #6 |

## Tests at the end of Sprint 3

Counted with all Sprint 3 pull requests merged (102 test methods; `@Nested` classes and parameterized cases can make the CI count higher).

| Kind | Count | Files |
|---|---|---|
| Unit (Mockito, no Spring context) | 56 | `InventoryServiceTest`, `ReservationServiceTest`, `UserServiceTest` |
| Integration (Spring context + H2) | 46 | `ReservationRepositoryIntegrationTest`, `ReservationServiceIntegrationTest`, `ReservationWebIntegrationTest`, `SecurityAndRegistrationIntegrationTest`, `DatabaseMigrationIntegrationTest`, `AccountIntegrationTest` |

Coverage: every CI run uploads the JaCoCo HTML report as the `jacoco-coverage-report` artifact and
posts per-package coverage as annotations on the run page.

## Carried over to Milestone 4

3.2, 3.4, 3.6, 3.7, 3.8 (29 h). 3.6 (the advanced feature) is a course requirement and depends on 3.2.
