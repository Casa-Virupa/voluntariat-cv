/**
 * SQLite schema, in two strictly separated zones.
 *
 *   fs_*    mirror of Firestore. Disposable — a full resync deletes and rebuilds it.
 *           ONLY the sync writer may write these tables, and none of them may ever hold
 *           a foreign key to a dashboard table (that would make a resync unsafe).
 *
 *   rest    owned by the dashboard. Must survive a full resync. This is where everything
 *           the mobile app cannot represent lives: the allowlist, per-area commitments,
 *           dated prices, and the payment ledger.
 *
 * Conventions: money in integer CENTS, durations in integer MINUTES, dates as TEXT
 * 'YYYY-MM-DD', instants as integer unix SECONDS.
 */

import { sql } from 'drizzle-orm'
import {
  index,
  integer,
  primaryKey,
  real,
  sqliteTable,
  text,
  uniqueIndex,
} from 'drizzle-orm/sqlite-core'

// =============================================================================
// Zone A — Firestore mirror
// =============================================================================

export const fsUser = sqliteTable(
  'fs_user',
  {
    /** Firestore doc id, which is the Firebase Auth uid. */
    uid: text('uid').primaryKey(),
    name: text('name').notNull().default(''),
    email: text('email').notNull().default(''),
    role: text('role').notNull().default('unknown'),
    volunteerType: text('volunteer_type'),
    onboardingCompleted: integer('onboarding_completed', { mode: 'boolean' })
      .notNull()
      .default(false),
    isMember: integer('is_member', { mode: 'boolean' }).notNull().default(false),
    /** Verbatim document, so a parser change can be replayed without re-reading Firestore. */
    rawJson: text('raw_json').notNull(),
    docHash: text('doc_hash').notNull(),
    firstSeenAt: integer('first_seen_at').notNull(),
    lastSeenAt: integer('last_seen_at').notNull(),
    /** Soft delete — bookings must keep resolving to a name after a user disappears. */
    deletedAt: integer('deleted_at'),
  },
  (t) => [
    index('ix_fs_user_email').on(t.email),
    index('ix_fs_user_role').on(t.role, t.volunteerType),
  ],
)

/**
 * The user's CURRENT area membership. Used only for pickers and for scoping an
 * area_responsible's view. Never use it to attribute hours — see fsBookingShift.area.
 */
export const fsUserArea = sqliteTable(
  'fs_user_area',
  {
    uid: text('uid').notNull(),
    area: text('area').notNull(),
  },
  (t) => [primaryKey({ columns: [t.uid, t.area] }), index('ix_fs_user_area').on(t.area)],
)

export const fsBooking = sqliteTable(
  'fs_booking',
  {
    /** Firestore doc id. */
    docId: text('doc_id').primaryKey(),
    /** No FK: Firestore has no referential integrity and users can vanish. */
    userId: text('user_id').notNull(),
    /** Raw `timestamp`, unix seconds, kept so serviceDate can be re-derived. */
    tsEpoch: integer('ts_epoch').notNull(),
    /** Derived via the +12h Europe/Madrid rule. THE date everything groups by. */
    serviceDate: text('service_date').notNull(),
    hasLunch: integer('has_lunch', { mode: 'boolean' }).notNull().default(false),
    hasDinner: integer('has_dinner', { mode: 'boolean' }).notNull().default(false),
    sleep: integer('sleep', { mode: 'boolean' }).notNull().default(false),
    /** Verbatim meal_types, so legacy "" / unknown values stay visible. */
    mealsRawJson: text('meals_raw_json').notNull().default('[]'),
    shiftCount: integer('shift_count').notNull().default(0),
    /** Denormalised sum of the child rows, for cheap day/month totals. */
    totalMinutes: integer('total_minutes').notNull().default(0),
    /** Bitmask, see ANOMALY in lib/sync/anomalies.ts */
    anomalyFlags: integer('anomaly_flags').notNull().default(0),
    rawJson: text('raw_json').notNull(),
    docHash: text('doc_hash').notNull(),
    firstSeenAt: integer('first_seen_at').notNull(),
    lastSeenAt: integer('last_seen_at').notNull(),
    /** Tombstone: the doc was inside the synced window but absent from Firestore. */
    deletedAt: integer('deleted_at'),
  },
  (t) => [
    index('ix_booking_date').on(t.serviceDate),
    index('ix_booking_user_date').on(t.userId, t.serviceDate),
    index('ix_booking_anomaly').on(t.anomalyFlags),
  ],
)

export const fsBookingShift = sqliteTable(
  'fs_booking_shift',
  {
    docId: text('doc_id')
      .notNull()
      .references(() => fsBooking.docId, { onDelete: 'cascade' }),
    /** Index within the source `shifts` array. */
    seq: integer('seq').notNull(),
    /** 'morning' | 'afternoon' | 'unknown' */
    slot: text('slot').notNull(),
    /** 'general' | 'specific' | 'unknown' */
    kind: text('kind').notNull(),
    /**
     * '__general__' | <area code> | '__unknown__'. Frozen at booking time — this is the
     * only correct basis for attributing hours to an area.
     */
    area: text('area').notNull(),
    /** Seconds of day; null when the wire value could not be parsed. */
    startSec: integer('start_sec'),
    endSec: integer('end_sec'),
    minutes: integer('minutes').notNull().default(0),
  },
  (t) => [
    primaryKey({ columns: [t.docId, t.seq] }),
    index('ix_shift_area').on(t.area),
  ],
)

/**
 * Mirror of the app's own payment docs. Deliberately NOT uniquely keyed on
 * (user, year, month): the app's addPayment is a non-transactional read-modify-write, so
 * duplicates are reachable. The app reads `.firstOrNull()`, i.e. MIN(doc_id) — that is
 * the canonical row, and the only one write-back may touch.
 */
export const fsPayment = sqliteTable(
  'fs_payment',
  {
    docId: text('doc_id').primaryKey(),
    userId: text('user_id').notNull(),
    year: integer('year').notNull(),
    month: integer('month').notNull(),
    paid: integer('paid', { mode: 'boolean' }).notNull().default(false),
    amountCents: integer('amount_cents').notNull().default(0),
    /** The exact double as stored, for drift forensics against our derived total. */
    amountRaw: real('amount_raw').notNull().default(0),
    rawJson: text('raw_json').notNull(),
    docHash: text('doc_hash').notNull(),
    firstSeenAt: integer('first_seen_at').notNull(),
    lastSeenAt: integer('last_seen_at').notNull(),
    deletedAt: integer('deleted_at'),
  },
  (t) => [index('ix_payment_period').on(t.userId, t.year, t.month)],
)

// =============================================================================
// Zone B — dashboard-owned
// =============================================================================

/**
 * Authorisation. Auth.js runs with JWT sessions and no database adapter, so this table
 * IS the authorisation model — it is re-checked server-side on every request so that
 * removing someone takes effect immediately rather than at token expiry.
 */
export const adminUser = sqliteTable('admin_user', {
  email: text('email').primaryKey(),
  displayName: text('display_name'),
  /** 'coordinator' sees everything; 'area_responsible' is scoped to adminUserArea. */
  role: text('role').notNull(),
  addedBy: text('added_by').notNull(),
  addedAt: integer('added_at').notNull(),
  disabledAt: integer('disabled_at'),
})

export const adminUserArea = sqliteTable(
  'admin_user_area',
  {
    email: text('email')
      .notNull()
      .references(() => adminUser.email, { onDelete: 'cascade' }),
    area: text('area').notNull(),
  },
  (t) => [primaryKey({ columns: [t.email, t.area] })],
)

/**
 * Per-area hour targets. Does not exist in the app at all — the app knows only a global
 * 60h/quarter (Mitra) or 8h/month (Habitual). Dated and scoped so that changing a target
 * does not rewrite history.
 */
export const commitmentRule = sqliteTable(
  'commitment_rule',
  {
    id: integer('id').primaryKey({ autoIncrement: true }),
    /** 'global' | 'volunteer_type' | 'user' — most specific match wins. */
    scopeKind: text('scope_kind').notNull(),
    /** null | 'mitra'/'habitual' | uid */
    scopeValue: text('scope_value'),
    /** '__total__' | '__general__' | <area code> */
    area: text('area').notNull(),
    /** 'month' | 'quarter' — the rule's native period. */
    periodKind: text('period_kind').notNull(),
    targetMinutes: integer('target_minutes').notNull(),
    validFrom: text('valid_from').notNull(),
    /** exclusive; null = still in force */
    validTo: text('valid_to'),
    note: text('note'),
    createdBy: text('created_by').notNull(),
    createdAt: integer('created_at').notNull(),
  },
  (t) => [index('ix_commitment_lookup').on(t.area, t.scopeKind, t.scopeValue, t.validFrom)],
)

/**
 * Supersedes the app's hardcoded MEAL_PRICE=8 / SLEEP_PRICE=10. Resolved against the
 * booking's service date, so a price change next month leaves last month's books alone.
 * null in volunteerType / isMember means "any"; more specific rows win.
 */
export const priceRule = sqliteTable(
  'price_rule',
  {
    id: integer('id').primaryKey({ autoIncrement: true }),
    item: text('item').notNull(),
    volunteerType: text('volunteer_type'),
    isMember: integer('is_member', { mode: 'boolean' }),
    unitPriceCents: integer('unit_price_cents').notNull(),
    validFrom: text('valid_from').notNull(),
    validTo: text('valid_to'),
    note: text('note'),
    createdBy: text('created_by').notNull(),
    createdAt: integer('created_at').notNull(),
  },
  (t) => [index('ix_price_lookup').on(t.item, t.validFrom)],
)

/**
 * INSERT-ONLY. Never UPDATE, never DELETE. A mistake is corrected by inserting the
 * negation with voidsId pointing at the original, so the history panel is just a query
 * and nothing can silently rewrite the books.
 *
 * Sign convention: POSITIVE = credit, i.e. reduces what the volunteer owes.
 */
export const ledgerEntry = sqliteTable(
  'ledger_entry',
  {
    id: integer('id').primaryKey({ autoIncrement: true }),
    userId: text('user_id').notNull(),
    /** 'payment' | 'adjustment' | 'opening_balance' | 'write_off' */
    kind: text('kind').notNull(),
    effectiveDate: text('effective_date').notNull(),
    amountCents: integer('amount_cents').notNull(),
    /** 'cash' | 'transfer' | 'app' | null */
    method: text('method'),
    note: text('note'),
    createdBy: text('created_by').notNull(),
    createdAt: integer('created_at').notNull(),
    voidsId: integer('voids_id'),
    /** Idempotency key for anything inserted automatically. */
    externalRef: text('external_ref'),
  },
  (t) => [
    index('ix_ledger_user_date').on(t.userId, t.effectiveDate),
    uniqueIndex('ux_ledger_external_ref').on(t.externalRef),
  ],
)

/** Freezes a period so a later price edit cannot mutate an already-invoiced month. */
export const periodClose = sqliteTable(
  'period_close',
  {
    userId: text('user_id').notNull(),
    periodKind: text('period_kind').notNull(),
    periodYear: integer('period_year').notNull(),
    /** month 1-12 or quarter 1-4 */
    periodIndex: integer('period_index').notNull(),
    closedAt: integer('closed_at').notNull(),
    closedBy: text('closed_by').notNull(),
    totalCents: integer('total_cents').notNull(),
  },
  (t) => [
    primaryKey({ columns: [t.userId, t.periodKind, t.periodYear, t.periodIndex] }),
  ],
)

/** Snapshot of the derived charges at close time; v_charge_effective prefers these. */
export const chargeLocked = sqliteTable(
  'charge_locked',
  {
    docId: text('doc_id').notNull(),
    item: text('item').notNull(),
    userId: text('user_id').notNull(),
    serviceDate: text('service_date').notNull(),
    qty: integer('qty').notNull(),
    unitPriceCents: integer('unit_price_cents').notNull(),
    amountCents: integer('amount_cents').notNull(),
    lockedAt: integer('locked_at').notNull(),
  },
  (t) => [
    primaryKey({ columns: [t.docId, t.item] }),
    index('ix_charge_locked_user').on(t.userId, t.serviceDate),
  ],
)

export const syncRun = sqliteTable(
  'sync_run',
  {
    id: integer('id').primaryKey({ autoIncrement: true }),
    /** 'window' | 'full' */
    mode: text('mode').notNull(),
    /** 'cron' | 'manual' */
    trigger: text('trigger').notNull(),
    triggeredBy: text('triggered_by'),
    /** 'running' | 'ok' | 'failed' | 'aborted' */
    status: text('status').notNull(),
    startedAt: integer('started_at').notNull(),
    heartbeatAt: integer('heartbeat_at').notNull(),
    finishedAt: integer('finished_at'),
    windowFrom: text('window_from'),
    readUsers: integer('read_users').notNull().default(0),
    readBookings: integer('read_bookings').notNull().default(0),
    readPayments: integer('read_payments').notNull().default(0),
    insertedBookings: integer('inserted_bookings').notNull().default(0),
    deletedBookings: integer('deleted_bookings').notNull().default(0),
    anomalies: integer('anomalies').notNull().default(0),
    errorMessage: text('error_message'),
  },
  (t) => [
    /**
     * The sync mutex. A partial unique index means the database itself refuses a second
     * concurrent run — this survives a restart and a second process, which an in-process
     * JS mutex would not.
     */
    uniqueIndex('ux_sync_single_run')
      .on(t.status)
      .where(sql`${t.status} = 'running'`),
    index('ix_sync_started').on(t.startedAt),
  ],
)

/** Every mutation we make to Firestore, with its compare-and-set precondition. */
export const writebackLog = sqliteTable(
  'writeback_log',
  {
    id: integer('id').primaryKey({ autoIncrement: true }),
    runId: integer('run_id'),
    paymentDocId: text('payment_doc_id'),
    userId: text('user_id').notNull(),
    year: integer('year').notNull(),
    month: integer('month').notNull(),
    expectedAmountCents: integer('expected_amount_cents'),
    expectedPaid: integer('expected_paid', { mode: 'boolean' }),
    newAmountCents: integer('new_amount_cents').notNull(),
    newPaid: integer('new_paid', { mode: 'boolean' }).notNull(),
    /** 'applied' | 'created' | 'skipped_nochange' | 'skipped_conflict' | 'failed' */
    status: text('status').notNull(),
    error: text('error'),
    createdAt: integer('created_at').notNull(),
  },
  (t) => [index('ix_writeback_period').on(t.userId, t.year, t.month)],
)

export const appSetting = sqliteTable('app_setting', {
  key: text('key').primaryKey(),
  valueJson: text('value_json').notNull(),
  updatedAt: integer('updated_at').notNull(),
  updatedBy: text('updated_by'),
})

export const auditLog = sqliteTable(
  'audit_log',
  {
    id: integer('id').primaryKey({ autoIncrement: true }),
    actorEmail: text('actor_email').notNull(),
    action: text('action').notNull(),
    entity: text('entity').notNull(),
    entityId: text('entity_id'),
    beforeJson: text('before_json'),
    afterJson: text('after_json'),
    at: integer('at').notNull(),
  },
  (t) => [index('ix_audit_at').on(t.at)],
)
