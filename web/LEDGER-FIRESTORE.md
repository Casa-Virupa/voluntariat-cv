# Money: the Firestore contract with the mobile app

Since 2026-08-01 the app derives every balance instead of storing it. The old contract
(`configuration/prices` + mutable `payments` docs + the dashboard write-back) is dead; the
`payments` collection and the `configuration/prices` doc are **frozen archives** and can be
deleted once nobody misses them.

## The three facts, and who writes each one

| Firestore collection | What it is | Single writer |
| --- | --- | --- |
| `volunteers` | consumption per day (meals, sleep, shifts) | the app |
| `price_rules` | dated history of the GENERIC prices, in euros | this dashboard (`lib/publish.ts`) |
| `ledger` | money movements, positive = received | this dashboard + admin console |

Everything else is computed, identically on the phone and here:

- charge of a booking = its consumption × the rule in force **on the service date**
- outstanding balance of a user = Σ charges (all bookings, past and future) − Σ ledger

## `price_rules`

One doc per change of the generic price set, doc ID = `valid_from`:

```
price_rules/2020-01-01
├── valid_from  "2020-01-01"   string YYYY-MM-DD
├── lunch       8              number, EUROS
├── dinner      8
├── breakfast   0
└── sleep       10
```

- The app resolves "rule for date D" as the doc with the greatest `valid_from <= D`, and
  falls back to its own defaults (8/8/0/10) when none applies. Missing/extra fields never
  break it.
- `buildPriceRuleDocs()` materialises the dashboard's generic `price_rule` timeline into
  these docs: one per boundary (`valid_from` and `valid_to` alike), so a rule ended with no
  successor correctly reverts to the defaults, and a future-dated rule is published today —
  the app applies it when its date arrives. No cron involved.
- Rules with `volunteer_type` / `is_member` specificity are a dashboard-only refinement:
  the app knows one flat price, so only the generic line is published. /configuracio warns
  when specific rules exist.
- Publishing runs automatically after `addPriceAction` / `endPriceAction` and is idempotent
  (deterministic IDs, orphan docs deleted). Manual retry: Configuració → Firestore (app).

## `ledger`

One doc per money movement:

```
ledger/dash-42                          ← doc ID: "dash-<ledger_entry.id>" when we wrote it
├── user_id         "<uid>"
├── date            "2026-08-01"        string YYYY-MM-DD (effective date)
├── amount          26                  number, EUROS. POSITIVE = money received
├── kind            "payment"           "payment" | "adjustment" — all the app models
├── note            "efectiu agost"     optional
├── dashboard_kind  "payment"           extra fields, ignored by the app
└── recorded_by     "coordinator@…"
```

- The app sums `amount` over a user's docs — nothing else matters to it. Voiding an entry
  publishes the negation as a second doc; history stays visible on both sides.
- `opening_balance` and `write_off` (SQLite-only kinds) are published as `"adjustment"`
  with the original preserved in `dashboard_kind`.

## Duplication, by design

Data lives in BOTH stores (Oscar's call, 2026-08-01): Firestore is what the app reads;
SQLite `ledger_entry` stays the dashboard's write model and query source.

- **Out:** every `insertLedgerEntry` / `voidLedgerEntry` is followed by a publish to
  `ledger/dash-<id>`. If Firestore is down the SQLite row survives and shows up in the
  retry queue (Configuració → Firestore (app) → "apunts pendents").
- **In:** the sync (every 15 minutes, see `deploy/crontab`) reads the whole `ledger`
  collection and imports any doc it did not publish itself (bootstrap entries, console
  entries) into `ledger_entry` with `external_ref = "fs:<docId>"` and actor
  `firestore-sync`. The unique index on `external_ref` makes the import exactly-once, and
  `dash-*` docs are skipped because they already ARE rows here.
- A doc later deleted from Firestore stays on the SQLite books: `ledger_entry` is
  insert-only. Corrections are voids, not deletions, on both sides.
- Malformed ledger docs are SKIPPED by the import (not flagged-and-kept like bookings):
  importing money wrong is worse than importing it a run later.

## What replaced the write-back

`lib/writeback.ts`, its UI, the `writeback_enabled` gate and the `payments` mirroring in
the sync are gone. There is nothing to reconcile anymore: both sides read the same facts
and apply the same pure formula. The `fs_payment` table and `writeback_log` remain in
SQLite as read-only history of the old era.

## Firestore security rules (console-managed, not in the repo)

`price_rules` and `ledger` must be client-read-only (the Admin SDK used here bypasses
rules); `payments` should reject client writes too now that it is an archive:

```
match /price_rules/{doc} { allow read: if request.auth != null; allow write: if false; }
match /ledger/{doc} {
  allow read: if request.auth != null && resource.data.user_id == request.auth.uid;
  allow write: if false;
}
match /payments/{doc} { allow read: if request.auth != null; allow write: if false; }
```
