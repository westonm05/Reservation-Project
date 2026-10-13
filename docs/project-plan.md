# LabLink — Project Plan (updated for Milestone 2)

* **Last updated:** October 13, 2026
* **Project coordinator:** Weston Mattingly
* **Repository:** https://github.com/westonm05/Reservation-Project

> Milestone due dates below marked *(per syllabus)* should be confirmed against the
> course schedule and filled in.

---

## 1. Team and responsibilities

| Member | Role | Owns in code | Owns in docs |
|---|---|---|---|
| **Weston Mattingly** | Project Coordinator, Authentication | `SecurityConfig`, `LabLinkUserDetailsService`, `UserService`, `AuthController`, `RegistrationForm`, project skeleton (`pom.xml`, app config) | Architecture document, ADR-0001/0004, project plan |
| **Isaiah Bain** | Reservations, Code Review Lead | `ReservationService`, `ReservationController`, `ApprovalController`, `ReservationForm`, reservation tests | Sequence diagrams, use case diagram, ADR-0005 |
| **Dawson Brown** | Front End, UI/UX Lead | All Thymeleaf templates, `lablink.css`, `HomeController`, `GlobalExceptionHandler` | Activity diagram, ADR-0002 |
| **PJ** | Database, Inventory, QA/DevOps | Entities, repositories, `schema.sql`, `data.sql`, `EquipmentService`, `EquipmentController`, `InventoryController`, CI workflow, security tests | ER diagram, database design, class diagram, ADR-0003 |

Shared rules: work on `feature/<name>` branches, open a pull request, at least one
approving review (Isaiah reviews by default), CI must be green before merging to `main`.

---

## 2. Milestone overview

| # | Milestone | Key deliverables | Status |
|---|---|---|---|
| 1 | Project proposal & setup | Concept, requirements, team roles, repository + README, branching strategy | ✅ Complete |
| 2 | **Design & architecture** | Architecture doc, UML, database design + DDL, ADRs, working prototype, updated plan | ✅ **Complete — this submission** |
| 3 | Core implementation *(per syllabus)* | Feature-complete CRUD, advanced feature hardened, API integration, test coverage | ⏳ Next |
| 4 | Final release & demo *(per syllabus)* | Deployed app on MySQL, polished UI, final docs, presentation | ⏳ Planned |

---

## 3. Milestone 2 — what was delivered

| Deliverable | Location | Owner(s) |
|---|---|---|
| Architecture document (pattern + justification, component diagram, tech stack) | `docs/architecture.md` | Weston |
| Use case diagram | `docs/uml.md` §1, `docs/diagrams/use-case.*` | Isaiah |
| Class diagram | `docs/uml.md` §2, `docs/diagrams/class.*` | PJ |
| Sequence diagrams (3: request, approve, sign in) | `docs/uml.md` §3 | Isaiah, Weston |
| Activity diagram | `docs/uml.md` §4 | Dawson |
| ER diagram, 3NF analysis | `docs/database-design.md` | PJ |
| DDL scripts | `src/main/resources/schema.sql`, `data.sql` | PJ |
| ADRs (5) | `docs/adr/` | All |
| Working prototype | `src/` — see feature list below | All |
| CI pipeline | `.github/workflows/ci.yml` | PJ |
| Updated project plan | `docs/project-plan.md` | Weston |

### Prototype — features working end to end

| Feature | Requirement covered |
|---|---|
| Register (student/faculty), sign in, sign out; BCrypt passwords | Authentication |
| Role-based access: `/manager/**` for lab managers only; role-aware menus | RBAC |
| Browse/search/filter equipment; view item detail; list labs | Front end, Read |
| Request equipment or whole-lab reservation with validation | Create |
| Double-booking prevention, 8-hour limit, maintenance block | Advanced feature |
| View and cancel own reservations | Read, Update (status) |
| Lab manager approval queue: approve/reject with reviewer recorded | Update, workflow |
| Lab manager add/edit equipment, set maintenance/retired | Equipment CRUD |
| Automated tests: 11 reservation-rule tests, 8 security/registration tests | Quality |

---

## 4. Milestone 3 — plan

| Task | Owner | Est. effort | Depends on |
|---|---|---|---|
| Lock equipment/lab row during booking to remove the race condition (ADR-0005) | Isaiah | M | — |
| Lab booking blocks the items inside that lab (and vice versa) | Isaiah | M | — |
| Weekly availability calendar view per item | Dawson + Isaiah | L | — |
| **API integration:** email notifications on approve/reject (e.g. SendGrid/Mailgun API) | Weston | M | Account setup |
| Password reset flow; admin page to create lab-manager accounts / change roles | Weston | M | — |
| Lab CRUD (add/edit labs) and category management for managers | PJ | S | — |
| "Retire" instead of delete for equipment with history; equipment delete when unused | PJ | S | — |
| Run full app against MySQL locally; document setup | PJ | S | — |
| Responsive polish, accessibility pass (labels, contrast, keyboard) | Dawson | M | — |
| Raise test coverage: controller tests for each page, inventory service tests | All (each own area) | M | — |

S = under 3 hrs, M = 3–8 hrs, L = over 8 hrs.

## 5. Milestone 4 — plan

| Task | Owner |
|---|---|
| Deploy to cloud host with managed MySQL; environment-variable configuration | PJ |
| Seed realistic campus data for the demo | PJ |
| Usage report for managers (most-booked items, pending age) | Isaiah |
| Final UI polish and user guide | Dawson |
| Final documentation pass, update ADRs, presentation and demo script | Weston (coordinates), all |

---

## 6. Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Uneven commit history across members (graded) | Medium | High | Each member commits their own slice from feature branches; work split by ownership table above |
| H2 vs MySQL behavior differences | Medium | Medium | Test against MySQL before each milestone demo (M3 task) |
| Team members new to Spring | High | Medium | Prototype provides a working pattern to copy; pair on first tasks; comments explain framework "magic" |
| Concurrent double booking | Low | Medium | Pessimistic lock in M3 |
| Scope creep (calendar, notifications) | Medium | Medium | Prioritize required features first; calendar is "nice to have" |
| Merge conflicts in shared classes (`User`, `Reservation`) | Medium | Low | Entity changes go through PJ; small, frequent PRs |

---

## 7. Communication

* Weekly team check-in (coordinated by Weston); status posted in the team channel.
* GitHub pull requests are the record of code review.
* Blockers raised in the channel the same day; Weston rebalances tasks if someone is stuck.
