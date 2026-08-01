#!/usr/bin/env bash
#
# PM2's entry point for the app — see deploy/ecosystem.config.cjs, which points `script` here.
#
# This exists because PM2 has no systemd-style `EnvironmentFile=`, and the secrets deliberately
# live in /etc/voluntariat/env (outside any release, so a deploy cannot overwrite them and a
# rollback cannot resurrect an old copy). Something has to source that file before node starts,
# and it cannot be an inline `bash -c` in the ecosystem file: PM2 resolves `script` as a path
# relative to `cwd` before it ever looks at `interpreter`, so `script: '-c'` fails with
# "Script not found: …/current/-c".

set -euo pipefail

# Resolve the release root from this script's own location instead of trusting the inherited
# cwd, so it behaves the same when run by hand for debugging.
cd "$(dirname "${BASH_SOURCE[0]}")/.."

# Captured BEFORE the env file is sourced. `set -a` exports everything in that file over the
# top of PM2's env, so a stray NODE_BIN there must not get to decide which node runs the app.
# Node >= 22.18 is a hard requirement — see "Node version" in deploy/README.md.
NODE_BIN="${NODE_BIN:-/usr/local/bin/node22}"

if [[ ! -x "$NODE_BIN" ]]; then
  echo "start.sh: $NODE_BIN is missing or not executable." >&2
  echo "start.sh:   nvm install 22 && sudo ln -sfn \"\$(nvm which 22)\" /usr/local/bin/node22" >&2
  exit 1
fi

# Fail loudly rather than booting with no AUTH_SECRET and no DATABASE_PATH, which would start
# a superficially healthy server pointed at the wrong (or a brand new, empty) database.
ENV_FILE="${VOLUNTARIAT_ENV_FILE:-/etc/voluntariat/env}"
if [[ ! -r "$ENV_FILE" ]]; then
  echo "start.sh: $ENV_FILE is missing or unreadable by $(whoami)." >&2
  echo "start.sh: it must be chmod 600 and owned by the user that owns the PM2 daemon." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
. "$ENV_FILE"
set +a

# A relative DATABASE_PATH resolves against the cwd set above — i.e. INSIDE the release — so
# the app silently opens a brand new empty database, and the next deploy throws it away. The
# symptom is not an error but a working site where nobody can sign in, because the allowlist
# it is reading is a different (empty) file from the one the migrations ran against.
if [[ "${DATABASE_PATH:-}" != /* ]]; then
  echo "start.sh: DATABASE_PATH must be absolute, got '${DATABASE_PATH:-<unset>}'." >&2
  echo "start.sh: fix it in $ENV_FILE:" >&2
  echo "start.sh:   DATABASE_PATH=/srv/voluntariat-dashboard/data/voluntariat.db" >&2
  exit 1
fi

# The standalone build has its own entry point; `next start` is not used and the full
# node_modules tree is not shipped.
exec "$NODE_BIN" server.js
