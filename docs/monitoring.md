# LabLink Monitoring & Logging Strategy

CS450 Week 14. What we measure, what we log, and how we notice and respond when something breaks.
Items marked **✅ built** exist in the code on `main`; items marked **planned** are the next step and
need an external service the course project doesn't have yet.

## 1. Goals

LabLink is used to book rooms for classes, so the failures that matter most are, in order:

1. **The site is down or can't log anyone in** — nobody can book or check a reservation.
2. **Wrong bookings** — a double booking or a reservation lost, so two classes show up to one lab.
3. **Slow pages** — the site works but is painful, especially at the start of a semester.
4. **Security events** — someone guessing passwords or a role being changed without reason.

Targets (service level objectives) for a campus system:

| Objective | Target |
|---|---|
| Availability (health check succeeds), 7 a.m.–11 p.m. Eastern | 99.5 % per month (≈ 2.5 h downtime allowed) |
| Page and API response time, 95th percentile | under 500 ms |
| Server errors (HTTP 5xx) | under 1 % of requests |
| Data loss after a server failure | at most 24 hours (nightly backup) |

## 2. What we built this week

| Piece | Where | What it gives us |
|---|---|---|
| **✅ Spring Boot Actuator** | `pom.xml`, `application.properties` | `/actuator/health` (with `liveness` and `readiness` probes, and a database check), `/actuator/metrics`, `/actuator/info` |
| **✅ Access rules** | `SecurityConfig`, `deploy/Caddyfile` | Health is public (Docker and uptime checkers need it). Metrics and info need a lab-manager login, and Caddy blocks them from the internet entirely |
| **✅ Container health checks** | `Dockerfile`, `docker-compose.yml` | Docker marks the app unhealthy after 3 failed checks; `restart: unless-stopped` restarts crashed containers |
| **✅ Structured event logs** | `SecurityEventLogger`, `ReservationService`, `UserService`, `AdminBootstrap` | One `key=value` line per important event (section 4) |
| **✅ Log rotation** | `docker-compose.yml` | 10 MB × 5 files per container, so logs can't fill the disk |
| **✅ Smoke test in CI** | `.github/workflows/ci.yml` | Every change is started in Docker against MySQL and checked before merge |

## 3. Metrics we track

Spring Boot (through Micrometer) already records most of these; they're readable at
`/actuator/metrics/<name>` by a lab manager today. **Planned:** add `micrometer-registry-prometheus`,
scrape it with Prometheus every 30 s, and graph it in Grafana.

### Availability and traffic

| Metric | Source (`/actuator/metrics/...`) | Why | Alert when |
|---|---|---|---|
| Up / down | external check of `/actuator/health` | The one number users feel | 2 failed checks in a row (2 min) |
| Requests per minute, by URL | `http.server.requests` (count, tag `uri`) | Normal load vs. a spike or a crawl | — (dashboard only) |
| Response time p95, by URL | `http.server.requests` (timer) | Slow pages, especially `/reservations/new` and `/labs/{id}/availability` | p95 > 500 ms for 10 min |
| 5xx error rate | `http.server.requests` with `outcome=SERVER_ERROR` | Bugs and database failures show up here first | > 1 % for 5 min, or any 5xx burst > 20/min |
| 4xx rate on `/api/**` | `http.server.requests` with `outcome=CLIENT_ERROR` | A campus system calling our API wrongly | — (dashboard only) |

### Resources

| Metric | Source | Why | Alert when |
|---|---|---|---|
| JVM heap used / max | `jvm.memory.used`, `jvm.memory.max` (`area=heap`) | Memory leaks; container limit is 768 MB | > 85 % of max for 15 min |
| GC pause time | `jvm.gc.pause` | Long pauses = slow requests | > 1 s total per minute |
| DB connections active / pending | `hikaricp.connections.active`, `hikaricp.connections.pending` | Pool exhaustion makes every page hang | any pending for > 1 min |
| DB query time | `hikaricp.connections.usage` | Missing index or a slow query | p95 > 200 ms |
| CPU | `process.cpu.usage`, `system.cpu.usage` | Undersized server | > 80 % for 15 min |
| Disk free (server and MySQL volume) | node exporter / `df` | MySQL stops writing when the disk is full | < 20 % free |
| Container restarts | `docker inspect` restart count | Crash loops | > 2 in 10 min |
| Backup age | timestamp of newest `backups/*.sql.gz` | Silent backup failure | older than 26 h |

### Business metrics

These say whether the *product* is working, not just the server. They can be counted from the event logs
(section 4) or with simple SQL queries.

| Metric | How | Why |
|---|---|---|
| Reservations created per day (lab vs. equipment) | count `reservation.created` | A sudden drop to zero during the semester means something is broken even if health is green |
| Pending requests and age of oldest | `SELECT COUNT(*), MIN(created_at) FROM reservations WHERE status='PENDING'` | Students waiting days for approval is a process problem lab managers should see |
| Rejected / cancelled ratio | count `reservation.status` by `to=` | Rising rejections may mean the UI lets students request things they can't have |
| Double-booking conflicts refused | count `ReservationException` "already booked" messages | Demand hot spots; which labs need more hours |
| Failed logins per hour | count `auth.login.failure` | Security (section 5) |

## 4. Events we log

### Format and rules

- One line per event: `event.name key=value key=value`, written to standard output, collected by Docker
  (`docker compose logs app`). Spring Boot prefixes each line with the timestamp, level and logger.
- Users are identified by **id**, not name, except the login events, which need the email typed.
- **Never logged:** passwords (or hashes), session ids, full request bodies, reservation `purpose` text.
- Levels: `INFO` normal events, `WARN` something a person should look at, `ERROR` a request failed
  because of a bug or an outage.

### Event catalogue

| Event | Level | Example | Why | Status |
|---|---|---|---|---|
| `auth.login.success` | INFO | `auth.login.success user=student@lablink.edu` | Who is using the system; audit trail | ✅ built |
| `auth.login.failure` | WARN | `auth.login.failure user=student@lablink.edu reason=BadCredentialsException` | Password-guessing detection | ✅ built |
| `user.created` | INFO | `user.created id=42 role=FACULTY` | New accounts, including admin-created ones | ✅ built |
| `user.role.changed` | INFO | `user.role.changed id=42 from=STUDENT to=LAB_MANAGER` | Privilege changes are the most sensitive admin action | ✅ built |
| `bootstrap.admin.created` | WARN | `bootstrap.admin.created email=...` | First-admin creation must be noticed and the password removed | ✅ built |
| `reservation.created` | INFO | `reservation.created id=310 user=7 status=PENDING` | Business volume; reconstruct what happened | ✅ built |
| `reservation.status` | INFO | `reservation.status id=310 from=PENDING to=APPROVED by=2` | Every approve / reject / cancel / reschedule | ✅ built |
| Flyway migrations applied | INFO | `Successfully applied 1 migration to schema "lablink"` | Confirms a release changed the schema as expected | ✅ (Spring/Flyway default) |
| Unhandled exceptions | ERROR | stack trace from Spring's error handling | Every 5xx has a cause in the log | ✅ (Spring default) |
| App start / stop | INFO | `Started LabLinkApplication in 9.8 seconds` / graceful shutdown | Restarts and deploys on the timeline | ✅ (Spring default) |
| Inventory changes (lab/equipment created, edited, retired) | INFO | `inventory.lab.updated id=5 by=2` | Who changed what lab managers rely on | planned |
| Booking refused (conflict, loan limit) | INFO | `reservation.refused lab=5 reason=conflict` | Demand hot spots | planned |
| Slow request | WARN | `http.slow uri=/labs/{id}/availability ms=2300` | Pinpoints which page is slow | planned |

### Retention and searching

- **Now:** Docker keeps the last ~50 MB per container; search with
  `docker compose logs app | grep auth.login.failure`.
- **Planned:** ship logs to a central store (Grafana Loki, or the cloud provider's log service) and keep
  them 90 days. Switch to JSON log output (`logging.structured.format.console=ecs`, built into Spring Boot 3.4+)
  so fields can be filtered directly.

## 5. Detecting failures

Three layers, from fastest and simplest to most detailed:

1. **Self-healing (✅ built).** Docker's health check plus `restart: unless-stopped` restarts a crashed or
   hung app in about a minute with no human involved. `-XX:+ExitOnOutOfMemoryError` turns a memory
   exhaustion into a clean restart.
2. **External uptime check (planned, free tier is enough).** A service such as UptimeRobot or Better Stack
   requests `https://<domain>/actuator/health` every minute *from outside our server* and emails/texts
   the on-call person after 2 failures. This is the check that still works when the whole server is down.
   The health endpoint includes the database check, so a dead MySQL turns it red too.
3. **Metric and log alerts (planned).** Prometheus alert rules (thresholds in section 3) and log queries:
   - more than 10 `auth.login.failure` for one email in 5 minutes → possible password guessing
   - any `user.role.changed ... to=LAB_MANAGER` → notify the other lab managers
   - `reservation.created` count = 0 between 9 a.m. and 5 p.m. on a weekday during the semester → silent breakage
   - newest backup older than 26 hours

Alerts go to the team's group chat; outages page the on-call person by text.

## 6. Responding to failures

### Severity levels

| Severity | Meaning | Example | Response time |
|---|---|---|---|
| **SEV1** | Nobody can use LabLink, or data is wrong | Site down; double bookings | Start within 30 min during campus hours |
| **SEV2** | A major feature broken, workaround exists | Approvals failing; very slow pages | Same day |
| **SEV3** | Minor bug or cosmetic | One page's layout broken on mobile | Next sprint (normal backlog) |

### Incident process

1. **Detect** — alert, uptime check, or user report.
2. **Acknowledge** — the on-call person posts "I'm on it" in the team chat so two people don't fight the same fire.
3. **Triage** — `docker compose ps`, `docker compose logs --tail 200 app`, `curl /actuator/health`. Decide the severity.
4. **Mitigate first, fix later** — restart, roll back (deployment guide §10), or put up a notice. Getting users back
   matters more than finding the root cause right now.
5. **Fix** — a normal branch → PR → CI → release, plus a test that would have caught it.
6. **Post-incident review** (SEV1/SEV2) — a short blameless write-up: timeline, root cause, what we change so it
   can't happen again. Follow-up tasks go on the backlog as corrective or preventive maintenance.

### Runbook for likely failures

| Symptom | Likely cause | First check | Response |
|---|---|---|---|
| Health check fails, `app` restarting | Startup failure: bad config or migration | `docker compose logs app` — look for Flyway or `ddl-auto=validate` errors | Fix `.env`; if a new release, roll back to the previous image tag |
| Health shows `db: DOWN` | MySQL stopped or disk full | `docker compose ps db`, `df -h` | `docker compose up -d db`; free disk (old images: `docker image prune`) |
| Pages slow, p95 high | Pool exhausted or slow query | `hikaricp.connections.pending`; MySQL `SHOW PROCESSLIST` | Restart app to recover; add an index via a new migration |
| 502 from Caddy | App container down or restarting | `docker compose ps` | Wait for the health check / restart; if it doesn't recover, follow row 1 |
| Certificate error in the browser | Let's Encrypt renewal failed (DNS changed, port 80 blocked) | `docker compose logs caddy` | Fix DNS / firewall; Caddy retries automatically |
| Burst of `auth.login.failure` for one account | Password guessing | count by `user=` and time | Tell the account owner to change the password; block the source IP in the firewall. Long-term fix: preventive item PV1 in maintenance.md (rate limiting) |
| Two reservations for the same lab and time | Race condition (ADR-0004) | `SELECT` overlapping rows for that lab | Lab manager contacts both users and cancels one; fix is corrective item C2 |
| Unexpected `user.role.changed` to `LAB_MANAGER` | Compromised manager account or mistake | `reservation_status_history` and logs for that user id | Change the role back, reset that manager's password, review what they changed |
| Backup age alert | `backup.sh` failing (disk, password change) | `backups/backup.log` | Fix and run `./deploy/backup.sh` by hand immediately |
