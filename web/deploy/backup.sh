#!/usr/bin/env bash
#
# Nightly SQLite backup.
#
# `sqlite3 .backup` is used rather than `cp` because the database runs in WAL mode: copying
# the .db file alone silently loses every transaction still sitting in the -wal, which is
# exactly the recent data you would want after a disk failure. `.backup` takes a consistent
# snapshot of a live database without blocking the writer.
#
# The dashboard-owned tables — the ledger, the prices, the commitments, the allowlist — are
# the only data in here that does not exist anywhere else. The fs_* mirror can always be
# rebuilt from Firestore; the payment history cannot.

set -euo pipefail

DATA_DIR="${VOLUNTARIAT_DATA_DIR:-/srv/voluntariat/data}"
DB="${DATABASE_PATH:-$DATA_DIR/voluntariat.db}"
BACKUP_DIR="${VOLUNTARIAT_BACKUP_DIR:-$DATA_DIR/backups}"
KEEP_DAYS="${VOLUNTARIAT_BACKUP_KEEP_DAYS:-30}"

# Offsite target, e.g. "remote:voluntariat" for rclone or "user@host:/path" for rsync.
# Empty means local-only, which is NOT a backup if the VPS is the thing that fails.
OFFSITE="${VOLUNTARIAT_OFFSITE:-}"

timestamp="$(date +%Y%m%d-%H%M%S)"
target="$BACKUP_DIR/voluntariat-$timestamp.db"

mkdir -p "$BACKUP_DIR"

if [[ ! -f "$DB" ]]; then
  echo "$(date +%Y-%m-%dT%H:%M:%S%z) ERROR: no database at $DB" >&2
  exit 1
fi

sqlite3 "$DB" ".backup '$target'"

# A backup that cannot be opened is worse than none, because it hides the failure. Check
# before compressing and before deleting anything old.
if ! sqlite3 "$target" 'PRAGMA integrity_check;' | grep -qx 'ok'; then
  echo "$(date +%Y-%m-%dT%H:%M:%S%z) ERROR: integrity check failed for $target — keeping it, deleting nothing" >&2
  exit 1
fi

# Row counts of the tables that cannot be rebuilt from Firestore, logged so a silently
# emptying ledger is visible in the backup log rather than discovered a year later.
counts="$(sqlite3 "$target" "
  SELECT 'ledger=' || (SELECT COUNT(*) FROM ledger_entry)
      || ' prices=' || (SELECT COUNT(*) FROM price_rule)
      || ' commitments=' || (SELECT COUNT(*) FROM commitment_rule)
      || ' admins=' || (SELECT COUNT(*) FROM admin_user)
      || ' bookings=' || (SELECT COUNT(*) FROM fs_booking WHERE deleted_at IS NULL);")"

# `.backup` leaves -wal/-shm sidecars next to the snapshot. They hold nothing (the snapshot
# is already consistent) and the prune below only matches *.db.gz, so remove them here
# rather than letting them accumulate forever.
rm -f "$target-wal" "$target-shm"

gzip -9 "$target"
echo "$(date +%Y-%m-%dT%H:%M:%S%z) ok $target.gz ($(du -h "$target.gz" | cut -f1)) $counts"

find "$BACKUP_DIR" -name 'voluntariat-*.db.gz' -mtime "+$KEEP_DAYS" -delete

if [[ -n "$OFFSITE" ]]; then
  if command -v rclone >/dev/null 2>&1 && [[ "$OFFSITE" == *:* && "$OFFSITE" != *@* ]]; then
    rclone copy "$target.gz" "$OFFSITE" --quiet
  else
    rsync -a "$target.gz" "$OFFSITE"
  fi
  echo "$(date +%Y-%m-%dT%H:%M:%S%z) offsite copy sent to $OFFSITE"
else
  echo "$(date +%Y-%m-%dT%H:%M:%S%z) WARNING: VOLUNTARIAT_OFFSITE is not set — backups are only on this host"
fi
