# LabLink Project Plan

Last updated: November 11, 2026 (Milestone 3)

## 1. Team and roles

| Member | Role | Owns |
|---|---|---|
| Weston Mattingly | Project Coordinator, Authentication | Planning, communication, registration/login/security |
| Isaiah Bain | Reservations, Code Review Lead | Reservation workflow, pull-request reviews |
| Dawson Brown | Front End, UI/UX Lead | Thymeleaf templates, styling, usability |
| PJ | Database, Inventory, QA/DevOps | Schema, lab/equipment CRUD, tests, CI, deployment |

## 2. Milestones

| Milestone | Focus | Due | Status |
|---|---|---|---|
| 1 | Proposal, project charter, repository setup | Sept 2026 | ✅ Complete |
| 2 | Design & architecture + working prototype | Oct 18, 2026 | ✅ Complete (this submission) |
| 3 | Core features complete, MySQL integration, API/advanced feature | Nov 15, 2026 | ✅ Complete — see [sprint3-progress.md](sprint3-progress.md) |
| 4 | Polish, testing, deployment, final demo | Target: early Dec 2026 | ⏳ Not started |

(Milestone 3 and 4 dates are targets; adjust to the syllabus dates.)

## 3. Milestone 2 progress

| Deliverable | Location | Status | Completed by |
|---|---|---|---|
| Architecture document (pattern, component diagram, tech stack) | `docs/architecture.md` | ✅ Done | Weston |
| UML: use case, class, 3 sequence, activity | `docs/uml.md` | ✅ Done | Weston |
| ER diagram, 3NF schema, DDL | `docs/database-design.md`, `src/main/resources/db/migration/` | ✅ Done | Weston |
| ADRs (4) | `docs/adr/` | ✅ Done | Weston |
| Working prototype | `src/` | ✅ Done | Weston |
| CI pipeline (GitHub Actions) | `.github/workflows/ci.yml` | ✅ Done | Weston |
| Updated project plan | this file | ✅ Done | Weston |

**Prototype features working now:** registration and login with three roles; lab and equipment CRUD for lab managers; reservation requests with double-booking prevention; faculty auto-approval; lab-manager approve/reject; owner cancellation; 18 automated tests.

## 4. Milestone 3 work breakdown

| # | Task | Owner | Estimate | Depends on |
|---|---|---|---|---|
| 3.1 | Run app against real MySQL; add Flyway migrations (ADR-0003 follow-up) | PJ | 4 h | — |
| 3.2 | Reservation calendar/availability view per lab | Isaiah | 6 h | — |
| 3.3 | Edit/reschedule a pending reservation | Isaiah | 3 h | — |
| 3.4 | Admin page to create faculty/lab-manager accounts and change roles | Weston | 4 h | — |
| 3.5 | Password change and account page | Weston | 3 h | — |
| 3.6 | Advanced feature: public REST availability API (chosen over email notifications; see [api.md](api.md)) | Weston + Isaiah | 8 h | 3.2 |
| 3.7 | UI redesign: layout, navigation, mobile-friendly tables, form feedback | Dawson | 8 h | — |
| 3.8 | Search/filter labs and equipment | Dawson | 3 h | — |
| 3.9 | Tests for inventory CRUD and new features; keep CI green | PJ | 4 h | ongoing |
| 3.10 | Update docs/diagrams for new features | All | 2 h | all |

## 5. Milestone 4 outline

- Deploy to a cloud host with managed MySQL; environment-variable configuration
- End-to-end test pass with demo script for each role
- Bug fixing, accessibility check, final documentation and user guide
- Final presentation and demo

## 6. Process

- **Branching:** `feature/<name>` branches → pull request → review requested (Isaiah, code review lead) → squash merge to `main`. No direct commits to `main`. Since Nov 11, 2026 an approval is no longer required to merge (see [git-workflow.md §4](git-workflow.md#4-merge-policy)).
- **CI:** every push and pull request runs `mvn verify` (tests + Checkstyle); a failing build blocks merging.
- **Communication:** weekly check-in coordinated by Weston; tasks tracked as GitHub Issues assigned to owners above.
- **Definition of done:** code merged through a reviewed PR, tests added and passing, docs updated if behavior changed.

## 7. Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Uneven contribution across team members | High (occurred in M2 and M3) | High | Assign M3 tasks as GitHub Issues with owners and dates; review progress at weekly check-ins; report through peer evaluations |
| Team new to Spring Boot | High | Medium | Commented prototype code as a reference; pair on first feature each |
| H2 vs MySQL differences | Medium | Medium | Flyway migrations are plain SQL and CI runs H2 in MySQL mode; run against a real MySQL database before the M4 deployment |
| Deployment issues close to the final demo | Medium | High | Deploy a first version at the start of M4, not the end |
| Double-booking race condition | Low | Low | Acceptable at campus scale; row locking documented in ADR-0004 |
