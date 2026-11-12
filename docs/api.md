# LabLink REST API (advanced feature, backlog 3.6)

A small, public, **read-only** JSON API so other campus systems can check lab availability without
logging in. Examples: a department web page that shows "Robotics Lab: free now", or a screen outside
a lab door that lists today's bookings.

- Base URL (local): `http://localhost:8080/api`
- Authentication: none for `GET`. Every other method is refused (`403`).
- Dates and times use ISO-8601 local time, for example `2026-11-16T08:00`.
- The API never says *who* booked a lab, only when it is busy.

Code: [`AvailabilityApiController`](../src/main/java/edu/campbellsville/lablink/api/AvailabilityApiController.java),
which reuses `AvailabilityService` (the same service behind the lab availability page).

## `GET /api/labs`

Every lab that can currently be reserved (inactive labs are left out).

```http
GET /api/labs
```

```json
[
  {
    "id": 5,
    "name": "Cybersecurity Lab",
    "building": "Science Building",
    "roomNumber": "217",
    "capacity": 16,
    "department": "Information Technology"
  }
]
```

## `GET /api/labs/{id}/availability`

When a lab is booked during a time window. A booking counts if it is `PENDING` or `APPROVED`,
the same rule the app uses to prevent double booking.

| Parameter | Required | Default | Notes |
|---|---|---|---|
| `from` | no | today at 00:00 | start of the window |
| `to` | no | `from` + 7 days | end of the window; at most 31 days after `from` |

```http
GET /api/labs/5/availability?from=2026-09-22T08:00&to=2026-09-22T18:00
```

```json
{
  "labId": 5,
  "labName": "Cybersecurity Lab",
  "from": "2026-09-22T08:00:00",
  "to": "2026-09-22T18:00:00",
  "free": false,
  "busy": [
    { "start": "2026-09-22T13:00:00", "end": "2026-09-22T15:00:00", "status": "APPROVED" }
  ]
}
```

`free` is `true` when `busy` is empty, so "is the lab free from 3 to 5?" is one request:
`GET /api/labs/5/availability?from=2026-09-22T15:00&to=2026-09-22T17:00`.

## Errors

Errors come back as JSON with the HTTP status repeated in the body.

| Status | When | Example body |
|---|---|---|
| `400` | `to` is not after `from`, the window is over 31 days, or a date can't be read | `{"status": 400, "error": "Ask for at most 31 days at a time"}` |
| `404` | no lab with that id | `{"status": 404, "error": "Lab not found"}` |

## Trying it

With the app running (`mvn spring-boot:run`):

```bash
curl http://localhost:8080/api/labs
curl "http://localhost:8080/api/labs/5/availability?from=2026-09-22T08:00&to=2026-09-22T18:00"
```

Lab ids come from `GET /api/labs`; they depend on the order the seed data was loaded.

## Tests

- `AvailabilityApiControllerTest`: unit tests, with the controller called as plain Java and the services mocked
- `AvailabilityApiIntegrationTest`: integration tests against the running app with no logged-in user
