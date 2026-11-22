# Final Presentation: Run-of-Show and Rehearsal Notes

**Limit: 15 minutes.** Plan for **13:30** so there is 1:30 of buffer for a slow demo, a question, or a
speaker handoff. Speakers below follow each person's project role in the README; swap them as the team decides.

## 1. Run-of-show (planned)

| # | Segment | Speaker | Planned | Cumulative | What to show / say |
|---|---|---|---|---|---|
| 1 | Problem and solution | Weston | 1:30 | 1:30 | Sign-up sheets and email threads → one place to see what's free, prevent double booking, keep history. Three roles. |
| 2 | Architecture | Weston | 1:30 | 3:00 | Component diagram ([architecture.md](architecture.md)): layered Spring Boot monolith, Thymeleaf, MySQL + Flyway, Docker + Caddy. One sentence on *why* a monolith (ADR-0001). |
| 3 | Live demo: student and faculty | Isaiah | 3:00 | 6:00 | Faculty books a lab 1–3 PM → APPROVED instantly. Student searches labs → **Availability** shows the booking → student requests 2–4 PM → refused with *"already booked"* → requests 3–4 PM → PENDING. |
| 4 | Live demo: lab manager | Dawson | 2:30 | 8:30 | Manager **Manage requests** → **Approve** → student sees APPROVED. **Add lab**. **Users → Add account**. Point out the responsive layout. |
| 5 | Advanced feature: REST API | PJ | 1:30 | 10:00 | `curl /api/labs` and `/api/labs/{id}/availability` in a terminal, with no login. Show the JSON 404/400 errors. |
| 6 | Quality and DevOps | PJ | 1:30 | 11:30 | GitHub Actions: Lint, Build & Test, Docker Smoke Test all green. 176 tests, 13 end-to-end, 85.9 % coverage. The system test that found defect D-01. |
| 7 | Process and lessons learned | Weston | 1:00 | 12:30 | GitHub Flow, Conventional Commits, milestones and releases (v1.0.0 tag). One honest lesson. |
| 8 | Known issues, future work, wrap-up | Weston | 1:00 | 13:30 | From [RELEASE_NOTES.md](../RELEASE_NOTES.md): notifications and password reset next. Thank the audience; invite questions. |
| | **Buffer** | | 1:30 | **15:00** | |

### Demo safety checklist (do before presenting)

- [ ] App running locally with demo data (`docker compose up --build`, or `mvn spring-boot:run`), opened in the browser **before** the talk starts
- [ ] Three browser windows or profiles already logged in: student, faculty, manager (saves ~45 s of typing)
- [ ] Demo dates chosen in advance: a weekday next week with no seed bookings in the lab you'll use
- [ ] Terminal open with the two `curl` commands typed in and ready
- [ ] GitHub Actions page and the `v1.0.0` release open in tabs
- [ ] **Backup:** screenshots or a 2-minute screen recording of the demo in case Wi-Fi or the app fails
- [ ] Slides exported to PDF on a USB drive / cloud

## 2. Rehearsal notes (fill in during the rehearsal)

**Rehearsal #1:** Date ________ · Location / call ________ · Present: ☐ Weston ☐ Isaiah ☐ Dawson ☐ PJ ·
Timekeeper ________

| # | Segment | Planned | **Actual** | Over / under | Notes (what went wrong, what to cut) |
|---|---|---|---|---|---|
| 1 | Problem and solution | 1:30 | | | |
| 2 | Architecture | 1:30 | | | |
| 3 | Demo: student and faculty | 3:00 | | | |
| 4 | Demo: lab manager | 2:30 | | | |
| 5 | REST API | 1:30 | | | |
| 6 | Quality and DevOps | 1:30 | | | |
| 7 | Process and lessons | 1:00 | | | |
| 8 | Wrap-up | 1:00 | | | |
| | **Total** | **13:30** | | | |

**Adjustments made after rehearsal #1**

| Problem observed | Change | Time saved |
|---|---|---|
| | | |
| | | |

**Rehearsal #2 (optional, recommended):** Date ________ · Total time ________ · Within 15:00? ☐ Yes ☐ No

### If you're over time: cut in this order

1. Segment 2: show the diagram only, drop the ADR detail (−0:45)
2. Segment 4: skip **Add lab**; keep approve + **Users** (−0:45)
3. Segment 7: merge into the wrap-up (−0:45)
4. Segment 6: show only the green checks and the test count (−0:30)

Never cut the double-booking moment in segment 3 or the API in segment 5. They show the core business
rule and the advanced-feature requirement.
