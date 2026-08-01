#!/usr/bin/env bash
#
# Builds a release into /srv/voluntariat-dashboard/releases/<timestamp>, flips the `current`
# symlink and reloads the PM2 app. Run it on the VPS, as the user that owns PM2 (NOT with
# sudo — that would talk to root's PM2 daemon, which is a different process list):
#
#   cd /srv/voluntariat-dashboard/repo && git pull && web/deploy/deploy.sh
#
# Three things this script exists to guarantee:
#
#   1. THE LIVE DATABASE IS NEVER INSIDE A RELEASE. It lives in
#      /srv/voluntariat-dashboard/data and is reached through DATABASE_PATH. next.config.ts
#      also excludes ./data from the build trace, so even a mistaken `cp -r` of the source
#      tree cannot ship a stale copy over real data.
#   2. MIGRATIONS RUN BEFORE THE SWITCH, so the new code never briefly serves an old schema.
#   3. NOTHING HERE TOUCHES THE OTHER APPS ON THIS BOX. This VPS runs several other Next
#      servers under the same PM2 daemon and behind the same nginx. Every PM2 verb below is
#      scoped with `--only`, every destructive path is under $ROOT, and the build is capped
#      so it cannot get a neighbour OOM-killed. Do not add `pm2 reload all`,
#      `pm2 restart all`, `pm2 update`, `pm2 kill` or `pm2 flush` — each of those cycles or
#      truncates every app on the machine. `pm2 save` is deliberately absent too: it
#      re-snapshots the whole process list, so running it here while another app happened
#      to be stopped would quietly drop that app from the boot list.

set -euo pipefail

ROOT="${VOLUNTARIAT_ROOT:-/srv/voluntariat-dashboard}"
WEB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RELEASE="$ROOT/releases/$(date +%Y%m%d-%H%M%S)"
DATA_DIR="$ROOT/data"
KEEP_RELEASES="${VOLUNTARIAT_KEEP_RELEASES:-5}"
APP_NAME="${VOLUNTARIAT_PM2_NAME:-voluntariat}"
PORT="${VOLUNTARIAT_PORT:-3003}"

# A Next build peaks well over a gigabyte. On a box already holding several resident Next
# servers the kernel's OOM killer scores by footprint and may pick a NEIGHBOUR rather than
# the build, so cap the heap and run the build nice'd. 3072 is measured, not generous:
# Next 16's TypeScript pass alone OOMs a 1536 MB heap on this project. The box needs swap
# for this to be safe next to the resident apps — see free -h before lowering it.
BUILD_HEAP_MB="${VOLUNTARIAT_BUILD_HEAP_MB:-3072}"

# --- preflight ----------------------------------------------------------------------
#
# Both of these fail the whole server, not just this app: a full disk stops every nginx log
# and every SQLite write on the box, and a build that swaps the machine to death takes the
# neighbours with it. Warn loudly rather than discover it halfway through.

mkdir -p "$ROOT/releases" "$DATA_DIR"

# Node 24 exactly-this-major, not a recommendation. The runtime is pinned to node 24
# (NODE_BIN in ecosystem.config.cjs — node 22.23.x crashes importing firebase-admin), and
# the BUILD must match it: better-sqlite3 picks its native binary at `npm ci` time for the
# node running the install, so a build under 22 produces a release the runtime under 24
# refuses to load. Node 20 is doubly out: every operational .ts here (tests, migrations,
# add-admin.ts) relies on native type stripping, which 20 lacks.
node_major="$(node -p 'process.versions.node.split(".")[0]' 2>/dev/null || echo 0)"
if (( node_major != 24 )); then
  echo "!!! node $(node -v 2>/dev/null || echo 'not found') — this deploy needs node 24,"
  echo "!!! the same major the app runs on (NODE_BIN in deploy/ecosystem.config.cjs),"
  echo "!!! or better-sqlite3's native binary will not match the runtime."
  echo "!!! Do NOT upgrade the system node: the other apps on this VPS are spawned by PM2"
  echo "!!! with whatever node is on PATH. For this shell only:"
  echo "!!!   nvm install 24 && nvm use 24"
  exit 1
fi

if ! command -v pm2 >/dev/null 2>&1; then
  echo "!!! pm2 is not on PATH for $(whoami). Run this as the user that owns the PM2"
  echo "!!! daemon — the one whose \`pm2 list\` shows the other apps on this box."
  exit 1
fi

avail_mb="$(df -Pm "$ROOT" 2>/dev/null | awk 'NR==2 {print $4}' || echo 0)"
if [[ "$avail_mb" -lt 2048 ]]; then
  echo "!!! only ${avail_mb} MB free on $ROOT — a release plus node_modules needs more."
  echo "!!! prune releases (VOLUNTARIAT_KEEP_RELEASES) or backups before deploying."
  exit 1
fi

# A warning, not a gate, and deliberately not a prompt: this script has to stay runnable
# non-interactively (over ssh without a tty, from cron, under nohup), and a `read` here would
# hang such a run forever with no output explaining why.
free_mb="$(free -m 2>/dev/null | awk '/^Mem:/ {print $7}' || echo '')"
if [[ -n "$free_mb" && "$free_mb" -lt "$BUILD_HEAP_MB" ]]; then
  echo "!!! ${free_mb} MB available RAM, build heap cap is ${BUILD_HEAP_MB} MB."
  echo "!!! building now risks the OOM killer taking one of the other apps on this VPS."
  echo "!!! lower VOLUNTARIAT_BUILD_HEAP_MB, add swap, or deploy at a quieter moment."
  echo "!!! continuing anyway in 10s — Ctrl-C to stop."
  sleep 10
fi

# Is this the very first deploy? Recorded now, acted on after the build: on a first run the
# app has to be *started*, and it cannot be started before a release exists for it to run.
# `pm2 save` stays manual either way — see the end of the script.
FIRST_RUN=false
pm2 describe "$APP_NAME" >/dev/null 2>&1 || FIRST_RUN=true

echo "==> building in $WEB_DIR (heap cap ${BUILD_HEAP_MB} MB)"
cd "$WEB_DIR"

# `npm ci` and not `npm install`: a deploy must not resolve a different dependency tree than
# the one that was tested.
nice -n 10 npm ci

# The test suite is NOT run here. It belongs where a failure is cheap and someone is watching
# — before you push — not two minutes into a deploy on the live box, where the only available
# reaction is to skip it. Run `npm test` locally, or set VOLUNTARIAT_RUN_TESTS=1 to include it.
if [[ "${VOLUNTARIAT_RUN_TESTS:-}" == 1 ]]; then
  npm test
fi

NODE_OPTIONS="--max-old-space-size=$BUILD_HEAP_MB" nice -n 10 npm run build

echo "==> assembling $RELEASE"
mkdir -p "$RELEASE" "$DATA_DIR"

# The standalone output is self-contained apart from the two asset directories Next expects
# to find next to it.
cp -r .next/standalone/. "$RELEASE/"
mkdir -p "$RELEASE/.next"
cp -r .next/static "$RELEASE/.next/static"
[[ -d public ]] && cp -r public "$RELEASE/public"

# Migrations, the seeds and the deploy scripts are not part of the standalone bundle.
mkdir -p "$RELEASE/drizzle" "$RELEASE/lib/db" "$RELEASE/deploy"
cp -r drizzle/. "$RELEASE/drizzle/"
cp lib/db/views.sql "$RELEASE/lib/db/views.sql"
cp -r deploy/. "$RELEASE/deploy/"
chmod +x "$RELEASE/deploy/"*.sh

echo "==> migrating (idempotent: migrations + views + seeds)"
set -a
# shellcheck disable=SC1091
. /etc/voluntariat/env
set +a
DATABASE_PATH="${DATABASE_PATH:-$DATA_DIR/voluntariat.db}" node "$WEB_DIR/lib/db/migrate.ts"

# The release to fall back to, resolved BEFORE the flip: the newest one that is not the one
# being deployed. Empty on a first deploy, which is why the failure path below checks.
PREVIOUS="$(ls -1dt "$ROOT"/releases/*/ 2>/dev/null | grep -v "^$RELEASE/\?$" | head -n 1 || true)"

echo "==> switching current -> $RELEASE"
ln -sfn "$RELEASE" "$ROOT/current.new"
mv -Tf "$ROOT/current.new" "$ROOT/current"

# Always go through the ecosystem file rather than a bare app name, so a changed port, env
# or cwd in the new release is actually picked up; `--only` keeps it to this app and leaves
# the other PM2 processes on this box completely alone. In fork mode a reload is a graceful
# restart, and kill_timeout in the ecosystem file gives an in-flight sync time to finish its
# transaction. `start` is only reached the first time, when there is nothing to reload yet.
reload_app() {
  local ecosystem="$ROOT/current/deploy/ecosystem.config.cjs"
  if pm2 describe "$APP_NAME" >/dev/null 2>&1; then
    pm2 reload "$ecosystem" --only "$APP_NAME" --update-env
  else
    pm2 start "$ecosystem" --only "$APP_NAME"
  fi
}

# Reaching the login page proves the process is serving, not merely running. Next needs a
# moment to boot, so poll rather than sleeping a fixed guess.
wait_until_serving() {
  for _ in $(seq 1 20); do
    if curl -fsS -o /dev/null -m 3 "http://127.0.0.1:$PORT/login"; then return 0; fi
    sleep 1
  done
  return 1
}

if [[ "$FIRST_RUN" == true ]]; then
  echo "==> starting PM2 app '$APP_NAME' for the first time"
else
  echo "==> reloading PM2 app '$APP_NAME'"
fi
reload_app

if wait_until_serving; then
  echo "    /login responds on 127.0.0.1:$PORT"
else
  echo "    DEPLOY FAILED — /login did not respond"
  if [[ -n "$PREVIOUS" ]]; then
    echo "    rolling back to $PREVIOUS"
    ln -sfn "${PREVIOUS%/}" "$ROOT/current"
    reload_app
    wait_until_serving && echo "    rolled back and serving" \
      || echo "    ROLLBACK ALSO FAILED — check: pm2 logs $APP_NAME --lines 100"
  else
    echo "    no previous release to roll back to — check: pm2 logs $APP_NAME --lines 100"
  fi
  exit 1
fi

echo "==> pruning old releases (keeping $KEEP_RELEASES)"
ls -1dt "$ROOT"/releases/*/ | tail -n "+$((KEEP_RELEASES + 1))" | xargs -r rm -rf

echo "done."

# Deliberately not run automatically, and only relevant once: `pm2 save` rewrites the
# resurrect list from whatever is running AT THAT MOMENT, across every app on this box. Run
# from a deploy it would silently drop any neighbour that happened to be stopped, so it is
# left to a human who can first check that `pm2 list` is all green.
if [[ "$FIRST_RUN" == true ]]; then
  echo
  echo "!!! '$APP_NAME' is new to PM2 and will NOT survive a reboot until you run:"
  echo "!!!   pm2 list      # confirm every other app on this box is 'online' first"
  echo "!!!   pm2 save"
fi
