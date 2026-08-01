// PM2 process definition — the counterpart to deploy/nginx.conf.
//
// This PM2 already runs other apps. Everything below is additive and scoped by name:
//
//   ss -ltnp | grep :3003        # FIRST: is 3003 free? see the PORT note below
//   pm2 list                     # and is the name `voluntariat` free?
//   pm2 start /srv/voluntariat-dashboard/current/deploy/ecosystem.config.cjs
//   pm2 save                     # snapshots EVERY running app, not just this one
//   pm2 reload voluntariat       # after a deploy flipped `current` — never `reload all`
//
// `pm2 save` rewrites the resurrect list from whatever is running at that moment, so make
// sure the other apps are up (`pm2 list` — all `online`) before saving, or a reboot will
// bring back a list that is missing them. `pm2 startup` is presumably already configured
// for the existing apps; re-running it is harmless but unnecessary.
//
// ONE PROCESS, DELIBERATELY. SQLite in WAL mode tolerates many readers and one writer in a
// single process; two Node processes on the same file (cluster mode, a second app entry, a
// stray `node server.js` in a shell) can interleave writes during a sync and corrupt the
// database. `exec_mode: 'fork'` + `instances: 1` is not a starting point to scale from —
// scaling means moving to Postgres first. The load is a dozen coordinators.
//
// Secrets stay in /etc/voluntariat/env, outside any release, so a deploy cannot overwrite
// them and a rollback cannot resurrect an old copy. PM2 has no EnvironmentFile= equivalent,
// so deploy/start.sh sources the file and then execs node.
//
// It has to be a real script file, not an inline `bash -c`: PM2 resolves `script` as a path
// relative to `cwd` BEFORE it looks at `interpreter`, so `interpreter: '/bin/bash'` with
// `script: '-c'` dies with "Script not found: /srv/voluntariat-dashboard/current/-c".

// THIS APP NEEDS NODE 24 (current LTS). Node >= 22.18 was the old floor (native .ts type
// stripping; ABI-tagged better-sqlite3 prebuilds), but 22 is now EXCLUDED outright:
// node 22.23.x dies with ERR_INTERNAL_ASSERTION when the Turbopack runtime imports
// firebase-admin as an external module (verified 2026-08-01 — the same standalone build
// runs fine on 24). The other apps on this VPS may well be on an older node, and PM2
// spawns each app with whatever `node` resolves at that moment — so pin an absolute path
// here rather than upgrading the system node out from under the neighbours.
//
//   nvm install 24
//   sudo ln -sfn "$(nvm which 24)" /usr/local/bin/node24    # stable across nvm upgrades
//
// An nvm path like /home/user/.nvm/versions/node/v24.x.y/bin/node works too, but bakes in a
// patch version that a later `nvm install` will orphan. Passed to start.sh as NODE_BIN.
//
// The BUILD must run under node 24 as well (`nvm use 24` before deploy.sh): better-sqlite3
// picks its native binary at `npm ci` time for the node that runs the install, and an
// install under 22 produces a binary the runtime under 24 refuses to load.
const NODE_BIN = '/usr/local/bin/node24'

module.exports = {
  apps: [
    {
      name: 'voluntariat',
      // `current` is a symlink to the active release; PM2 resolves it at (re)start, so a
      // rollback is one `ln -sfn` + `pm2 reload`.
      cwd: '/srv/voluntariat-dashboard/current',

      // Absolute path deliberately: `current` is where cwd points anyway, and an absolute
      // script is one less thing for PM2 to resolve. `interpreter: 'bash'` means the exec
      // bit and the shebang do not both have to survive the deploy's `cp -r`.
      script: '/srv/voluntariat-dashboard/current/deploy/start.sh',
      interpreter: 'bash',

      exec_mode: 'fork',
      instances: 1,

      // Everything here is overridable by /etc/voluntariat/env, because start.sh sources that
      // file AFTER PM2 has applied this block. That is fine for NODE_ENV and TZ; it is a trap
      // for PORT, so do not define PORT in the env file.
      env: {
        NODE_BIN,
        NODE_ENV: 'production',
        // Madrid in the process too, not only in cron: `service_date`, the done/pending
        // split and every "which month is this" decision are computed here.
        TZ: 'Europe/Madrid',
        // If another PM2 app already holds 3003, change it here AND in three other
        // places, or the sync and the deploy health check will hit the wrong app:
        // the `upstream voluntariat_app` in deploy/nginx.conf, the two curls in
        // deploy/crontab, and the /login check in deploy/deploy.sh.
        PORT: '3003',
        // Bind loopback only — nginx is the sole way in.
        HOSTNAME: '127.0.0.1',
      },

      autorestart: true,
      restart_delay: 3000,
      max_restarts: 10,
      // The sync can hold a write transaction for a few seconds; never kill mid-transaction.
      kill_timeout: 30000,

      out_file: '/srv/voluntariat-dashboard/data/pm2-out.log',
      error_file: '/srv/voluntariat-dashboard/data/pm2-error.log',
      merge_logs: true,
      time: true,
    },
  ],
}
