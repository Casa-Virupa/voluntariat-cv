/**
 * Read queries for /configuracio: the dated rule tables, the allowlist, the sync history,
 * and the data-quality report.
 *
 * The data-quality section is not decoration. Every anomaly here is a booking whose hours
 * or money the dashboard could not fully make sense of, and each one is a number somewhere
 * else on the site that is quietly lower than the truth. It is the first place to look when
 * a coordinator says a total is wrong.
 */

import { raw } from '../db/index.ts'
import { ANOMALY, ANOMALY_LABELS } from '../sync/anomalies.ts'

export interface PriceRuleRow {
  id: number
  item: string
  volunteerType: string | null
  isMember: number | null
  unitPriceCents: number
  validFrom: string
  validTo: string | null
  note: string | null
  createdBy: string
}

export function priceRules(): PriceRuleRow[] {
  return raw()
    .prepare(
      `SELECT id, item, volunteer_type AS volunteerType, is_member AS isMember,
              unit_price_cents AS unitPriceCents, valid_from AS validFrom,
              valid_to AS validTo, note, created_by AS createdBy
         FROM price_rule
        ORDER BY item, valid_from DESC, id DESC`,
    )
    .all() as PriceRuleRow[]
}

export interface CommitmentRuleRow {
  id: number
  scopeKind: string
  scopeValue: string | null
  /** Resolved display name when scopeValue is a uid. */
  scopeName: string | null
  area: string
  periodKind: string
  targetMinutes: number
  validFrom: string
  validTo: string | null
  note: string | null
}

export function commitmentRules(): CommitmentRuleRow[] {
  return raw()
    .prepare(
      `SELECT c.id, c.scope_kind AS scopeKind, c.scope_value AS scopeValue,
              u.name AS scopeName, c.area, c.period_kind AS periodKind,
              c.target_minutes AS targetMinutes, c.valid_from AS validFrom,
              c.valid_to AS validTo, c.note
         FROM commitment_rule c
         LEFT JOIN fs_user u ON u.uid = c.scope_value AND c.scope_kind = 'user'
        ORDER BY c.area, c.scope_kind DESC, c.valid_from DESC, c.id DESC`,
    )
    .all() as CommitmentRuleRow[]
}

export interface AdminRow {
  email: string
  displayName: string | null
  role: string
  addedBy: string
  addedAt: number
  disabledAt: number | null
  areas: string[]
}

export function admins(): AdminRow[] {
  const rows = raw()
    .prepare(
      `SELECT a.email, a.display_name AS displayName, a.role, a.added_by AS addedBy,
              a.added_at AS addedAt, a.disabled_at AS disabledAt,
              (SELECT GROUP_CONCAT(ar.area) FROM admin_user_area ar WHERE ar.email = a.email) AS areas
         FROM admin_user a
        ORDER BY a.disabled_at IS NOT NULL, a.role, a.email`,
    )
    .all() as Array<Omit<AdminRow, 'areas'> & { areas: string | null }>

  return rows.map((r) => ({ ...r, areas: (r.areas ?? '').split(',').filter(Boolean) }))
}

/** The volunteer picker for a per-person commitment. */
export function volunteerOptions(): Array<{ uid: string; name: string; volunteerType: string | null }> {
  return raw()
    .prepare(
      `SELECT uid, COALESCE(NULLIF(name, ''), email, uid) AS name, volunteer_type AS volunteerType
         FROM fs_user
        WHERE deleted_at IS NULL
        ORDER BY name COLLATE NOCASE`,
    )
    .all() as Array<{ uid: string; name: string; volunteerType: string | null }>
}

export interface SyncRunRow {
  id: number
  mode: string
  trigger: string
  triggeredBy: string | null
  status: string
  startedAt: number
  finishedAt: number | null
  windowFrom: string | null
  readBookings: number
  insertedBookings: number
  deletedBookings: number
  anomalies: number
  errorMessage: string | null
}

export function recentSyncRuns(limit = 12): SyncRunRow[] {
  return raw()
    .prepare(
      `SELECT id, mode, trigger, triggered_by AS triggeredBy, status,
              started_at AS startedAt, finished_at AS finishedAt, window_from AS windowFrom,
              read_bookings AS readBookings, inserted_bookings AS insertedBookings,
              deleted_bookings AS deletedBookings, anomalies, error_message AS errorMessage
         FROM sync_run
        ORDER BY id DESC
        LIMIT ?`,
    )
    .all(limit) as SyncRunRow[]
}

// --- data quality ------------------------------------------------------------

export interface DataQuality {
  /** One entry per anomaly bit, with the number of live bookings carrying it. */
  anomalies: Array<{ bit: number; label: string; count: number }>
  zeroMinuteShifts: number
  liveBookings: number
  unpricedItems: number
  unpricedAmountRows: number
  unknownAreaShifts: number
  orphanBookings: number
  usersWithoutType: number
  /** Bookings whose hours are 0 in total — the signature of a wrong time parser. */
  zeroMinuteBookings: number
}

export function dataQuality(): DataQuality {
  const q = <T>(sql: string, ...args: unknown[]) => raw().prepare(sql).get(...args) as T

  const anomalies = Object.entries(ANOMALY).map(([, bit]) => {
    const row = q<{ n: number }>(
      `SELECT COUNT(*) AS n FROM fs_booking WHERE deleted_at IS NULL AND (anomaly_flags & ?) != 0`,
      bit,
    )
    return { bit: bit as number, label: ANOMALY_LABELS[bit as number] ?? String(bit), count: row.n }
  })

  return {
    anomalies: anomalies.filter((a) => a.count > 0).sort((a, b) => b.count - a.count),
    zeroMinuteShifts: q<{ n: number }>(
      `SELECT COUNT(*) AS n FROM fs_booking_shift s
         JOIN fs_booking b ON b.doc_id = s.doc_id
        WHERE b.deleted_at IS NULL AND s.minutes = 0`,
    ).n,
    zeroMinuteBookings: q<{ n: number }>(
      `SELECT COUNT(*) AS n FROM fs_booking WHERE deleted_at IS NULL AND total_minutes = 0`,
    ).n,
    liveBookings: q<{ n: number }>(
      `SELECT COUNT(*) AS n FROM fs_booking WHERE deleted_at IS NULL`,
    ).n,
    unpricedItems: q<{ n: number }>(
      `SELECT COALESCE(SUM(qty), 0) AS n FROM v_charge_effective WHERE has_price = 0`,
    ).n,
    unpricedAmountRows: q<{ n: number }>(
      `SELECT COUNT(*) AS n FROM v_charge_effective WHERE has_price = 0`,
    ).n,
    unknownAreaShifts: q<{ n: number }>(
      `SELECT COUNT(*) AS n FROM fs_booking_shift s
         JOIN fs_booking b ON b.doc_id = s.doc_id
        WHERE b.deleted_at IS NULL AND s.area = '__unknown__'`,
    ).n,
    orphanBookings: q<{ n: number }>(
      `SELECT COUNT(*) AS n FROM fs_booking b
        WHERE b.deleted_at IS NULL
          AND NOT EXISTS (SELECT 1 FROM fs_user u WHERE u.uid = b.user_id)`,
    ).n,
    usersWithoutType: q<{ n: number }>(
      `SELECT COUNT(*) AS n FROM fs_user
        WHERE deleted_at IS NULL AND role = 'volunteer' AND volunteer_type IS NULL`,
    ).n,
  }
}

// --- the change log ----------------------------------------------------------

export interface AuditEntry {
  id: number
  actor: string
  action: string
  entity: string
  entityId: string | null
  at: number
}

/** One page of it, plus the before/after payloads — only the export asks for those. */
export interface AuditEntryFull extends AuditEntry {
  beforeJson: string | null
  afterJson: string | null
}

export const AUDIT_PAGE_SIZE = 100

export interface AuditPage {
  rows: AuditEntry[]
  /** Rows in the whole log, so the pager can size itself and the page can say "de N". */
  total: number
  /** 1-based, already clamped to the pages that exist. */
  page: number
  pageCount: number
  pageSize: number
}

/**
 * A page of the change log, newest first.
 *
 * Paged rather than `LIMIT 20`: this table only grows, and the point of an audit log is
 * that the entry you need is still reachable months later. The full history — including
 * the before/after payloads, which are too wide for the screen — goes out through
 * `allAuditEntries()` and /api/export/canvis.
 */
export function auditPage(page = 1, pageSize = AUDIT_PAGE_SIZE): AuditPage {
  const total = (raw().prepare(`SELECT COUNT(*) AS n FROM audit_log`).get() as { n: number }).n
  const pageCount = Math.max(1, Math.ceil(total / pageSize))
  const current = Math.min(Math.max(1, Math.trunc(page) || 1), pageCount)

  const rows = raw()
    .prepare(
      `SELECT id, actor_email AS actor, action, entity, entity_id AS entityId, at
         FROM audit_log ORDER BY id DESC LIMIT ? OFFSET ?`,
    )
    .all(pageSize, (current - 1) * pageSize) as AuditEntry[]

  return { rows, total, page: current, pageCount, pageSize }
}

/**
 * The whole log for the archive export. Deliberately unbounded — an export that silently
 * stopped at N would be worse than no export — so it is never called from a page render.
 */
export function allAuditEntries(): AuditEntryFull[] {
  return raw()
    .prepare(
      `SELECT id, actor_email AS actor, action, entity, entity_id AS entityId, at,
              before_json AS beforeJson, after_json AS afterJson
         FROM audit_log ORDER BY id DESC`,
    )
    .all() as AuditEntryFull[]
}
