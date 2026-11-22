#!/bin/sh
# Nightly MySQL backup for a LabLink server. Run from cron (see docs/deployment.md, section 8)
# or by hand before every release:  ./deploy/backup.sh
# Writes backups/lablink-YYYY-MM-DD-HHMM.sql.gz and deletes local copies older than 14 days.
set -eu
cd "$(dirname "$0")/.."
mkdir -p backups
file="backups/lablink-$(date +%F-%H%M).sql.gz"
docker compose exec -T db sh -c \
  'exec mysqldump --single-transaction --routines -uroot -p"$MYSQL_ROOT_PASSWORD" lablink' \
  | gzip > "$file"
# A dump that's nearly empty means mysqldump failed; fail loudly so cron mails the error.
[ "$(gzip -dc "$file" | wc -c)" -gt 1000 ] || { echo "backup too small: $file" >&2; exit 1; }
find backups -name 'lablink-*.sql.gz' -mtime +14 -delete
echo "backup ok: $file"
