/**
 * The month-grid queries behind /calendari. Reads only the mirror — no dashboard-owned
 * concepts appear here at all.
 *
 * Written as raw SQL rather than Drizzle's builder because every one of these is a
 * grouped aggregate with an optional IN-list, which the builder expresses less legibly
 * than the SQL itself. All values are bound; nothing is interpolated.
 *
 * Two rules the SQL enforces and the UI must not be trusted to:
 *
 *   - An area_responsible's scope is a WHERE clause. Hiding rows client-side is not
 *     access control, and someone will edit the URL.
 *   - Hours are attributed via fs_booking_shift.area, frozen at booking time. Grouping by
 *     the user's CURRENT areas would silently rewrite history every time somebody joins
 *     a new area.
 */

import { raw } from '../db/index.ts'
import { AREA_GENERAL } from '../contract.ts'
import { ANOMALY_AFFECTS_NUMBERS } from '../sync/anomalies.ts'
import { myVolunteerUids } from './scope.ts'
import type { CalendarFilters } from './filters.ts'

/** Turns a list into `(?, ?, ?)` plus its bindings. Never called with an empty list. */
function inList(values: string[]): string {
  return `(${values.map(() => '?').join(', ')})`
}

interface ShiftScope {
  sql: string
  args: unknown[]
}

/**
 * The shared WHERE fragment. `allowedAreas` is the authorisation scope (null = every
 * area, i.e. a coordinator); `filters.area` is the user's own choice, already narrowed to
 * the allowed set by resolveAreaFilter().
 */
function scopeOf(
  filters: CalendarFilters,
  allowedAreas: string[] | null,
  myVolunteerAreas: string[],
  /** Restrict to a single day instead of the whole period — the detail panel. */
  onDay?: string,
): ShiftScope {
  const parts: string[] = ['b.deleted_at IS NULL']
  const args: unknown[] = []

  if (onDay) {
    parts.push('b.service_date = ?')
    args.push(onDay)
  } else {
    parts.push('b.service_date >= ?', 'b.service_date < ?')
    args.push(filters.period.from, filters.period.to)
  }

  if (allowedAreas !== null) {
    if (allowedAreas.length === 0) {
      // An area_responsible with no areas assigned sees nothing, and must not accidentally
      // see everything because an empty IN-list was dropped.
      return { sql: '1 = 0', args: [] }
    }
    parts.push(`s.area IN ${inList(allowedAreas)}`)
    args.push(...allowedAreas)
  }

  if (filters.slot) {
    parts.push('s.slot = ?')
    args.push(filters.slot)
  }

  if (filters.types.length > 0) {
    parts.push(`u.volunteer_type IN ${inList(filters.types)}`)
    args.push(...filters.types)
  }

  if (filters.onlyMine) {
    // Resolved here rather than passed in so /calendari and /coordinacio cannot end up with
    // two different ideas of whose volunteers these are — see lib/query/scope.ts.
    const mine = myVolunteerUids(myVolunteerAreas, filters.period.from)
    if (mine.length === 0) {
      return { sql: '1 = 0', args: [] }
    }
    parts.push(`b.user_id IN ${inList(mine)}`)
    args.push(...mine)
  }

  return { sql: parts.join(' AND '), args }
}

const FROM = `
  FROM fs_booking b
  JOIN fs_booking_shift s ON s.doc_id = b.doc_id
  -- LEFT, not INNER: a booking whose user document has vanished must still be visible.
  LEFT JOIN fs_user u ON u.uid = b.user_id
`

export interface DayPerson {
  uid: string
  name: string
  volunteerType: string | null
  minutes: number
  shifts: number
  anomalies: number
}

export interface DayCell {
  date: string
  minutes: number
  people: number
  persons: DayPerson[]
}

/**
 * One row per (day, volunteer). Grouped by user_id and not by doc_id, so a volunteer with
 * two bookings on the same day — which the app permits, its guard is client-side — counts
 * as one person with their minutes summed.
 */
export function monthGrid(
  filters: CalendarFilters,
  allowedAreas: string[] | null,
  myVolunteerAreas: string[],
): Map<string, DayCell> {
  const scope = scopeOf(filters, allowedAreas, myVolunteerAreas)

  const rows = raw()
    .prepare(
      `SELECT b.service_date            AS date,
              b.user_id                 AS uid,
              COALESCE(u.name, '')      AS name,
              u.volunteer_type          AS volunteerType,
              SUM(s.minutes)            AS minutes,
              COUNT(*)                  AS shifts,
              MAX(b.anomaly_flags)      AS anomalies
         ${FROM}
        WHERE ${scope.sql}
        GROUP BY b.service_date, b.user_id
        ORDER BY b.service_date, minutes DESC, name`,
    )
    .all(...scope.args) as Array<{
    date: string
    uid: string
    name: string
    volunteerType: string | null
    minutes: number
    shifts: number
    anomalies: number
  }>

  const cells = new Map<string, DayCell>()
  for (const r of rows) {
    let cell = cells.get(r.date)
    if (!cell) {
      cell = { date: r.date, minutes: 0, people: 0, persons: [] }
      cells.set(r.date, cell)
    }
    cell.minutes += r.minutes
    cell.people += 1
    cell.persons.push({
      uid: r.uid,
      name: r.name || 'Sense nom',
      volunteerType: r.volunteerType,
      minutes: r.minutes,
      shifts: r.shifts,
      anomalies: r.anomalies,
    })
  }
  return cells
}

export interface MonthSummary {
  minutes: number
  people: number
  activeDays: number
  bookings: number
  anomalies: number
}

/**
 * COUNT(DISTINCT …) throughout: `(user_id, service_date)` is not unique in Firestore, so
 * COUNT(*) would inflate both the person and the active-day tiles.
 */
export function monthSummary(
  filters: CalendarFilters,
  allowedAreas: string[] | null,
  myVolunteerAreas: string[],
): MonthSummary {
  const scope = scopeOf(filters, allowedAreas, myVolunteerAreas)

  const row = raw()
    .prepare(
      `SELECT COALESCE(SUM(s.minutes), 0)          AS minutes,
              COUNT(DISTINCT b.user_id)            AS people,
              COUNT(DISTINCT b.service_date)       AS activeDays,
              COUNT(DISTINCT b.doc_id)             AS bookings,
              -- Only the flags that distort a number. A booking made from abroad or a second
              -- booking on the same day are normal and handled correctly; counting them here
              -- would keep the tile permanently amber.
              COUNT(DISTINCT CASE WHEN (b.anomaly_flags & ${ANOMALY_AFFECTS_NUMBERS}) != 0
                                  THEN b.doc_id END) AS anomalies
         ${FROM}
        WHERE ${scope.sql}`,
    )
    .get(...scope.args) as MonthSummary

  return row ?? { minutes: 0, people: 0, activeDays: 0, bookings: 0, anomalies: 0 }
}

/**
 * Areas that actually have hours in the period, so the filter dropdown only offers areas
 * worth clicking. Pass the admin's PERMISSION scope (`visibleAreas`) here, not the
 * narrowed filter — otherwise selecting an area would collapse the list to that one area.
 */
export function areasInPeriod(
  filters: CalendarFilters,
  allowedAreas: string[] | null,
): Array<{ area: string; minutes: number }> {
  const scope = scopeOf({ ...filters, area: null, slot: null, onlyMine: false }, allowedAreas, [])
  return raw()
    .prepare(
      `SELECT s.area AS area, SUM(s.minutes) AS minutes
         ${FROM}
        WHERE ${scope.sql}
        GROUP BY s.area
        ORDER BY minutes DESC`,
    )
    .all(...scope.args) as Array<{ area: string; minutes: number }>
}

// --- the day-detail panel ----------------------------------------------------

export interface DayShift {
  docId: string
  uid: string
  name: string
  volunteerType: string | null
  slot: string
  kind: string
  area: string
  startSec: number | null
  endSec: number | null
  minutes: number
  hasLunch: boolean
  hasDinner: boolean
  sleep: boolean
  anomalies: number
}

/**
 * Every shift on one day, one row per shift rather than per booking: morning and
 * afternoon can carry different areas, and the panel groups by time slot.
 */
export function dayDetail(
  date: string,
  filters: CalendarFilters,
  allowedAreas: string[] | null,
  myVolunteerAreas: string[],
): DayShift[] {
  const scope = scopeOf(filters, allowedAreas, myVolunteerAreas, date)

  return raw()
    .prepare(
      `SELECT b.doc_id              AS docId,
              b.user_id             AS uid,
              COALESCE(u.name, '')  AS name,
              u.volunteer_type      AS volunteerType,
              s.slot, s.kind, s.area, s.start_sec AS startSec, s.end_sec AS endSec, s.minutes,
              b.has_lunch           AS hasLunch,
              b.has_dinner          AS hasDinner,
              b.sleep,
              b.anomaly_flags       AS anomalies
         ${FROM}
        WHERE ${scope.sql}
        ORDER BY s.start_sec IS NULL, s.start_sec, name`,
    )
    .all(...scope.args)
    .map((r) => {
      const row = r as Record<string, unknown>
      return {
        docId: row.docId as string,
        uid: row.uid as string,
        name: (row.name as string) || 'Sense nom',
        volunteerType: (row.volunteerType as string | null) ?? null,
        slot: row.slot as string,
        kind: row.kind as string,
        area: row.area as string,
        startSec: (row.startSec as number | null) ?? null,
        endSec: (row.endSec as number | null) ?? null,
        minutes: row.minutes as number,
        hasLunch: Boolean(row.hasLunch),
        hasDinner: Boolean(row.hasDinner),
        sleep: Boolean(row.sleep),
        anomalies: row.anomalies as number,
      }
    })
}

/** Grid geometry: Monday-first weeks covering the whole month. */
export function monthWeeks(year: number, month: number): Array<Array<string | null>> {
  const first = new Date(Date.UTC(year, month - 1, 1))
  const daysInMonth = new Date(Date.UTC(year, month, 0)).getUTCDate()
  // getUTCDay() is 0 for Sunday; Catalan calendars start on Monday.
  const lead = (first.getUTCDay() + 6) % 7

  const cells: Array<string | null> = Array(lead).fill(null)
  for (let d = 1; d <= daysInMonth; d++) {
    cells.push(`${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`)
  }
  while (cells.length % 7 !== 0) cells.push(null)

  const weeks: Array<Array<string | null>> = []
  for (let i = 0; i < cells.length; i += 7) weeks.push(cells.slice(i, i + 7))
  return weeks
}

export const WEEKDAYS_CA = ['dl', 'dt', 'dc', 'dj', 'dv', 'ds', 'dg']

/** 36000 -> '10:00'. Null renders as a dash, never as 00:00. */
export function formatSecondOfDay(sec: number | null): string {
  if (sec === null) return '—'
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

/**
 * '09:00–13:00', or an explicit "unknown" when either bound failed to parse. Composing two
 * dashes with a range dash gives '——13:00', which reads like a corrupt time rather than
 * like the missing value it is.
 */
export function formatRange(startSec: number | null, endSec: number | null): string {
  if (startSec === null || endSec === null) return 'horari desconegut'
  return `${formatSecondOfDay(startSec)}–${formatSecondOfDay(endSec)}`
}

export { AREA_GENERAL }
