# Deploying the dashboard

Hostinger VPS, Next.js standalone behind nginx, managed by **PM2**, **one process**, SQLite
on a path that is never inside a release. Debian/Ubuntu.

**This VPS is shared.** Several other Next apps run under the same PM2 daemon and behind the
same nginx, each with its own certificate. Everything below is scoped so it cannot disturb
them: PM2 verbs always carry `--only`, nginx lives in its own `sites-available` file with
prefixed zone and upstream names, and the certificate is issued with `certbot certonly
--webroot` so the `--nginx` plugin never rewrites anyone's vhost. The app listens on
**127.0.0.1:3003** — check with `ss -ltnp` before assuming that port is still free.

Never run `pm2 reload all`, `pm2 restart all`, `pm2 update`, `pm2 kill` or `pm2 flush` on
this box: each of them cycles or truncates the logs of *every* app, not just this one.

## Before anything else, on your own machine: gcloud

Firestore access needs credentials, and locally those come from **Application Default
Credentials** against the **dev** project. Without this the sync fails immediately, before
it reads a single booking.

```bash
gcloud auth application-default login --project voluntariat-casa-virupa-dev
gcloud auth application-default set-quota-project voluntariat-casa-virupa-dev
```

Both are needed, and the second is the one people skip: without a quota project the
credentials authenticate fine and then every Firestore call is rejected for having nothing
to bill the API quota to. `login` writes
`~/.config/gcloud/application_default_credentials.json`; nothing else reads that file, so
re-running it is always safe.

**This is local only — it is not part of a deploy, and it is not in `deploy.sh`.** Two
reasons: `application-default login` is interactive and opens a browser, which a headless
VPS cannot do; and on the server `GOOGLE_APPLICATION_CREDENTIALS` points at a
service-account key, which takes precedence over ADC anyway. The server also runs against
the **prod** project (`FIREBASE_PROJECT_ID` below), not `-dev` — that asymmetry is the
point: a local sync must never be able to write to the real `payments` documents.

If `gcloud` itself is not installed: `brew install --cask google-cloud-sdk`.

## Layout on the server

```
/srv/voluntariat-dashboard/
  repo/                     git checkout, only used to build
  releases/20260727-120000/ a built release (standalone server + static + drizzle)
  current -> releases/…      symlink PM2's cwd points at
  data/                     THE DATABASE LIVES HERE, never in a release
    voluntariat.db
    backups/
    sync.log  backup.log
/etc/voluntariat/env        secrets, chmod 600, owned by the PM2 user
/etc/voluntariat/firebase-sa.json   service-account key, chmod 600
```

The database being outside the release directory is not tidiness: it is what makes a deploy
or a rollback incapable of overwriting real payment history. `next.config.ts` also excludes
`./data/**` from the build trace so it can never travel inside the artefact.

## First install

Run everything as **the user that already owns the PM2 daemon** — the one whose `pm2 list`
shows the other apps. Not root, unless that is genuinely the PM2 user: `sudo pm2` talks to
root's own daemon, which is a separate, empty process list.

```bash
# 1. directories, packages.  $PM2_USER = whoever runs the other apps.
sudo mkdir -p /srv/voluntariat-dashboard/{repo,releases,data/backups} /etc/voluntariat
sudo chown -R "$PM2_USER" /srv/voluntariat-dashboard
sudo apt install -y nginx sqlite3 certbot python3-certbot-nginx
# The nginx plugin IS wanted: the other vhosts on this box are already certbot-managed
# (`# managed by Certbot` in their files) and setup-server.sh follows that convention.

# 1b. Node >= 22.18 FOR THIS APP ONLY — see "Node version" below. Do not touch the
#     system node: PM2 spawns the other apps with whatever `node` is on PATH.
nvm install 22
sudo ln -sfn "$(nvm which 22)" /usr/local/bin/node22   # what ecosystem.config.cjs pins
nvm use 22                                             # for deploy.sh in this shell

# 2. secrets
sudo install -o "$PM2_USER" -g "$PM2_USER" -m 600 /dev/null /etc/voluntariat/env
sudoedit /etc/voluntariat/env         # contents below

# 3. code + first build. The script builds, migrates, flips `current`, and — because PM2
#    does not know the app yet — starts it rather than reloading it.
git clone <repo> /srv/voluntariat-dashboard/repo
cd /srv/voluntariat-dashboard/repo/web
./deploy/deploy.sh

# 4. the first coordinator — without this nobody can sign in at all
DATABASE_PATH=/srv/voluntariat-dashboard/data/voluntariat.db \
  node scripts/add-admin.ts you@example.cat --coordinator --name "Nom"

# 5a. make it survive a reboot (ONE TIME, and the script reminds you). `pm2 save` snapshots
#     every running app, so check the others are all `online` first or a reboot loses them.
pm2 list
pm2 save
# `pm2 startup` is already configured for the existing apps — do not re-run it blindly.

# 5b. proxy + TLS, in one idempotent step: checks DNS (A *and* AAAA), installs the vhost,
#     reloads nginx (never restarts — graceful for the other sites) and runs
#     `certbot --nginx --redirect`, which writes the TLS block the same way the neighbouring
#     vhosts have it. Re-running it later is a no-op that just re-verifies.
sudo ./deploy/setup-server.sh

# 5c. cron, as the PM2 user
crontab /srv/voluntariat-dashboard/current/deploy/crontab
```

### `/etc/voluntariat/env`

```sh
NODE_ENV=production
TZ=Europe/Madrid

AUTH_SECRET=              # openssl rand -base64 32
AUTH_GOOGLE_ID=
AUTH_GOOGLE_SECRET=
AUTH_URL=https://voluntariat.casavirupa.com
AUTH_TRUST_HOST=true

DATABASE_PATH=/srv/voluntariat-dashboard/data/voluntariat.db

FIREBASE_PROJECT_ID=voluntariat-casa-virupa
GOOGLE_APPLICATION_CREDENTIALS=/etc/voluntariat/firebase-sa.json

SYNC_KEY=                 # openssl rand -hex 32
```

Do **not** put `PORT` or `HOSTNAME` in this file. They come from the `env` block in
`ecosystem.config.cjs`, and the wrapper sources this file *afterwards* — so anything defined
here wins. A stray `PORT` would silently override the 3003 that nginx, cron and the deploy
health check all assume.

`GOOGLE_APPLICATION_CREDENTIALS` can be omitted if the PM2 user has gcloud ADC on the box
(`lib/firebase.ts` falls through to it), but that is a **user** refresh token: it dies after 7
days while the project's OAuth consent screen is in *Testing*, and the site keeps serving
happily with a stale mirror. A service-account key is the only thing that keeps working
unattended.

The Google OAuth client needs
`https://voluntariat.casavirupa.com/api/auth/callback/google` as an authorised redirect URI.
The Firebase service account needs only **Cloud Datastore User** (read/write on
`payments`; everything else the dashboard touches is read-only by choice, not by grant).

## Deploying a change

```bash
cd /srv/voluntariat-dashboard/repo && git pull
web/deploy/deploy.sh
```

The script refuses to start if the disk is low or if free RAM is under the build's heap cap
— a Next build is the most memory-hungry thing that happens on this box, and the OOM killer
does not necessarily pick the build. It then runs `npm ci`, the test suite and the build
(nice'd, `--max-old-space-size=1536`), assembles a release, applies migrations **before**
flipping the symlink, reloads only this PM2 app, polls `/login`, and rolls back to the
previous release if it does not come up. Old releases are pruned to the last five.

Rollback by hand:

```bash
ln -sfn /srv/voluntariat-dashboard/releases/<previous> /srv/voluntariat-dashboard/current
pm2 reload /srv/voluntariat-dashboard/current/deploy/ecosystem.config.cjs \
  --only voluntariat --update-env
```

A rollback does **not** undo a migration. Migrations here are additive; if one ever is not,
restore a backup instead.

## Node version

**>= 22.18, and it is a hard requirement.** Everything operational in this project is a
`.ts` file run directly by `node` — the test suite, `lib/db/migrate.ts`,
`scripts/add-admin.ts`, `scripts/opening-balance.ts` — and Node's native type stripping only
became unflagged in 22.18. On Node 20 every one of them dies with
`ERR_UNKNOWN_FILE_EXTENSION`, so migrations never run and the first coordinator can never be
created, i.e. **nobody can sign in**. `better-sqlite3` compounds it: its prebuilt binaries
are ABI-tagged for `>=22` and throw *"compiled against a different Node.js version"* at
require time otherwise.

The failure mode is nasty because Next itself declares `engines: >=20.9.0` — the site starts
and looks completely healthy while none of the above works.

`deploy.sh` refuses to run below 22.18, and `ecosystem.config.cjs` pins `/usr/local/bin/node22`
rather than resolving `node` from PATH. That pinning is the point on this box: the other apps
are spawned by the same PM2 daemon with whatever `node` it finds, so upgrading the *system*
node to satisfy this app could change the runtime under all of them at their next restart.
Install 22 for this user, symlink it, pin it here, leave the neighbours alone.

## Why one process

SQLite in WAL mode is safe with many readers and one writer *in one process*. Two Node
processes on the same file can interleave writes during a sync and corrupt it. So:
`exec_mode: 'fork'` with `instances: 1`, never `cluster`, never a second PM2 app on the same
database, and no `node server.js` in a shell "just to check something". The load is a dozen
coordinators; there is nothing to scale.

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
curl -fsS -X POST -H "X-Sync-Key: $SYNC_KEY" http://127.0.0.1:3003/api/sync | jq
curl -fsS -H "X-Sync-Key: $SYNC_KEY" http://127.0.0.1:3003/api/sync | jq   # status only
```

The header of every page shows how stale the mirror is: green, amber past 14 h, red past
26 h. Full history is at **Configuració → Sincronització**.

## Backups

One cron line, 03:20, in `deploy/crontab`: `sqlite3 .backup` → gzip → keep 7 days, in
`data/backups/`. It uses `.backup` and not `cp` because in WAL mode a plain copy of the `.db`
silently loses whatever is still in the `-wal`, which is precisely the recent data you would
want. Check it ran: `tail /srv/voluntariat-dashboard/data/backup.log`.

**Know what this does and does not cover.** It gets you out of a bad migration or a mistaken
bulk edit. It does nothing about losing the disk, because the copy is on the same disk. The
`fs_*` mirror is disposable — a full resync rebuilds it from Firestore — but `ledger_entry`,
`price_rule`, `commitment_rule` and `admin_user` are dashboard-owned and exist **nowhere
else**: no Firestore document, no export. If that file goes, every recorded payment and
adjustment is gone for good.

So either enable Hostinger's VPS snapshots, or add an offsite copy to the cron line:

```sh
rclone copy "$b/db-$d.db.gz" remote:voluntariat --quiet    # or: rsync -a … user@host:/path
```

Restore:

```bash
pm2 stop voluntariat
gunzip -c /srv/voluntariat-dashboard/data/backups/db-<YYYY-MM-DD>.db.gz > /tmp/restore.db
sqlite3 /tmp/restore.db 'PRAGMA integrity_check;'
mv /srv/voluntariat-dashboard/data/voluntariat.db /srv/voluntariat-dashboard/data/voluntariat.db.broken
mv /tmp/restore.db /srv/voluntariat-dashboard/data/voluntariat.db
chown "$PM2_USER" /srv/voluntariat-dashboard/data/voluntariat.db
pm2 start voluntariat
# then run a sync: the mirror will be a day stale, the ledger will not
```

## Going live: starting the books at zero

**Do this before showing `/coordinacio` to anyone.** It is the one step that is easy to skip
and confusing to explain afterwards.

Charges are derived from the mirror, so on day one the dashboard computes every meal and
overnight stay in the whole mirrored history — while the ledger, which is dashboard-owned
and brand new, is empty. Everyone therefore appears to owe months of meals they in fact paid
for in cash long ago. The number is arithmetically correct and factually nonsense.

Run these from the **source checkout** — the same directory you run `deploy.sh` from — not
from `current`. `scripts/*.ts` import `../lib/*.ts`, and the release only contains the
standalone bundle plus `lib/db/views.sql`, so `cd current && node scripts/…` fails on a
missing import. Set `DATABASE_PATH` explicitly, or `set -a; . /etc/voluntariat/env; set +a`
first, or they will happily create a brand new empty database next to the checkout.

```bash
set -a; . /etc/voluntariat/env; set +a

# 1. If the database still has demo/test data, clear it. --mirror is always safe: the
#    mirror is disposable and a sync rebuilds it. --ledger destroys money data that exists
#    nowhere else. Neither ever touches admin_user.
node scripts/reset.ts --all              # dry run: prints what it would delete
node scripts/reset.ts --all --yes

# 2. Rebuild the mirror from Firestore.
set -a; . /etc/voluntariat/env; set +a
curl -fsS -m 1800 -X POST -H "X-Sync-Key: $SYNC_KEY" 'http://127.0.0.1:3003/api/sync?full=1'

# 3. Record the historic debt as settled, as of the day the books start.
node scripts/opening-balance.ts --as-of 2026-09-01                  # dry run: the list
node scripts/opening-balance.ts --as-of 2026-09-01 --yes --by you@example.cat
```

Step 3 writes one `opening_balance` ledger entry per volunteer, dated the day *before* the
cut-off so it lands in the carry-over rather than in the first live period, and signed so
anyone genuinely in credit stays in credit. `external_ref` is unique, so running it twice is
a no-op rather than a double correction. Afterwards "Saldo anterior" reads 0 € for everyone
and the first period starts clean — with the historic figure recorded in the ledger, not
erased.

Pick the cut-off deliberately: charges from that date onwards are what the dashboard will
ask volunteers to pay.

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
| "Mai sincronitzat" in the header | `pm2 logs voluntariat --lines 100`; then Configuració → Sincronització for the error |
| Sync fails with `invalid_grant` | the service-account key, or the clock: `timedatectl` |
| Sync stuck as `running` | a dead run releases after 10 min via the heartbeat; the mutex is a partial unique index on `sync_run`, so it cannot double-run |
| "Protecció d'esborrat massiu" | a truncated Firestore read. Nothing was written. Re-run; if it repeats, the window really did lose bookings |
| Hours all zero | Configuració → Qualitat de dades → "Torns de 0 minuts". A high count means the `time_range` wire format is not what the parser expects |
| Numbers disagree with the app | expected: the app's Profile screen counts a flat 4 h per shift, the dashboard uses the real time range (as the app's own History screen does) |
| Nobody can sign in | `sqlite3 …/voluntariat.db 'select email, role, disabled_at from admin_user;'`, then `scripts/add-admin.ts` |
| 500 after a deploy | `ls -l /srv/voluntariat-dashboard/current`, `pm2 logs voluntariat`; roll the symlink back |
