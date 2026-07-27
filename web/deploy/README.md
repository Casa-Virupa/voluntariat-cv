# Deploying the dashboard

Hostinger VPS, Next.js standalone behind nginx, **one process**, SQLite on a path that is
never inside a release. Everything here assumes Debian/Ubuntu and a `voluntariat` system
user.

## Layout on the server

```
/srv/voluntariat/
  repo/                     git checkout, only used to build
  releases/20260727-120000/ a built release (standalone server + static + drizzle)
  current -> releases/…      symlink the systemd unit points at
  data/                     THE DATABASE LIVES HERE, never in a release
    voluntariat.db
    backups/
    sync.log  backup.log
/etc/voluntariat/env        secrets, chmod 600, root:voluntariat
/etc/voluntariat/firebase-sa.json   service-account key, chmod 600
```

The database being outside the release directory is not tidiness: it is what makes a deploy
or a rollback incapable of overwriting real payment history. `next.config.ts` also excludes
`./data/**` from the build trace so it can never travel inside the artefact.

## First install

```bash
# 1. user, directories, packages
adduser --system --group --home /srv/voluntariat voluntariat
mkdir -p /srv/voluntariat/{repo,releases,data/backups} /etc/voluntariat
chown -R voluntariat:voluntariat /srv/voluntariat
apt install -y nginx sqlite3 certbot python3-certbot-nginx
# Node 22 LTS or newer (better-sqlite3 needs a prebuilt for it, or build tools)

# 2. secrets
install -o root -g voluntariat -m 600 /dev/null /etc/voluntariat/env
$EDITOR /etc/voluntariat/env          # contents below

# 3. code
git clone <repo> /srv/voluntariat/repo
cd /srv/voluntariat/repo/web
./deploy/deploy.sh                    # builds, migrates, symlinks, starts

# 4. the first coordinator — without this nobody can sign in at all
DATABASE_PATH=/srv/voluntariat/data/voluntariat.db \
  node scripts/add-admin.ts you@example.cat --coordinator --name "Nom"

# 5. service, proxy, TLS, cron
cp deploy/voluntariat.service /etc/systemd/system/
systemctl daemon-reload && systemctl enable --now voluntariat
cp deploy/nginx.conf /etc/nginx/sites-available/voluntariat
ln -s /etc/nginx/sites-available/voluntariat /etc/nginx/sites-enabled/
nginx -t && systemctl reload nginx
certbot --nginx -d voluntariat.example.cat
crontab -u voluntariat /srv/voluntariat/current/deploy/crontab
```

### `/etc/voluntariat/env`

```sh
NODE_ENV=production
TZ=Europe/Madrid

AUTH_SECRET=              # openssl rand -base64 32
AUTH_GOOGLE_ID=
AUTH_GOOGLE_SECRET=
AUTH_URL=https://voluntariat.example.cat
AUTH_TRUST_HOST=true

DATABASE_PATH=/srv/voluntariat/data/voluntariat.db

FIREBASE_PROJECT_ID=voluntariat-casa-virupa
GOOGLE_APPLICATION_CREDENTIALS=/etc/voluntariat/firebase-sa.json

SYNC_KEY=                 # openssl rand -hex 32

VOLUNTARIAT_OFFSITE=      # rclone remote or user@host:/path — see "Backups"
```

The Google OAuth client needs
`https://voluntariat.example.cat/api/auth/callback/google` as an authorised redirect URI.
The Firebase service account needs only **Cloud Datastore User** (read/write on
`payments`; everything else the dashboard touches is read-only by choice, not by grant).

## Deploying a change

```bash
cd /srv/voluntariat/repo && git pull
web/deploy/deploy.sh
```

The script runs `npm ci`, the test suite and the build, assembles a release, applies
migrations **before** flipping the symlink, restarts, and rolls back to the previous release
if the service does not come up. Old releases are pruned to the last five.

Rollback by hand:

```bash
ln -sfn /srv/voluntariat/releases/<previous> /srv/voluntariat/current
systemctl restart voluntariat
```

A rollback does **not** undo a migration. Migrations here are additive; if one ever is not,
restore a backup instead.

## Why one process

SQLite in WAL mode is safe with many readers and one writer *in one process*. Two Node
processes on the same file can interleave writes during a sync and corrupt it. So: no PM2
cluster mode, no second unit, no `npm start` in a shell "just to check something". The load
is a dozen coordinators; there is nothing to scale.

If the site ever needs a second process, the database has to move to Postgres first.

## The sync schedule

`00:10` and `12:10`, plus a full resync on Sunday at `04:10`.

The ten-minute offset is deliberate: a sync firing exactly at the day rollover races the
`service_date <= today` boundary that splits "fets" from "previstos" on the coordination
table. The weekly full resync exists because a windowed sync only detects deletions inside
the window it read — a booking deleted a year later would otherwise stay in the mirror
forever.

Check it by hand:

```bash
set -a; . /etc/voluntariat/env; set +a
curl -fsS -X POST -H "X-Sync-Key: $SYNC_KEY" http://127.0.0.1:3000/api/sync | jq
curl -fsS -H "X-Sync-Key: $SYNC_KEY" http://127.0.0.1:3000/api/sync | jq   # status only
```

The header of every page shows how stale the mirror is: green, amber past 14 h, red past
26 h. Full history is at **Configuració → Sincronització**.

## Backups

`deploy/backup.sh` runs nightly at 03:20. It uses `sqlite3 .backup`, not `cp` — in WAL mode
a plain copy of the `.db` silently loses whatever is still in the `-wal`, which is precisely
the recent data you would want. It then runs `PRAGMA integrity_check`, logs the row counts
of the tables that **cannot** be rebuilt from Firestore (ledger, prices, commitments,
allowlist), gzips, prunes past 30 days, and copies offsite.

**Set `VOLUNTARIAT_OFFSITE`.** A backup on the same VPS is not a backup against the failure
mode that matters.

Restore:

```bash
systemctl stop voluntariat
gunzip -c /srv/voluntariat/data/backups/voluntariat-<ts>.db.gz > /tmp/restore.db
sqlite3 /tmp/restore.db 'PRAGMA integrity_check;'
mv /srv/voluntariat/data/voluntariat.db /srv/voluntariat/data/voluntariat.db.broken
mv /tmp/restore.db /srv/voluntariat/data/voluntariat.db
chown voluntariat:voluntariat /srv/voluntariat/data/voluntariat.db
systemctl start voluntariat
# then run a sync: the mirror will be a day stale, the ledger will not
```

## Enabling the payments write-back

Off by default, and it should stay off until someone has read the report.

1. **Configuració → Escriptura a Firebase** shows, month by month, what the dashboard
   derives against what the app's `payments` documents say.
2. Expect large differences. `payments.amount` is wrong in the app for anyone who has ever
   cancelled a booking: `addPayment` increments it on every booking and never decrements,
   and `pay()` sets `paid = true` without clearing the amount.
3. Before switching it on, record the historic difference as an `opening_balance` ledger
   entry per volunteer (side panel → "Registra un pagament o un ajust"), so the gap is
   documented rather than erased.
4. Then enable it and write one month. Every write is a compare-and-set inside a Firestore
   transaction against the value the last sync observed, only `amount` and `paid` are ever
   touched, and only the canonical (`MIN(doc_id)`) document is written. If the app moved in
   between, that volunteer is skipped and the next sync recomputes.
5. Check the result on a real phone before doing more months.

## Troubleshooting

| Symptom | Where to look |
| --- | --- |
| "Mai sincronitzat" in the header | `journalctl -u voluntariat -n 100`; then Configuració → Sincronització for the error |
| Sync fails with `invalid_grant` | the service-account key, or the clock: `timedatectl` |
| Sync stuck as `running` | a dead run releases after 10 min via the heartbeat; the mutex is a partial unique index on `sync_run`, so it cannot double-run |
| "Protecció d'esborrat massiu" | a truncated Firestore read. Nothing was written. Re-run; if it repeats, the window really did lose bookings |
| Hours all zero | Configuració → Qualitat de dades → "Torns de 0 minuts". A high count means the `time_range` wire format is not what the parser expects |
| Numbers disagree with the app | expected: the app's Profile screen counts a flat 4 h per shift, the dashboard uses the real time range (as the app's own History screen does) |
| Nobody can sign in | `sqlite3 …/voluntariat.db 'select email, role, disabled_at from admin_user;'`, then `scripts/add-admin.ts` |
| 500 after a deploy | `ls -l /srv/voluntariat/current`, `journalctl -u voluntariat`; roll the symlink back |
