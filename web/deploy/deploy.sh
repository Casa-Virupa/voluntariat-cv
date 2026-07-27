#!/usr/bin/env bash
#
# Builds a release into /srv/voluntariat/releases/<timestamp> and flips the `current`
# symlink. Run it on the VPS from a checkout of the repo:
#
#   cd /srv/voluntariat/repo && git pull && web/deploy/deploy.sh
#
# Two things this script exists to guarantee:
#
#   1. THE LIVE DATABASE IS NEVER INSIDE A RELEASE. It lives in /srv/voluntariat/data and is
#      reached through DATABASE_PATH. next.config.ts also excludes ./data from the build
#      trace, so even a mistaken `cp -r` of the source tree cannot ship a stale copy over
#      real data.
#   2. MIGRATIONS RUN BEFORE THE SWITCH, so the new code never briefly serves an old schema.

set -euo pipefail

ROOT="${VOLUNTARIAT_ROOT:-/srv/voluntariat}"
WEB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RELEASE="$ROOT/releases/$(date +%Y%m%d-%H%M%S)"
DATA_DIR="$ROOT/data"
KEEP_RELEASES="${VOLUNTARIAT_KEEP_RELEASES:-5}"

echo "==> building in $WEB_DIR"
cd "$WEB_DIR"

# `npm ci` and not `npm install`: a deploy must not resolve a different dependency tree than
# the one that was tested.
npm ci
npm test
npm run build

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

echo "==> switching current -> $RELEASE"
ln -sfn "$RELEASE" "$ROOT/current.new"
mv -Tf "$ROOT/current.new" "$ROOT/current"

echo "==> restarting"
sudo systemctl restart voluntariat
sleep 3
sudo systemctl is-active --quiet voluntariat && echo "    service is up" || {
  echo "    SERVICE FAILED — rolling back"
  ln -sfn "$(ls -1dt "$ROOT"/releases/*/ | sed -n 2p)" "$ROOT/current"
  sudo systemctl restart voluntariat
  exit 1
}

# Reaching the login page proves the process is serving, not merely running.
curl -fsS -o /dev/null http://127.0.0.1:3000/login && echo "    /login responds"

echo "==> pruning old releases (keeping $KEEP_RELEASES)"
ls -1dt "$ROOT"/releases/*/ | tail -n "+$((KEEP_RELEASES + 1))" | xargs -r rm -rf

echo "done."
