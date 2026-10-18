# ADR-0004: Enforce double-booking prevention in the service layer with an overlap query

- **Status:** Accepted
- **Date:** 2026-10-18
- **Deciders:** LabLink team

## Context

The most important rule in LabLink is that a lab or piece of equipment cannot be reserved by two people at the same time. Options: check in the controller, check in the service, or rely only on a database constraint. Standard SQL cannot express "no overlapping time ranges" as a simple constraint in MySQL.

## Decision

`ReservationService` checks for conflicts before saving, inside the same transaction, using a repository query that implements the interval-overlap test:

```
existing.start_time < requested.end_time  AND  existing.end_time > requested.start_time
```

restricted to the same resource and to statuses that still hold the slot (`PENDING`, `APPROVED`). Back-to-back bookings (one ends at 10:00, the next starts at 10:00) are allowed. Pending requests hold the slot so two students cannot both be waiting for the same time.

## Consequences

**Positive**
- One authoritative place for the rule, covered by automated tests (`ReservationServiceTest`).
- Clear error message shown to the user ("Robotics Lab is already booked during that time").
- Foreign-key indexes on `lab_id` / `equipment_id` keep the lookup fast.

**Negative**
- Two requests submitted at exactly the same instant could both pass the check (race condition). Acceptable for campus-scale traffic in the prototype; if needed later, lock the resource row (`SELECT … FOR UPDATE`) before checking.
