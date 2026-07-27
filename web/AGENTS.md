<!-- BEGIN:nextjs-agent-rules -->
# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing any code. Heed deprecation notices.
<!-- END:nextjs-agent-rules -->

# Voluntariat admin dashboard

Reads the mobile app's Firestore into a local SQLite mirror and renders the coordination
views. Self-hosted on a VPS. Catalan UI, no i18n framework.

## Things already learned the hard way

- **Next 16 renamed `middleware.ts` to `proxy.ts`** (named export `proxy`, nodejs runtime
  only). The proxy here is an optimistic cookie check, *not* access control — real
  authorisation is `requireAdmin()` / `visibleAreas()` in `lib/authz.ts`, per request.
- **`cookies()`, `headers()`, `params` and `searchParams` are async.** Await them.
- **`unauthorized()` and `forbidden()` are canary-only.** Use `redirect()`.
- **`lib/` runs under Node's strip-only TypeScript**, because `npm test` executes the
  pure modules directly with `node --test`. That means **no TS parameter properties, no
  `enum`, no namespaces** in anything `lib/` imports — declare fields explicitly instead.
  Relative imports inside `lib/` need explicit `.ts` extensions for the same reason
  (`allowImportingTsExtensions` is on; Turbopack resolves them fine).
- **Never let the tracer bundle `data/`** — `outputFileTracingExcludes` keeps the live
  SQLite file out of the standalone artefact so a deploy cannot overwrite real data.
- **`react-hooks/purity` rejects `Date.now()` inside a component**, even an async server
  one. Read the clock through `nowSeconds()` / `todayInMadrid()` in `lib/dates.ts`, which is
  where "what day is it" should be defined anyway so the views cannot disagree.
- **Filter state belongs in the URL, not in component state.** `lib/query/filters.ts` parses
  and rebuilds it; every filtered view is then bookmarkable, and `/api/export` is the same
  query string with a different path. The only client components are `AutoSubmitForm` (so a
  `<select>` applies without a button) and `SyncButton`.
- **The palette and the two faces come from the mobile app**, not from Tailwind's defaults:
  `app/globals.css` mirrors `shared/designsystem/.../theme/Colors.kt`, and Kalice (display) +
  DM Sans (text) are self-hosted from `app/fonts` — the same files the app ships. Use the
  semantic tokens (`brand-*`, `canvas`, `surface`, `line`, `ink*`, `ok`/`warn`/`bad`); a raw
  `slate-*` or `amber-*` utility in a view means a cool grey leaked back in. The identity
  orange (`brand-500`) is too light to carry white text, so text-bearing fills use
  `brand-700`.
- **Server actions carry their outcome in a redirect**, not in component state: they
  authorise, parse, call `lib/mutations.ts`, then redirect back with `?ok=` or `?error=`.
  A `returnTo` field brings the coordinator back to the same period and open panel — it is
  validated as a same-site path, since a redirect that accepted an absolute URL would be an
  open redirect.

## Non-negotiable data rules

- Money is **integer cents**, durations **integer minutes**, dates TEXT `YYYY-MM-DD`,
  instants integer unix **seconds**. Format only at the edge. Ranges are half-open.
- `volunteers.timestamp` is midnight **in the booker's device timezone**, not UTC. Always
  derive the day with `serviceDate()` from `lib/dates.ts`. Reading it as a UTC date shifts
  every booking back a day and moves the 1st of the month into the previous month.
- Attribute hours to an area via `fs_booking_shift.area` (frozen at booking time), never
  via the user's current `specific_areas`.
- `fs_*` tables are the mirror: only the sync writes them, a full resync rebuilds them,
  and they must never hold a foreign key to a dashboard-owned table.
- Charges are **derived** from the mirror plus the dated price table, never stored. That
  is what makes a re-sync idempotent and a cancellation self-correcting.
- Counting people or active days uses `COUNT(DISTINCT …)`: `(user_id, service_date)` is
  **not** unique, the app's duplicate guard is client-side only.
- **`ledger_entry` is insert-only**, and dated rules (`price_rule`, `commitment_rule`) are
  never edited in place: superseding one closes it with `valid_to` and inserts a new row.
  Both properties are what stop last month's books changing under a coordinator's feet.
  Every dashboard-owned write goes through `lib/mutations.ts` and writes an `audit_log` row.
- **Positive = credit** in the ledger: it reduces what the volunteer owes. A negative
  adjustment is an extra charge.
- **An area scope is a WHERE clause, not a hidden column.** Same for the payments columns,
  which an `area_responsible` must not receive from the page *or* the export.

## Commands

    npm run dev            # dev server
    npm test               # node --test over lib/**/*.test.ts
    npm run db:migrate     # migrations + views + seeds (idempotent)
    npm run db:generate    # new migration after editing lib/db/schema.ts
    npm run seed:demo      # fake mirror data, no Firebase credentials needed (--force to wipe)
    node scripts/add-admin.ts <email> --coordinator
