/**
 * The aggregation behind /coordinacio: one row per volunteer, hours per area against the
 * resolved commitment, then the meal/overnight ledger.
 *
 * Everything is assembled from a handful of grouped queries and joined in memory rather
 * than in one wide SQL statement. With a few dozen volunteers that is faster to run and
 * far easier to reason about — and it keeps each query's grouping obvious, which is where
 * a wrong number would come from.
 *
 * Money is never stored: charges come from `v_charge_effective`, which resolves the dated
 * price table against each booking's own service date. Re-syncing cannot double-count and
 * a cancelled booking stops being charged by itself.
 */

import { raw } from '../db/index.ts'
import { AREA_GENERAL, AREA_TOTAL, AREA_UNKNOWN } from '../contract.ts'
import { monthsIn, type Period } from '../dates.ts'
import {
  progressOf,
  resolveCommitment,
  ruleMatchesVolunteer,
  type Progress,
} from '../commitments.ts'
import { balanceOf, settlementOf, type Balance, type SettlementState } from '../ledger.ts'
import { commitmentRulesValidAt, myVolunteerUids } from './scope.ts'
import type { CoordinationFilters } from './filters.ts'

function inList(values: unknown[]): string {
  return `(${values.map(() => '?').join(', ')})`
}

export interface ItemCounts {
  /** service_date <= today: consumed, and definitely chargeable. */
  done: number
  /** Booked but still in the future. Charged all the same — the app charges at booking. */
  upcoming: number
}

export interface VolunteerRow {
  uid: string
  name: string
  email: string
  role: string
  volunteerType: string | null
  isMember: boolean
  /** The user document is gone from Firestore but their bookings are still mirrored. */
  userMissing: boolean
  minutesByArea: Map<string, number>
  totalMinutes: number
  /** Keyed by the same strings as minutesByArea, plus AREA_TOTAL. */
  progressByArea: Map<string, Progress>
  totalProgress: Progress
  items: Map<string, ItemCounts>
  /** Charges whose price could not be resolved — a data-quality signal, not a free meal. */
  unpricedItems: number
  balance: Balance
  settlement: SettlementState
  /** What the phone currently shows for the months in this period, for drift detection. */
  appPaid: boolean | null
  appAmountCents: number | null
}

export interface CoordinationTable {
  rows: VolunteerRow[]
  /** Column order: general first, then areas with data, unknown last. */
  areaColumns: string[]
  totals: {
    minutesByArea: Map<string, number>
    totalMinutes: number
    chargesCents: number
    creditsCents: number
    owedTotalCents: number
    items: Map<string, ItemCounts>
  }
  /** False for an area_responsible: meals and money are coordination's business. */
  showPayments: boolean
  /** (user, year, month) tuples with more than one payment doc in Firestore. */
  duplicatePayments: Array<{ uid: string; name: string; year: number; month: number; count: number }>
}

export interface CoordinationScope {
  /** Permission scope: null means every area. */
  allowedAreas: string[] | null
  /** Narrowed by the requested area filter — what the hour columns actually sum. */
  queryAreas: string[] | null
  /** For "només els meus voluntaris". */
  myVolunteerAreas: string[]
  showPayments: boolean
  today: string
  atRiskRatio: number
}

export function coordinationTable(
  filters: CoordinationFilters,
  scope: CoordinationScope,
): CoordinationTable {
  // "Els meus voluntaris" resolves to a uid list once and is then applied to both queries,
  // so the hours and the rows can never be filtered by different definitions of "mine".
  const mine = filters.onlyMine
    ? myVolunteerUids(scope.myVolunteerAreas, filters.period.from)
    : null

  const hours = hoursByUserArea(filters.period, scope.queryAreas, mine)
  const rules = commitmentRulesValidAt(filters.period.from)

  const users = candidateUsers(filters, scope, hours, mine)

  const charges = scope.showPayments ? chargesByUserItem(filters.period, scope.today) : new Map()
  const carryCharges = scope.showPayments ? chargeTotalsBefore(filters.period.from) : new Map<string, number>()
  const credits = scope.showPayments ? creditsInPeriod(filters.period) : new Map<string, { total: number; payments: number }>()
  const carryCredits = scope.showPayments ? creditTotalsBefore(filters.period.from) : new Map<string, number>()
  const appPayments = scope.showPayments ? appPaymentState(filters.period) : new Map()

  // Columns are the areas with actual hours, plus any area a listed volunteer has a
  // commitment for — a target of 8h with 0h done is exactly what a coordinator needs to
  // see, and it would be invisible if columns came from the hours alone.
  const columns = new Set<string>()
  for (const areaMap of hours.values()) for (const area of areaMap.keys()) columns.add(area)

  const rows: VolunteerRow[] = []
  for (const u of users) {
    const who = { uid: u.uid, volunteerType: u.volunteerType }

    for (const rule of rules) {
      if (rule.area === AREA_TOTAL) continue
      if (ruleMatchesVolunteer(rule, who) && (scope.allowedAreas === null || scope.allowedAreas.includes(rule.area))) {
        columns.add(rule.area)
      }
    }

    const minutesByArea = hours.get(u.uid) ?? new Map<string, number>()
    const totalMinutes = [...minutesByArea.values()].reduce((a, b) => a + b, 0)

    const progressByArea = new Map<string, Progress>()
    for (const area of columns) {
      const done = minutesByArea.get(area) ?? 0
      const commitment = resolveCommitment(rules, who, area, filters.period)
      if (done === 0 && !commitment) continue
      progressByArea.set(area, progressOf(done, commitment, scope.atRiskRatio))
    }

    const totalProgress = progressOf(
      totalMinutes,
      resolveCommitment(rules, who, AREA_TOTAL, filters.period),
      scope.atRiskRatio,
    )

    const items = (charges.get(u.uid) ?? new Map()) as Map<string, ItemRow>
    const chargesCents = [...items.values()].reduce((a, r) => a + r.amountCents, 0)
    const unpriced = [...items.values()].reduce((a, r) => a + r.unpriced, 0)
    const cr = credits.get(u.uid) ?? { total: 0, payments: 0 }

    const balance = balanceOf({
      chargesCents,
      creditsCents: cr.total,
      paymentsCents: cr.payments,
      carryInCents: (carryCharges.get(u.uid) ?? 0) - (carryCredits.get(u.uid) ?? 0),
    })

    const app = appPayments.get(u.uid) ?? null

    rows.push({
      uid: u.uid,
      name: u.name || 'Sense nom',
      email: u.email,
      role: u.role,
      volunteerType: u.volunteerType,
      isMember: u.isMember,
      userMissing: u.deleted,
      minutesByArea,
      totalMinutes,
      progressByArea,
      totalProgress,
      items: new Map([...items].map(([k, v]) => [k, { done: v.qtyDone, upcoming: v.qtyUpcoming }])),
      unpricedItems: unpriced,
      balance,
      settlement: settlementOf(balance),
      appPaid: app?.paid ?? null,
      appAmountCents: app?.amountCents ?? null,
    })
  }

  if (!filters.showEmpty) {
    // Drop volunteers with nothing at all this period, but keep anyone who owes money or
    // has an unmet commitment — those are the rows the page exists for.
    for (let i = rows.length - 1; i >= 0; i--) {
      const r = rows[i]
      const nothing =
        r.totalMinutes === 0 &&
        r.balance.chargesCents === 0 &&
        r.balance.creditsCents === 0 &&
        r.balance.carryInCents === 0 &&
        r.totalProgress.status === 'none' &&
        [...r.progressByArea.values()].every((p) => p.status === 'none')
      if (nothing) rows.splice(i, 1)
    }
  }

  rows.sort((a, b) => a.name.localeCompare(b.name, 'ca'))

  const areaColumns = orderColumns(columns)

  const totals = {
    minutesByArea: new Map<string, number>(),
    totalMinutes: 0,
    chargesCents: 0,
    creditsCents: 0,
    owedTotalCents: 0,
    items: new Map<string, ItemCounts>(),
  }
  for (const r of rows) {
    for (const [area, m] of r.minutesByArea) {
      totals.minutesByArea.set(area, (totals.minutesByArea.get(area) ?? 0) + m)
    }
    totals.totalMinutes += r.totalMinutes
    totals.chargesCents += r.balance.chargesCents
    totals.creditsCents += r.balance.creditsCents
    totals.owedTotalCents += r.balance.owedTotalCents
    for (const [item, c] of r.items) {
      const cur = totals.items.get(item) ?? { done: 0, upcoming: 0 }
      totals.items.set(item, { done: cur.done + c.done, upcoming: cur.upcoming + c.upcoming })
    }
  }

  return {
    rows,
    areaColumns,
    totals,
    showPayments: scope.showPayments,
    duplicatePayments: scope.showPayments ? duplicatePaymentDocs() : [],
  }
}

/** General first (it is everyone's), then areas alphabetically, unknown always last. */
function orderColumns(columns: Set<string>): string[] {
  const rest = [...columns].filter((c) => c !== AREA_GENERAL && c !== AREA_UNKNOWN).sort()
  const out: string[] = []
  if (columns.has(AREA_GENERAL)) out.push(AREA_GENERAL)
  out.push(...rest)
  if (columns.has(AREA_UNKNOWN)) out.push(AREA_UNKNOWN)
  return out
}

// --- the individual queries --------------------------------------------------

/**
 * Minutes per (volunteer, area). `fs_booking_shift.area` is already '__general__' for a
 * general shift, so no CASE is needed — and using the shift's own area is the only correct
 * attribution, since a user's `specific_areas` changes over time.
 */
export function hoursByUserArea(
  period: Period,
  areas: string[] | null,
  /** From myVolunteerUids(); null means the filter is off. Empty means nobody, not everybody. */
  onlyUids: string[] | null,
): Map<string, Map<string, number>> {
  const parts = ['b.deleted_at IS NULL', 'b.service_date >= ?', 'b.service_date < ?']
  const args: unknown[] = [period.from, period.to]

  if (areas !== null) {
    if (areas.length === 0) return new Map()
    parts.push(`s.area IN ${inList(areas)}`)
    args.push(...areas)
  }
  if (onlyUids !== null) {
    if (onlyUids.length === 0) return new Map()
    parts.push(`b.user_id IN ${inList(onlyUids)}`)
    args.push(...onlyUids)
  }

  const rows = raw()
    .prepare(
      `SELECT b.user_id AS uid, s.area AS area, SUM(s.minutes) AS minutes
         FROM fs_booking b
         JOIN fs_booking_shift s ON s.doc_id = b.doc_id
        WHERE ${parts.join(' AND ')}
        GROUP BY b.user_id, s.area`,
    )
    .all(...args) as Array<{ uid: string; area: string; minutes: number }>

  const out = new Map<string, Map<string, number>>()
  for (const r of rows) {
    let m = out.get(r.uid)
    if (!m) {
      m = new Map()
      out.set(r.uid, m)
    }
    m.set(r.area, (m.get(r.area) ?? 0) + r.minutes)
  }
  return out
}

interface UserRow {
  uid: string
  name: string
  email: string
  role: string
  volunteerType: string | null
  isMember: boolean
  deleted: boolean
}

/**
 * Who gets a row. Anyone with hours in the period always does — including a volunteer
 * whose user document has since disappeared, because their hours still happened.
 * `showEmpty` widens it to everyone who could plausibly volunteer.
 */
function candidateUsers(
  filters: CoordinationFilters,
  scope: CoordinationScope,
  hours: Map<string, Map<string, number>>,
  mine: string[] | null,
): UserRow[] {
  const parts: string[] = []
  const args: unknown[] = []

  if (filters.types.length > 0) {
    parts.push(`u.volunteer_type IN ${inList(filters.types)}`)
    args.push(...filters.types)
  }
  if (mine !== null) {
    if (mine.length === 0) return []
    parts.push(`u.uid IN ${inList(mine)}`)
    args.push(...mine)
  } else if (!scope.showPayments && scope.allowedAreas !== null) {
    // An area_responsible's table is their areas: volunteers who WORKED those areas in the
    // period, plus everyone the areas belong to (listed in them, or carrying a commitment
    // aimed at one of them). Enforced here, in the query.
    //
    // All of it, and not membership alone, for two reasons: hours that happened in the area
    // must appear even if the volunteer has since removed it from their profile — otherwise
    // the column silently under-reports its own area — and this set is a superset of
    // `myVolunteerUids`, so ticking "només els meus voluntaris" can only ever narrow the
    // table, never add to it.
    if (scope.allowedAreas.length === 0) return []
    const visible = [
      ...new Set([...hours.keys(), ...myVolunteerUids(scope.allowedAreas, filters.period.from)]),
    ]
    if (visible.length === 0) return []
    parts.push(`u.uid IN ${inList(visible)}`)
    args.push(...visible)
  }

  const where = parts.length ? `WHERE ${parts.join(' AND ')}` : ''
  const all = raw()
    .prepare(
      `SELECT u.uid, u.name, u.email, u.role, u.volunteer_type AS volunteerType,
              u.is_member AS isMember, u.deleted_at AS deletedAt
         FROM fs_user u
         ${where}`,
    )
    .all(...args) as Array<Record<string, unknown>>

  const byUid = new Map<string, UserRow>()
  for (const r of all) {
    const uid = r.uid as string
    // A soft-deleted user is only interesting if they have activity in the period.
    if (r.deletedAt !== null && !hours.has(uid)) continue
    byUid.set(uid, {
      uid,
      name: (r.name as string) ?? '',
      email: (r.email as string) ?? '',
      role: (r.role as string) ?? 'unknown',
      volunteerType: (r.volunteerType as string | null) ?? null,
      isMember: Boolean(r.isMember),
      deleted: r.deletedAt !== null,
    })
  }

  // Bookings whose user_id resolves to nothing at all: still shown, flagged.
  for (const uid of hours.keys()) {
    if (!byUid.has(uid) && filters.types.length === 0 && !filters.onlyMine) {
      byUid.set(uid, {
        uid,
        name: `Voluntari desconegut (${uid.slice(0, 6)}…)`,
        email: '',
        role: 'unknown',
        volunteerType: null,
        isMember: false,
        deleted: true,
      })
    }
  }

  return [...byUid.values()]
}

interface ItemRow {
  qtyDone: number
  qtyUpcoming: number
  amountCents: number
  unpriced: number
}

export function chargesByUserItem(period: Period, today: string): Map<string, Map<string, ItemRow>> {
  const rows = raw()
    .prepare(
      `SELECT c.user_id AS uid, c.item,
              SUM(CASE WHEN c.service_date <= ? THEN c.qty ELSE 0 END) AS qtyDone,
              SUM(CASE WHEN c.service_date >  ? THEN c.qty ELSE 0 END) AS qtyUpcoming,
              SUM(c.amount_cents)                                      AS amountCents,
              SUM(CASE WHEN c.has_price = 0 THEN c.qty ELSE 0 END)     AS unpriced
         FROM v_charge_effective c
        WHERE c.service_date >= ? AND c.service_date < ?
        GROUP BY c.user_id, c.item`,
    )
    .all(today, today, period.from, period.to) as Array<{ uid: string; item: string } & ItemRow>

  const out = new Map<string, Map<string, ItemRow>>()
  for (const r of rows) {
    let m = out.get(r.uid)
    if (!m) {
      m = new Map()
      out.set(r.uid, m)
    }
    m.set(r.item, {
      qtyDone: r.qtyDone,
      qtyUpcoming: r.qtyUpcoming,
      amountCents: r.amountCents,
      unpriced: r.unpriced,
    })
  }
  return out
}

function chargeTotalsBefore(date: string): Map<string, number> {
  const rows = raw()
    .prepare(
      `SELECT user_id AS uid, SUM(amount_cents) AS cents
         FROM v_charge_effective WHERE service_date < ? GROUP BY user_id`,
    )
    .all(date) as Array<{ uid: string; cents: number }>
  return new Map(rows.map((r) => [r.uid, r.cents]))
}

function creditsInPeriod(period: Period): Map<string, { total: number; payments: number }> {
  const rows = raw()
    .prepare(
      `SELECT user_id AS uid,
              SUM(amount_cents)                                            AS total,
              SUM(CASE WHEN kind = 'payment' THEN amount_cents ELSE 0 END) AS payments
         FROM ledger_entry
        WHERE effective_date >= ? AND effective_date < ?
        GROUP BY user_id`,
    )
    .all(period.from, period.to) as Array<{ uid: string; total: number; payments: number }>
  return new Map(rows.map((r) => [r.uid, { total: r.total, payments: r.payments }]))
}

function creditTotalsBefore(date: string): Map<string, number> {
  const rows = raw()
    .prepare(
      `SELECT user_id AS uid, SUM(amount_cents) AS cents
         FROM ledger_entry WHERE effective_date < ? GROUP BY user_id`,
    )
    .all(date) as Array<{ uid: string; cents: number }>
  return new Map(rows.map((r) => [r.uid, r.cents]))
}

/**
 * What the app itself believes, summed over the months the period covers. Only the
 * canonical doc counts — the one `.firstOrNull()` returns on the phone. Mirrored for
 * drift detection only; `payments.amount` is corrupt by construction and is never input.
 */
export function appPaymentState(
  period: Period,
): Map<string, { paid: boolean; amountCents: number }> {
  const keys = monthsIn(period).map(([y, m]) => y * 100 + m)
  if (keys.length === 0) return new Map()

  const rows = raw()
    .prepare(
      `SELECT user_id AS uid, paid, amount_cents AS amountCents
         FROM v_payment_canonical
        WHERE is_canonical = 1 AND (year * 100 + month) IN ${inList(keys)}`,
    )
    .all(...keys) as Array<{ uid: string; paid: number; amountCents: number }>

  const out = new Map<string, { paid: boolean; amountCents: number }>()
  for (const r of rows) {
    const cur = out.get(r.uid) ?? { paid: true, amountCents: 0 }
    // "paid" for a quarter means every month in it is paid.
    out.set(r.uid, {
      paid: cur.paid && Boolean(r.paid),
      amountCents: cur.amountCents + r.amountCents,
    })
  }
  return out
}

function duplicatePaymentDocs() {
  return raw()
    .prepare(
      `SELECT p.user_id AS uid, COALESCE(u.name, p.user_id) AS name, p.year, p.month, COUNT(*) AS count
         FROM v_payment_canonical p
         LEFT JOIN fs_user u ON u.uid = p.user_id
        GROUP BY p.user_id, p.year, p.month
       HAVING COUNT(*) > 1
        ORDER BY p.year DESC, p.month DESC`,
    )
    .all() as Array<{ uid: string; name: string; year: number; month: number; count: number }>
}

/**
 * Volunteers whose charges for this period are frozen. A closed period still shows its
 * numbers; they just no longer move when a price is corrected.
 */
export function closedFor(period: Period): Map<string, { closedAt: number; totalCents: number }> {
  const rows = raw()
    .prepare(
      `SELECT user_id AS uid, closed_at AS closedAt, total_cents AS totalCents
         FROM period_close
        WHERE period_kind = ? AND period_year = ? AND period_index = ?`,
    )
    .all(period.kind, period.year, period.index) as Array<{
    uid: string
    closedAt: number
    totalCents: number
  }>
  return new Map(rows.map((r) => [r.uid, { closedAt: r.closedAt, totalCents: r.totalCents }]))
}

// --- the per-volunteer side panel --------------------------------------------

export interface VolunteerDay {
  serviceDate: string
  minutes: number
  areas: string[]
  hasLunch: boolean
  hasDinner: boolean
  sleep: boolean
  anomalies: number
}

export interface VolunteerCharge {
  serviceDate: string
  item: string
  qty: number
  unitPriceCents: number
  amountCents: number
  locked: boolean
  hasPrice: boolean
}

export interface LedgerRow {
  id: number
  kind: string
  effectiveDate: string
  amountCents: number
  method: string | null
  note: string | null
  createdBy: string
  createdAt: number
  voidsId: number | null
  /** True when a later entry voids this one. */
  voided: boolean
}

export interface VolunteerDetail {
  days: VolunteerDay[]
  charges: VolunteerCharge[]
  ledger: LedgerRow[]
}

export function volunteerDetail(uid: string, period: Period): VolunteerDetail {
  const days = raw()
    .prepare(
      `SELECT b.service_date AS serviceDate,
              b.total_minutes AS minutes,
              b.has_lunch AS hasLunch, b.has_dinner AS hasDinner, b.sleep,
              b.anomaly_flags AS anomalies,
              (SELECT GROUP_CONCAT(DISTINCT s.area) FROM fs_booking_shift s WHERE s.doc_id = b.doc_id) AS areas
         FROM fs_booking b
        WHERE b.deleted_at IS NULL AND b.user_id = ?
          AND b.service_date >= ? AND b.service_date < ?
        ORDER BY b.service_date`,
    )
    .all(uid, period.from, period.to)
    .map((r) => {
      const row = r as Record<string, unknown>
      return {
        serviceDate: row.serviceDate as string,
        minutes: row.minutes as number,
        areas: ((row.areas as string | null) ?? '').split(',').filter(Boolean),
        hasLunch: Boolean(row.hasLunch),
        hasDinner: Boolean(row.hasDinner),
        sleep: Boolean(row.sleep),
        anomalies: row.anomalies as number,
      }
    })

  const charges = raw()
    .prepare(
      `SELECT service_date AS serviceDate, item, qty,
              unit_price_cents AS unitPriceCents, amount_cents AS amountCents,
              locked, has_price AS hasPrice
         FROM v_charge_effective
        WHERE user_id = ? AND service_date >= ? AND service_date < ?
        ORDER BY service_date, item`,
    )
    .all(uid, period.from, period.to)
    .map((r) => {
      const row = r as Record<string, unknown>
      return {
        serviceDate: row.serviceDate as string,
        item: row.item as string,
        qty: row.qty as number,
        unitPriceCents: row.unitPriceCents as number,
        amountCents: row.amountCents as number,
        locked: Boolean(row.locked),
        hasPrice: Boolean(row.hasPrice),
      }
    })

  // The whole ledger, not just the period: a payment history that starts mid-story is
  // worse than none, and this is the only place the carry-over becomes explainable.
  const ledgerRows = raw()
    .prepare(
      `SELECT id, kind, effective_date AS effectiveDate, amount_cents AS amountCents,
              method, note, created_by AS createdBy, created_at AS createdAt,
              voids_id AS voidsId
         FROM ledger_entry
        WHERE user_id = ?
        ORDER BY effective_date DESC, id DESC`,
    )
    .all(uid) as Array<Omit<LedgerRow, 'voided'>>

  const voided = new Set(ledgerRows.map((r) => r.voidsId).filter((v): v is number => v !== null))
  const ledger: LedgerRow[] = ledgerRows.map((r) => ({ ...r, voided: voided.has(r.id) }))

  return { days, charges, ledger }
}
