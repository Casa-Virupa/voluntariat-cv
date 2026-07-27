# Handoff — Voluntariat admin dashboard (`web/`)

**Status:** feature-complete. Both views, the config page, the Excel export, the payments
write-back and the deploy setup are built and verified against seeded data. What remains is
**verification against real Firestore**, which is blocked on credentials (§7).

Read `web/AGENTS.md` first — it holds the framework gotchas and the non-negotiable data
rules. This file is the project state.

Reference documents:

| What | Where |
| --- | --- |
| Approved implementation plan | `~/.claude/plans/binary-wishing-hejlsberg.md` |
| Framework + data conventions | `web/AGENTS.md` |
| Deployment runbook | `web/deploy/README.md` |
| Mobile app architecture | `CLAUDE.md` (repo root) — **its Firebase section is out of date, see §3** |

---

## 1. What this is

Casa Virupa's volunteer data lives in Firestore and is only visible through the mobile app,
one volunteer at a time. This dashboard gives coordinators a read-oriented view over the
same data, plus the two things the app has never modelled: **per-area hour commitments**
and a **real payment ledger** (dated prices, per-item detail, manual adjustments,
carry-over balance).

It mirrors Firestore into local SQLite twice a day plus on demand, and is read-only on app
data with exactly one exception: the payments write-back (§5.4).

---

## 2. Decisions already taken — do not relitigate

| Decision | Value |
| --- | --- |
| Location | `web/` in the mobile repo. Not a Gradle module; never affects the app build |
| Hosting | Hostinger VPS, Next.js standalone behind nginx, **single process** |
| Database | SQLite (`better-sqlite3`, WAL) + Drizzle |
| Auth | Auth.js v5 + Google, allowlist table carrying a role and area scope |
| Firebase | `firebase-admin` service account, read-only except the payments write-back |
| Export | `.xlsx` with **live formulas** (ExcelJS), not baked-in numbers |
| Periods | Monthly + quarterly selector. Habitual = 8h/month, Mitra = 60h/**quarter** |
| Language | Catalan only, strings inline, no i18n framework |
| Commitments | Dashboard-owned, per volunteer, per area, dated |
| Prices | Dashboard-owned table keyed on (item × volunteer type × member), dated |
| Filter state | Lives in the URL. Every view is a pure function of its query string |

---

## 3. The Firestore contract (verified against source — trust this over `CLAUDE.md`)

`CLAUDE.md` documents a `reservations` collection with a `FirebaseReservation` shape.
**That is wrong.** The collection is `volunteers`, and a `payments` collection also exists.

### `users/{uid}` — doc id is the Firebase Auth uid
`shared/data/.../data/FirestoreModels.kt:6`

`id`, `name`, `email`, `role` (`volunteer` | `area_responsible` | `coordination_team`),
`volunteer_type` (`habitual` | `mitra`, nullable), `onboarding_completed`,
`specific_areas` (array of 21 area codes), `is_member`.

### `volunteers/{autoId}` — one doc = one volunteer's booking for ONE day
`shared/data/.../repositories/requests/FirebaseVolunteer.kt:9`

```
user_id     string       FK to users doc id — the ONLY link, no name is denormalised
timestamp   Timestamp    the booked DATE at midnight in the BOOKER'S DEVICE ZONE (see §4)
shifts      array 1–2    { shift: "morning"|"afternoon",
                           time_range: { start: LocalTime, end: LocalTime },
                           type: { type: "general"|"specific",
                                   specific_areas: string|null } }
meal_types  array        only "lunch" / "dinner" are ever written; breakfast never is
sleep       boolean
```

Morning and afternoon can carry **different areas**. `specific_areas` is singular-valued
despite the plural name. Docs are created with `.add()` and **never updated** — an edit is
delete + recreate.

### `payments/{autoId}` — one logical doc per (user, year, month)
`shared/data/.../repositories/requests/FirebasePayments.kt:7`

`user_id`, `year`, `month`, `paid`, `amount`.

### Enum strings
Exhaustive lists live in `FirebaseAuthRepository.kt:104-150` and
`FirebaseVolunteerRepository.kt:157-266`. They are mirrored in `web/lib/contract.ts` and
**also** in `tools/users/add-user.mjs`. Three copies, nothing enforcing agreement — adding
a `SpecificArea` in Kotlin means editing all three or it surfaces as `__unknown__` (and
shows up in Configuració → Qualitat de dades).

---

## 4. The five traps that produce wrong numbers

Every one of these is verified in the Kotlin source. Do not "simplify" past them.

1. **`timestamp` is midnight in the booker's device timezone**, not UTC
   (`DateTimeUtils.kt:34` uses `TimeZone.currentSystemDefault()`). A Spanish booking for
   1 May is stored as `2026-04-30T22:00Z`. Reading it as a UTC date shifts every booking
   back a day and moves month-boundary bookings into the wrong month — and the totals
   still look plausible. Always derive the day with `serviceDate()` from `lib/dates.ts`
   (shift +12h, then take the Europe/Madrid date). Pinned by tests.

2. **Hours are computed two different ways in the app.** `Volunteer.calculateHours()`
   (`Volunteer.kt:22,50`) returns a flat **4h per shift** and drives the Profile progress
   bar. `HistoryViewModel.getTotalHour()` (`HistoryViewModel.kt:237-264`) uses the **real
   `end - start`**. The dashboard uses the real range and says so on both views ("hores
   reals"). **Expect coordinators to notice the dashboard disagreeing with a volunteer's
   Profile screen.** The proper fix is a mobile change aligning Profile to History.

3. **`payments.amount` is corrupt by construction.** `addPayment`
   (`FirebasePaymentRepository.kt:46-84`) increments on every booking, is never decremented
   when a booking is deleted, and `pay()` only flips `paid=true` without clearing the
   amount — so "paid while still owing" is reachable today. Never treat it as input; it is
   mirrored for forensics only, and surfaced as a `≠ app` badge on /coordinacio.

4. **`(user_id, service_date)` is not unique.** The duplicate-booking guard is client-side
   only (`ReservationFormViewModel.kt:502`). `COUNT(DISTINCT …)` is used for the "persones"
   and "dies amb activitat" tiles, and the coordination table groups by user so a double
   booking counts as one person with their minutes summed.

5. **Area attribution comes from the shift, not the user.** The form silently falls back to
   `user.specific_areas.first()` when the picker is hidden
   (`ReservationFormViewModel.kt:457`), and a user's areas change over time. Everything
   groups by `fs_booking_shift.area` (frozen at booking), never by `fs_user_area`.

Also: `addPayment` is a non-transactional read-modify-write, so **duplicate payment docs**
for one (user, year, month) are reachable. The app reads `.firstOrNull()` = `MIN(doc_id)`,
which is what `v_payment_canonical` reproduces and what write-back targets.

---

## 5. What is built

### File map

```
web/
  AGENTS.md              framework gotchas + data rules — READ FIRST
  HANDOFF.md             this file
  proxy.ts               Next 16 middleware. Optimistic cookie check ONLY, not authz
  next.config.ts         standalone output, native externals, data/ excluded from tracing

  lib/
    contract.ts          Firestore enum strings + Catalan labels + app defaults
    dates.ts             serviceDate(), periods, shift minutes, formatting  [PURE, TESTED]
    commitments.ts       rule precedence, proration, status bands           [PURE, TESTED]
    ledger.ts            balance arithmetic, euro parsing, settlement       [PURE, TESTED]
    export.ts            ExcelJS workbook — live formulas, three sheets
    writeback.ts         reconciliation + the gated Firestore write-back
    mutations.ts         EVERY write to a dashboard-owned table, with the rules
    settings.ts          app_setting access + audit_log helper
    admins.ts authz.ts auth.ts firebase.ts
    db/                  schema.ts (16 tables), views.sql, migrate.ts, index.ts
    sync/                parse.ts [PURE, TESTED], run.ts, anomalies.ts
    query/
      filters.ts         URL <-> filter state                              [PURE, TESTED]
      calendar.ts        month grid, summary, day detail, grid geometry
      coordination.ts    the big table, per-volunteer detail, closed periods
      config.ts          rule tables, allowlist, sync history, data quality
      sync-status.ts     header freshness (no firebase-admin import)

  app/
    (dash)/layout.tsx            header, nav, freshness indicator, sync button
    (dash)/_components/          ui.tsx, AutoSubmitForm.tsx, SyncButton.tsx
    (dash)/calendari/            VIEW 1
    (dash)/coordinacio/          VIEW 2 + actions.ts
    (dash)/configuracio/         6 sections + actions.ts (coordinator only)
    api/{auth,sync,export}/
  scripts/
    add-admin.ts         allowlist CLI (bootstrap: no way to sign in without it)
    seed-demo.ts         fake mirror data through the REAL parser + applyAll
  deploy/                systemd, nginx, crontab, backup.sh, deploy.sh, README.md
```

### 5.1 Database zones

`fs_*` is the **mirror**: disposable, rebuilt by a full resync, written only by the sync
(and by `scripts/seed-demo.ts`, which exists so the views can be verified without
credentials), and never holding a foreign key to a dashboard table. Everything else is
**owned** by the dashboard and must survive a resync: `admin_user`, `admin_user_area`,
`commitment_rule`, `price_rule`, `ledger_entry`, `period_close`, `charge_locked`,
`sync_run`, `writeback_log`, `app_setting`, `audit_log`.

### 5.2 The ledger model

```
charges     = derived from the mirror × the dated price table   -- NEVER stored
adjustments = Σ ledger_entry(kind='adjustment')                 -- insert-only
paid        = Σ ledger_entry(kind='payment')                    -- insert-only
owed        = charges − Σ ledger                                -- computed
carry-in    = the same sum over everything before the period    -- shown as its own line
```

Charges key on `(doc_id, item)` from the mirror, so a re-sync cannot double-count and a
cancelled booking stops being charged automatically. `ledger_entry` is **insert-only**:
corrections are new rows with `voids_id` set, never edits. Sign convention: **positive =
credit** (reduces what the volunteer owes).

Closing a period snapshots its charges into `charge_locked`; `v_charge_effective` then
prefers the snapshot, so a later price correction cannot rewrite an invoice that has
already gone out. Verified: with July closed for one volunteer and a retroactive lunch
price change, that volunteer's total held at 74,00 € while an open volunteer's moved.

### 5.3 Commitment resolution (`lib/commitments.ts`)

Most specific rule wins (`user` > `volunteer_type` > `global`), evaluated **at the period
start** so an edit in July cannot rewrite May. A `volunteer_type` rule never matches
someone whose type is null. If the rule's native period differs from the one being viewed
the target is scaled (quarter ÷ 3, month × 3) and marked `~`; **a scaled target never
reports "no complert"** — its deadline has not arrived, so it reads "en curs". Status bands:
≥100 % complete, ≥`at_risk_ratio` (default 0.8, configurable) at risk, else not complete;
no rule → "sense compromís", never 0 %.

### 5.4 Payments write-back

Off by default (`app_setting.writeback_enabled = false`). All six rules from the plan are
enforced in `lib/writeback.ts` and restated at the top of that file: only `amount` and
`paid` are written, `amount` is that month's derived charges only, each write is a
compare-and-set inside a Firestore transaction against the value the last sync observed,
nothing is written when the document already matches, only the canonical `MIN(doc_id)` doc
is targeted, and the whole thing is gated. `reconcile()` is read-only and is what the
Configuració → Escriptura a Firebase report renders.

### 5.5 Authorisation

`requireAdmin()` / `requireCoordinator()` per request; the allowlist is re-read from the
database every time, so removing an access takes effect immediately rather than at token
expiry. The area scope of an `area_responsible` is applied **in the SQL** — verified: a
responsible for Cuina + Hort who asks for `?area=temple` gets their own areas back
(77h), not Temple's data. They also get no meal, overnight or money columns at all, on the
page **and** in the export, because `showPayments` is re-derived from the session in both.

---

## 6. Verified

Seeded with `npm run seed:demo` (232 bookings, 9 volunteers, 5 months, deliberate
anomalies) and checked against SQL by hand:

- Calendar totals: 222h / 10 persones / 19 dies / 47 reserves for July, matching
  `SUM(minutes)` and `COUNT(DISTINCT …)` exactly. Area filter: Cuina 64h 30m = SQL.
- Area scope enforced in the query, including the "asks for someone else's area" case.
- Coordination table: per-area hours match SQL per volunteer; rule precedence visible
  (Alba's personal 40h Cuina rule beats the global 10h); a quarterly Temple rule renders as
  `~10h` per month; a commitment with zero hours still produces a column (`0h / 10h`).
- Dated prices: a retroactive lunch correction moved open periods and not the closed one.
- Ledger: void arithmetic nets to zero; a re-void is refused; 0 € and malformed dates are
  refused; `owed = charges + carry-in − credits` reconciles on screen (98 + 182 + 5 = 285).
- Excel export: every per-volunteer charge total in the workbook equals the database, and
  every item count too; all derived cells are formulas (`=E2*F2`, `=SUMIF(…)`, `=SUM(…)`,
  `=IF(N(target)=0,"",done/target)`).
- Write-back: the plan detects drift, duplicate docs and missing docs; `applyWriteback`
  refuses to run while the gate is closed.
- Data quality panel: 1 zero-minute shift, 1 orphan booking, 3 bookings whose numbers are
  affected — each traceable to a deliberately malformed seed document.
- `deploy/backup.sh` end to end: snapshot, `PRAGMA integrity_check`, row-count log, gzip,
  restore.
- `npm test` 67 tests, `npx tsc --noEmit` clean, `npm run lint` clean, `npm run build`
  clean.

### Not verified

- **Any real Firestore read or write.** No credentials (§7).
- **The `time_range` wire format** (§7).
- **An actual Google sign-in.** Verified by minting a session locally instead.

---

## 7. Blocked on the user

| Needed | Why | Status |
| --- | --- | --- |
| `gcloud auth application-default login`, or a service-account key | The sync reaches Firestore and fails with `invalid_grant` (expired token) | **Blocking real verification** |
| Google OAuth client id + secret in `web/.env.local` | Only the refusal paths of sign-in can be tested without it | Blocking sign-in test |
| Confirm the prod Firebase project id | `tools/users/add-user.mjs:22` guesses `voluntariat-casa-virupa`; only dev is verified | Minor |

### The one unverified assumption — resolve this first

**`time_range.start/end` wire format has not been observed on a real document.** It is
`kotlinx.datetime.LocalTime` (`FirebaseVolunteer.kt:32`), which should serialise to
`"10:00"` — but that is inference. `parseSecondOfDay()` in `lib/dates.ts` accepts
`HH:mm`, `HH:mm:ss` and a bare seconds-of-day number, and flags anything else as
`ANOMALY.UNPARSEABLE_TIME` rather than returning 0.

Once credentials work, read one document and confirm:

```bash
node -e "
const {getFirestore}=require('firebase-admin/firestore');
const {initializeApp,applicationDefault}=require('firebase-admin/app');
initializeApp({credential:applicationDefault(),projectId:'voluntariat-casa-virupa-dev'});
getFirestore().collection('volunteers').limit(1).get()
  .then(s=>console.log(JSON.stringify(s.docs[0]?.data(),null,2)));"
```

Then run a real sync and open **Configuració → Qualitat de dades**. "Torns de 0 minuts"
near zero means the parser is right; a large number means it is wrong and every hour figure
on the site is understated.

---

## 8. Running and verifying

```bash
cd web
npm install
cp .env.example .env.local        # fill AUTH_SECRET, AUTH_GOOGLE_*, SYNC_KEY
npm run db:migrate                # migrations + views + seeds (idempotent)
node scripts/add-admin.ts you@example.cat --coordinator --name "You"
npm run seed:demo                 # OPTIONAL: fake mirror data, needs no credentials
npm run dev

npm test                          # 67 tests, node --test over lib/**/*.test.ts
npx tsc --noEmit
npm run lint
npm run build                     # must stay clean
```

`npm run seed:demo --force` wipes and rebuilds the `fs_*` mirror; it refuses to run with
`NODE_ENV=production` and never touches a dashboard-owned table, so seeded data still
exercises the real prices, commitments and ledger. Clear it with a real
`POST /api/sync?full=1` once credentials exist.

Trigger a sync:

```bash
curl -X POST -H "X-Sync-Key: $SYNC_KEY" http://127.0.0.1:3000/api/sync
curl -X POST -H "X-Sync-Key: $SYNC_KEY" 'http://127.0.0.1:3000/api/sync?full=1'   # coordinator only
```

Deployment is `web/deploy/README.md` — systemd unit, nginx, certbot, crontab at **00:10 and
12:10** plus a weekly full resync, and a nightly verified `sqlite3 .backup` offsite.

---

## 9. Known issues, not yet addressed

- **30 npm advisories** (10 moderate, 20 high), all transitive: eslint→minimatch,
  esbuild dev server, postcss, sharp, exceljs→archiver, firebase-admin→@google-cloud/storage.
  `npm audit fix` resolves none of them; every fix needs a major bump. Mostly dev-time.
- **A Turbopack NFT warning** on `next.config.ts` — `lib/db/index.ts` resolves its path from
  an env var, so the tracer pulls in the working directory. Contained with
  `outputFileTracingExcludes`; the standalone artefact correctly excludes `data/`.
- **The first month after go-live will show a large "saldo anterior" for everyone**, because
  the mirror has charges but the ledger has no history. Seed an `opening_balance` entry per
  volunteer (side panel → "Registra un pagament o un ajust") before showing the page to
  anyone, or the numbers will be alarming and wrong in spirit.
- **A global commitment rule matches everyone**, including `coordination_team` members with
  no `volunteer_type`. Intended, but worth knowing before creating one.
- **`README.md` at the repo root** has ~53 uncommitted lines of Firestore reference tables
  that predate this work. Not mine, left untouched — worth folding into `CLAUDE.md` or
  discarding.
- **`CLAUDE.md`'s Firebase section is out of date** (§3). Worth fixing in the same pass as
  whatever touches the mobile app next.
