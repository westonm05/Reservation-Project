# LabLink System Test Report (Week 15)

CS450 Software Engineering · Final integration · Release v1.0.0

## 1. Summary

| | Result |
|---|---|
| End-to-end scenarios executed | **13** (assignment minimum: 10) |
| Passed / failed | **13 / 0** on the final run |
| Full automated suite on the release candidate | **176 tests, 0 failures, 0 errors, 0 skipped** |
| Line / branch coverage (JaCoCo) | **85.9 % / 77.9 %** (up from 80.7 % / 73.5 % before this week) |
| CI checks | Lint ✔ · Build & Test ✔ · Docker Smoke Test ✔ |
| Defects found | 1 (low severity, logged as a known issue, see §5) |
| Final run | GitHub Actions run [45056687216](https://github.com/westonm05/Reservation-Project/actions/runs/45056687216), commit `cccef1b`, Nov 22, 2026 |

## 2. Scope and approach

**What a "system test" means here.** The scenarios live in
[`EndToEndSystemTest`](../src/test/java/edu/campbellsville/lablink/e2e/EndToEndSystemTest.java). Each test
starts the **whole application on a real HTTP port**: embedded Tomcat, the Spring Security filter chain,
every controller and service, Flyway migrations, and a fresh H2 database with the demo seed data. It then
drives the app **from the outside, over HTTP, the way a browser does**:

- loads the page that holds a form and reads its CSRF token,
- submits the form fields a user would type,
- keeps the session cookie between requests (so login really happens through the login page),
- follows redirects and checks the HTML or JSON the user would see.

Nothing is mocked. The repositories are only used to look up ids and double-check what ended up in the
database. This differs from the existing integration tests, which call controllers through MockMvc and
log users in with `@WithMockUser`.

**Isolation.** The suite gets its own in-memory database (`lablink-e2e`). Every scenario creates its own lab
or user and uses its own dates, so scenarios are independent and order doesn't matter.

**Environment.** GitHub Actions `ubuntu-latest`, Temurin Java 17, `mvn -B verify`. The same suite runs on every
push and pull request from now on.

**Coverage of major workflows.**

| Workflow (backlog) | Scenarios |
|---|---|
| Registration, login, logout, password change (authentication) | E2E-01, 02, 12 |
| Lab inventory CRUD and search (3.8) | E2E-03 |
| Reservation request → approve / reject (core workflow) | E2E-04, 05 |
| Double-booking prevention, faculty auto-approval | E2E-06 |
| Reschedule (3.3) and cancel | E2E-07 |
| Equipment reservations and loan limits | E2E-08 |
| Availability week view (3.2) and public REST API (3.6, advanced feature) | E2E-09, 13 |
| Role-based access control | E2E-10 |
| Admin account management (3.4), account page (3.5) | E2E-11, 12 |
| Operations: health check | E2E-13 |

## 3. Scenarios and results

| ID | Scenario | Steps (as the user) | Expected result | Result |
|---|---|---|---|---|
| E2E-01 | New student registers, logs in and sees their dashboard | Open **Register**, submit name/email/password ×2 → log in with the new account | Redirect to login with *"Account created. Please log in."*; after login the Dashboard shows *"Welcome, Erin Example"* and role STUDENT | **PASS** |
| E2E-02 | Wrong password is refused and protected pages require login | Visit **My reservations** logged out → log in with a wrong password → try **My reservations** again | Sent to the login page; login returns `/login?error`; still no access | **PASS** |
| E2E-03 | Lab manager adds, searches, edits and deletes a lab | Manager: **Add lab** → search *"Chemistry Annex"* → **Edit** capacity 12→40 → **Delete** | *"Lab saved."*; lab appears in search; capacity stored as 40; *"Lab deleted."* and row gone | **PASS** |
| E2E-04 | Student request is approved | Student: **Reserve** a lab → manager: **Manage requests** → **Approve** → student: **My reservations** | Student sees PENDING; request listed for manager; *"Reservation approved."*; student sees APPROVED (DB status APPROVED) | **PASS** |
| E2E-05 | Student request is rejected | Student requests a lab → manager **Reject** | *"Reservation rejected."*; student sees REJECTED | **PASS** |
| E2E-06 | Faculty auto-approval and double-booking prevention | Faculty books 1–3 PM → student requests 2–4 PM in the same lab | Faculty booking APPROVED immediately; student's form re-shown with *"... is already booked during that time"*; reservation count unchanged | **PASS** |
| E2E-07 | Reschedule then cancel | Student requests a lab → **Reschedule** to the next day 3 PM → **Cancel** | *"Reservation rescheduled."* and new start stored; *"Reservation cancelled."*, status CANCELLED | **PASS** |
| E2E-08 | Equipment loan limit | Student reserves *3D Printer 2* for 9 h, then for 4 h | 9 h refused with *"... at most 8 hours at a time"*; 4 h accepted (*"Reservation submitted."*) | **PASS** |
| E2E-09 | Availability week view and public API | Faculty books a lab → open its **Availability** page → anonymous `GET /api/labs/{id}/availability` and `GET /api/labs` | Week view lists the APPROVED booking; API (no login) returns 200 JSON with the lab name, status APPROVED and the booked start time; lab listed by `/api/labs` | **PASS** |
| E2E-10 | Students blocked from manager-only pages | Student opens `/labs/new`, `/equipment/new`, `/reservations/manage`, `/admin/users`, then `/labs` | 403 for each manager page; 200 for the Labs list | **PASS** |
| E2E-11 | Admin creates an account and changes a role | Manager: **Users → Add account** (faculty) → new user logs in → manager changes role to Lab Manager | *"Account created for dr.e2e@lablink.edu."*; new user's Dashboard shows FACULTY; *"Role updated."*, DB role LAB_MANAGER | **PASS** |
| E2E-12 | Password change, logout, old password rejected | Register → log in → **Account → Change password** → **Log out** → log in with old, then new password | *"Password changed."*; logout lands on login page and `/account` is protected again; old password → `/login?error`; new password → Dashboard | **PASS** |
| E2E-13 | Health check and API error handling | Anonymous `GET /actuator/health`, `GET /api/labs/999999/availability`, `GET /api/labs/1/availability?from=next-tuesday` | 200 `{"status":"UP"}`; 404 JSON error; 400 JSON error telling the caller to use ISO format | **PASS** |

## 4. Run history

| Run | Commit | Result | Notes |
|---|---|---|---|
| 1 | `2c28ebb` | 9 passed, 4 failed | E2E-05, 06, 07, 09 failed in setup: the helper gave every test lab the same building and room number, which the database rejects (`uq_labs_location`). This was a **test-data error**, not an application failure, but it exposed defect D-01 below. |
| 2 | `cccef1b` | **13 passed, 0 failed** | Each test lab now gets its own room number. Full suite 176/176. |

## 5. Defects found

| ID | Severity | Description | Status |
|---|---|---|---|
| D-01 | Low | When a lab is saved with a **building + room number that already exists**, the form says *"A lab with that name already exists"*, even though the name is unique. `LabController.save` reports every `DataIntegrityViolationException` as a duplicate name. The data stays correct (nothing is saved); only the message is misleading. | Open. Listed under *Known issues* in the v1.0.0 release notes; fix planned for v1.0.1 (check the location before saving and show the error on the room-number field). |

## 6. How to re-run

```bash
mvn test -Dtest=EndToEndSystemTest     # just the 13 scenarios (~15 s)
mvn verify                             # everything: lint, 176 tests, coverage
```

Each scenario prints a line such as `E2E-RESULT | PASS | E2E-04 ...` to the console and the CI log.
