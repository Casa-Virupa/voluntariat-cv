# Voluntariat — admin dashboard

Next.js 16 dashboard for the Casa Virupa coordination team. It mirrors the mobile app's
Firestore into a local SQLite database and renders the coordination, calendar and
configuration views on top of that mirror. Catalan UI. Self-hosted on a VPS.

**Agent/contributor rules live in [`AGENTS.md`](./AGENTS.md)** (loaded automatically by
Claude Code via `CLAUDE.md`) — read it before touching code: it lists the data rules
(integer cents, half-open ranges, mirror vs. dashboard-owned tables…) that the schema
depends on. Deployment and operations are documented in
[`deploy/README.md`](./deploy/README.md).

## Local development

Requires **Node >= 22.18** (`.ts` files are executed directly by `node`).

```bash
npm install
npm run db:migrate     # migrations + views + seeds (idempotent)
npm run seed:demo      # fake mirror data — no Firebase credentials needed
node scripts/add-admin.ts you@example.com --coordinator
npm run dev            # http://localhost:3000
npm test               # node --test over lib/**/*.test.ts
```

To sync against the real (dev) Firestore instead of demo data, set up gcloud ADC first —
see "Before anything else" in `deploy/README.md`.

## Scripts

| Command | What it does |
| --- | --- |
| `npm run db:generate` | new migration after editing `lib/db/schema.ts` |
| `npm run db:migrate` | apply migrations + views + seeds |
| `npm run seed:demo` | fake mirror data (`--force` to wipe) |
| `npm run reset` | clear mirror and/or ledger (dry run by default) |
| `npm run opening-balance` | settle historic debt at go-live |
| `node scripts/add-admin.ts <email> --coordinator` | create/promote a dashboard admin |
