# LabLink Deployment Guide

CS450 Week 14 — how to deploy LabLink to a production-like environment.
For how the image itself is built, see [containerization.md](containerization.md).

## 1. Target environment

One Linux server running Docker, with three containers managed by Docker Compose:

```
                    Internet
                       │ HTTPS :443  (HTTP :80 redirects to HTTPS)
┌──────────────────────┼─────────────── Linux server (Ubuntu 24.04 LTS) ─┐
│               ┌──────▼──────┐                                         │
│               │    caddy    │  reverse proxy, automatic TLS certificate│
│               └──────┬──────┘                                         │
│                      │ http://app:8080   (private Docker network)      │
│               ┌──────▼──────┐        ┌──────────────┐                 │
│               │     app     │──JDBC─▶│      db      │                 │
│               │   LabLink   │        │  MySQL 8.4   │──▶ volume       │
│               └─────────────┘        └──────────────┘    db-data      │
│  firewall: only 22, 80, 443 open    nightly deploy/backup.sh ─▶ backups/ │
└────────────────────────────────────────────────────────────────────────┘
```

This fits LabLink's scale (one campus, a few hundred users) and its architecture (a single deployable
monolith, ADR-0001). The same images could later move to a managed container service with a managed
MySQL database without code changes, because all configuration comes from environment variables.

### Required infrastructure

| Item | Minimum | Notes |
|---|---|---|
| Linux server (any cloud VM or campus server) | 2 vCPU, 4 GB RAM, 30 GB disk | Ubuntu 24.04 LTS assumed below. App limit 768 MB + MySQL limit 1 GB + OS |
| Domain name | e.g. `lablink.campbellsville.edu` | An **A record** pointing at the server's public IP |
| Open ports | 22, 80, 443 | 80 is needed for Let's Encrypt to issue the certificate |
| Software | Docker Engine 24+ with the Compose plugin, Git | Installed in step 2 |
| Backup storage | Separate from the server | Object storage bucket, or at least another machine |

## 2. Prepare the server

```bash
# Docker's official convenience script (installs Engine + Compose plugin)
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER          # log out and back in afterwards

# Firewall: SSH, HTTP, HTTPS only
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable

# Turn on automatic OS security updates
sudo apt-get install -y unattended-upgrades
```

Create the DNS **A record** for your domain now; certificates can only be issued once it resolves.

## 3. Get a release of the code

Production always runs a **tagged release** from `main`, never a feature branch.

```bash
sudo mkdir -p /opt/lablink && sudo chown $USER /opt/lablink
git clone https://github.com/westonm05/Reservation-Project.git /opt/lablink
cd /opt/lablink
git checkout v1.0.0                     # the release tag being deployed
```

## 4. Environment variables

Create `/opt/lablink/.env` from the template and fill it in. `.env` is git-ignored and must be
readable only by the deploy user (`chmod 600 .env`).

```bash
cp .env.example .env
chmod 600 .env
openssl rand -base64 24                 # run twice to generate the two database passwords
```

| Variable | Production value | Used by | Purpose |
|---|---|---|---|
| `LABLINK_DOMAIN` | `lablink.campbellsville.edu` | caddy | Hostname to serve and get a certificate for. **Required** |
| `LABLINK_IMAGE` | `lablink:v1.0.0` | app | Image tag to run; tag = release, so rollback is a tag change |
| `SPRING_PROFILES_ACTIVE` | `mysql` (forced by `docker-compose.prod.yml`) | app | Schema only. **Never** `seed` in production: the seed script deletes all rows |
| `DB_USERNAME` | `lablink` | app, db | Application database user |
| `DB_PASSWORD` | long random string | app, db | Application database password |
| `MYSQL_ROOT_PASSWORD` | different long random string | db | MySQL root; used only for backups and admin |
| `DB_URL` | leave unset (uses the `db` container) | app | Set only when using a managed MySQL (section 5, option B) |
| `LABLINK_ADMIN_EMAIL` | the lab manager's real email | app | Creates the first lab-manager account on an empty database |
| `LABLINK_ADMIN_PASSWORD` | 12+ characters | app | Its initial password. **Remove after first start** |
| `TZ` | `America/New_York` | app, db | Campus time; reservation rules use local time |
| `JAVA_OPTS` | default (`-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError`) | app | JVM tuning |

Other settings are already correct in `application.properties` / `application-mysql.properties`
and normally never change: `spring.jpa.hibernate.ddl-auto=validate`, Thymeleaf caching on, Flyway enabled,
graceful shutdown, Actuator exposure.

## 5. Database setup

### Option A (default): MySQL container on the same server

Nothing to do by hand. On first start the `mysql:8.4` image creates the `lablink` database and the
`DB_USERNAME` user from `.env`, then LabLink's **Flyway** migrations (`V1__` … `V4__`) build every table
and index. Data lives in the `db-data` Docker volume. MySQL has no published port, so it can't be reached
from the internet.

### Option B: managed MySQL (e.g. a cloud provider's MySQL service)

Better once the system matters: the provider handles patching, replication and point-in-time backups.
Create the database and user once (Flyway needs DDL rights on its own schema only):

```sql
CREATE DATABASE lablink CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'lablink'@'%' IDENTIFIED BY '<DB_PASSWORD>';
GRANT ALL PRIVILEGES ON lablink.* TO 'lablink'@'%';
```

Then set in `.env` (TLS required over a network):

```
DB_URL=jdbc:mysql://<host>:3306/lablink?sslMode=REQUIRED
```

and remove the `db` service and the app's `depends_on: db` from your server's copy of the compose files.
Restrict the managed instance's firewall to the app server's IP.

### Migrations

Flyway runs automatically every time the app starts and applies only migrations that haven't run yet
(recorded in `flyway_schema_history`). Hibernate is set to `validate`, so the app refuses to start if
the code and the schema don't match, instead of corrupting data. Schema changes are only ever made by
adding a new `V5__...sql` file through a pull request.

## 6. First start

```bash
cd /opt/lablink
docker compose -f docker-compose.yml -f docker-compose.prod.yml build
docker tag lablink:latest lablink:v1.0.0
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --wait
```

To avoid typing both files every time: `export COMPOSE_FILE=docker-compose.yml:docker-compose.prod.yml`
(put it in the deploy user's `~/.bashrc`). The commands below assume it is set.

Watch the first start:

```bash
docker compose logs -f app
```

Expected lines: Flyway `Successfully applied 4 migrations`, then
`bootstrap.admin.created email=...`, then `Started LabLinkApplication`.

**Then immediately** delete `LABLINK_ADMIN_PASSWORD` (and `LABLINK_ADMIN_EMAIL`) from `.env` and run
`docker compose up -d` again. The lab manager logs in, changes the password on the Account page, and adds
other faculty/lab-manager accounts from the Admin page. Students register themselves.

## 7. Verify the deployment

| Check | How | Expected |
|---|---|---|
| Containers healthy | `docker compose ps` | `app` and `db` show `healthy`, `caddy` running |
| HTTPS works | open `https://<domain>` | Login page, valid certificate, HTTP redirects to HTTPS |
| Health endpoint | `curl https://<domain>/actuator/health` | `{"status":"UP"}` |
| Metrics hidden | `curl -i https://<domain>/actuator/metrics` | `404` (blocked by Caddy) |
| Database reachable | log in as the lab manager, create a lab | Lab appears in the list and in `GET /api/labs` |
| No demo data | try `manager@lablink.edu` / `manager123` | Login **fails** |
| Times correct | request a reservation 1 hour from now | Accepted (not "must start in the future") |

## 8. Backups

The database is the only thing that can't be rebuilt from Git. Back it up nightly and copy it off the server.

[`deploy/backup.sh`](../deploy/backup.sh) takes a consistent `mysqldump` (no locking, thanks to
`--single-transaction`), compresses it to `backups/lablink-<date>.sql.gz`, fails loudly if the dump is
suspiciously small, and deletes local copies older than 14 days. Schedule it with `crontab -e`:

```cron
COMPOSE_FILE=docker-compose.yml:docker-compose.prod.yml
15 2 * * * /opt/lablink/deploy/backup.sh >> /opt/lablink/backups/backup.log 2>&1
```

Copy `backups/` to off-server storage (e.g. `rclone copy` to a bucket) in the same cron line or a second one.
**Test a restore at least once a month** into a scratch database — a backup that has never been restored
is not a backup:

```bash
# restore test into a scratch database, then count rows and drop it
docker compose exec -T db sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "CREATE DATABASE restore_test"'
gunzip -c backups/lablink-2026-11-20-0215.sql.gz | docker compose exec -T db sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" restore_test'
docker compose exec -T db sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SELECT COUNT(*) FROM restore_test.reservations; DROP DATABASE restore_test"'
```

A real recovery is the same restore into `lablink` with the app stopped (`docker compose stop app`).

## 9. Releasing an update

1. Merge to `main` through a PR (CI green: Lint, Build & Test, Docker Smoke Test).
2. Tag the release: `git tag v1.1.0 && git push --tags`.
3. On the server:
   ```bash
   cd /opt/lablink
   ./deploy/backup.sh          # always back up right before a release
   git fetch --tags && git checkout v1.1.0
   docker compose build && docker tag lablink:latest lablink:v1.1.0
   sed -i 's/^LABLINK_IMAGE=.*/LABLINK_IMAGE=lablink:v1.1.0/' .env
   docker compose up -d --wait
   ```
4. Run the checks in section 7. Flyway applies any new migrations on startup.

Expected downtime is the app's restart time (about 20–40 seconds); Caddy returns 502 during it.
That is acceptable for a campus system if releases happen outside class hours.

## 10. Rollback

If a release misbehaves, point `LABLINK_IMAGE` back at the previous tag and restart:

```bash
sed -i 's/^LABLINK_IMAGE=.*/LABLINK_IMAGE=lablink:v1.0.0/' .env
docker compose up -d --wait
```

The old images stay on the server, so this takes under a minute. **Exception:** if the bad release
included a migration that changed or dropped existing columns, the old code will fail Hibernate
validation against the new schema. Then restore the pre-release backup (section 8) before starting the old
version. This is why migrations should be additive (add columns/tables) whenever possible.

## 11. Production security checklist

- [ ] `SPRING_PROFILES_ACTIVE` does not contain `seed` (no demo accounts with known passwords)
- [ ] `.env` is `chmod 600`, not in Git, and the bootstrap admin password has been removed
- [ ] Database passwords are random and different from each other
- [ ] Only ports 22, 80, 443 are open; MySQL is not published
- [ ] `/actuator/metrics` returns 404 from the internet
- [ ] SSH uses keys, password login disabled
- [ ] Nightly backups running and copied off the server; a restore has been tested
- [ ] Monitoring from [monitoring.md](monitoring.md) is set up (at minimum the external uptime check)
