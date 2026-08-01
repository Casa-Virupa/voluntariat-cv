/**
 * Date and period arithmetic. Pure functions — unit-tested, no I/O.
 *
 * Conventions used everywhere in this app:
 *   - a date is TEXT 'YYYY-MM-DD'
 *   - an instant is an integer number of unix SECONDS
 *   - a duration is an integer number of MINUTES
 *   - every range is half-open: `>= from AND < to`
 */

import { CANONICAL_TZ } from './contract.ts'

const parts = new Intl.DateTimeFormat('en-CA', {
  timeZone: CANONICAL_TZ,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

/** 'YYYY-MM-DD' for an instant, as seen in Europe/Madrid. */
export function localDate(epochSeconds: number): string {
  const p = parts.formatToParts(new Date(epochSeconds * 1000))
  const get = (t: string) => p.find((x) => x.type === t)!.value
  return `${get('year')}-${get('month')}-${get('day')}`
}

const dateTimeParts = new Intl.DateTimeFormat('en-GB', {
  timeZone: CANONICAL_TZ,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false,
})

/**
 * 'YYYY-MM-DD HH:MM:SS' in Europe/Madrid, for exports.
 *
 * Written as text and not as an Excel date cell on purpose: ExcelJS serialises a JS Date
 * against UTC, so a 00:30 Madrid audit entry would open as 22:30 the previous day. This
 * format still sorts correctly as a string, which is what an archive actually needs.
 */
export function localDateTime(epochSeconds: number): string {
  const p = dateTimeParts.formatToParts(new Date(epochSeconds * 1000))
  const get = (t: string) => p.find((x) => x.type === t)!.value
  // en-GB gives 24 as the hour at midnight in some runtimes; normalise it to 00.
  const hour = get('hour') === '24' ? '00' : get('hour')
  return `${get('year')}-${get('month')}-${get('day')} ${hour}:${get('minute')}:${get('second')}`
}

/**
 * The date a booking is FOR.
 *
 * `volunteers.timestamp` is midnight in the booker's own device timezone
 * (DateTimeUtils.kt:34), so a Spanish booking for the 4th is stored as 22:00Z or 23:00Z
 * on the 3rd. Reading it as a UTC date would move every booking back a day, and every
 * booking on the 1st into the previous month.
 *
 * Shifting by 12h and then taking the local date rounds to the nearest local midnight,
 * which is correct for any device within ±12h of Madrid and is DST-safe.
 */
export function serviceDate(epochSeconds: number): string {
  return localDate(epochSeconds + 12 * 3600)
}

/**
 * Now, as unix seconds. A named function rather than an inline `Date.now()` in every page:
 * it keeps the clock read out of the render body (which React's purity lint rightly objects
 * to) and gives the one place to look when a "which day is it" question goes wrong.
 */
export function nowSeconds(): number {
  return Math.floor(Date.now() / 1000)
}

/**
 * Today's date in Europe/Madrid, 'YYYY-MM-DD'. This is THE definition of "today" everywhere
 * in the dashboard: the done/pending split, the default period, and the day highlighted on
 * the calendar all come from here, so they cannot disagree with each other.
 */
export function todayInMadrid(): string {
  return localDate(nowSeconds())
}

/**
 * How far the stored instant sits from local midnight of the derived service date, in
 * minutes. Expected: 0 for a booking made in Spain (since it IS local midnight there).
 * Anything else means the booker's device was in another timezone — worth surfacing, not
 * an error.
 */
export function offsetFromLocalMidnight(epochSeconds: number): number {
  const midnight = epochSecondsAtLocalMidnight(serviceDate(epochSeconds))
  return Math.round((epochSeconds - midnight) / 60)
}

/** Inverse of `localDate`: the instant of 00:00 Europe/Madrid on the given date. */
export function epochSecondsAtLocalMidnight(date: string): number {
  const [y, m, d] = date.split('-').map(Number)
  // Guess UTC, then correct by the offset the guess lands in — one iteration is enough
  // because the offset changes by at most 1h and never at 00:00 local in Madrid.
  const guess = Date.UTC(y, m - 1, d) / 1000
  const off = tzOffsetSeconds(guess)
  return guess - off
}

/** Seconds east of UTC for Europe/Madrid at the given instant (+3600 or +7200). */
function tzOffsetSeconds(epochSeconds: number): number {
  const d = new Date(epochSeconds * 1000)
  const utc = new Date(d.toLocaleString('en-US', { timeZone: 'UTC' }))
  const local = new Date(d.toLocaleString('en-US', { timeZone: CANONICAL_TZ }))
  return Math.round((local.getTime() - utc.getTime()) / 1000)
}

// --- plain date arithmetic on 'YYYY-MM-DD' strings ---------------------------

export function addMonths(date: string, n: number): string {
  const [y, m, d] = date.split('-').map(Number)
  const total = y * 12 + (m - 1) + n
  const ny = Math.floor(total / 12)
  const nm = (total % 12) + 1
  return `${pad4(ny)}-${pad2(nm)}-${pad2(d)}`
}

export function firstOfMonth(year: number, month: number): string {
  return `${pad4(year)}-${pad2(month)}-01`
}

/** Matches the app's own integer division, ProfileViewModel.kt:127. */
export function quarterOf(month: number): number {
  return Math.floor((month + 2) / 3)
}

export function firstMonthOfQuarter(quarter: number): number {
  return quarter * 3 - 2
}

export type PeriodKind = 'month' | 'quarter'

export interface Period {
  kind: PeriodKind
  year: number
  /** 1–12 for a month, 1–4 for a quarter. */
  index: number
  /** inclusive */
  from: string
  /** EXCLUSIVE */
  to: string
  label: string
}

const MONTHS_CA = [
  'gener', 'febrer', 'març', 'abril', 'maig', 'juny',
  'juliol', 'agost', 'setembre', 'octubre', 'novembre', 'desembre',
]

export function monthPeriod(year: number, month: number): Period {
  const from = firstOfMonth(year, month)
  return {
    kind: 'month',
    year,
    index: month,
    from,
    to: addMonths(from, 1),
    label: `${capitalise(MONTHS_CA[month - 1])} ${year}`,
  }
}

export function quarterPeriod(year: number, quarter: number): Period {
  const from = firstOfMonth(year, firstMonthOfQuarter(quarter))
  return {
    kind: 'quarter',
    year,
    index: quarter,
    from,
    to: addMonths(from, 3),
    label: `T${quarter} ${year}`,
  }
}

export function periodContaining(kind: PeriodKind, date: string): Period {
  const [y, m] = date.split('-').map(Number)
  return kind === 'month' ? monthPeriod(y, m) : quarterPeriod(y, quarterOf(m))
}

export function previousPeriod(p: Period): Period {
  return p.kind === 'month'
    ? periodContaining('month', addMonths(p.from, -1))
    : periodContaining('quarter', addMonths(p.from, -3))
}

/** The months a period spans, as [year, month] pairs — used for payment write-back. */
export function monthsIn(p: Period): Array<[number, number]> {
  const out: Array<[number, number]> = []
  for (let cur = p.from; cur < p.to; cur = addMonths(cur, 1)) {
    const [y, m] = cur.split('-').map(Number)
    out.push([y, m])
  }
  return out
}

// --- times of day ------------------------------------------------------------

/**
 * Parses the `time_range.start/end` wire value. `kotlinx.datetime.LocalTime` serialises
 * to an ISO string ('10:00' or '10:00:00'), but this has NOT been confirmed against a
 * live document — so anything unrecognised returns null and is flagged as an anomaly
 * rather than silently becoming 0.
 */
export function parseSecondOfDay(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) {
    // A bare number could plausibly be seconds-of-day or nanoseconds; only accept the
    // unambiguous seconds-of-day range.
    return value >= 0 && value < 86400 ? Math.round(value) : null
  }
  if (typeof value !== 'string') return null
  const m = /^(\d{1,2}):(\d{2})(?::(\d{2}))?(?:\.\d+)?$/.exec(value.trim())
  if (!m) return null
  const [h, min, s] = [Number(m[1]), Number(m[2]), Number(m[3] ?? 0)]
  if (h > 23 || min > 59 || s > 59) return null
  return h * 3600 + min * 60 + s
}

/** A shift longer than this is not a real shift — see HistoryViewModel's abs(). */
export const MAX_PLAUSIBLE_SHIFT_MINUTES = 720

/**
 * Minutes worked in a shift. Mirrors HistoryViewModel.kt:237-264, which takes the
 * absolute difference — so an inverted range (22:00→02:00) yields 20h there. We keep the
 * abs() for parity but clamp implausible results to 0 and let the caller flag them,
 * rather than inflating someone's total by 20 hours.
 */
export function shiftMinutes(
  startSec: number | null,
  endSec: number | null,
): { minutes: number; implausible: boolean } {
  if (startSec === null || endSec === null) return { minutes: 0, implausible: true }
  const minutes = Math.round(Math.abs(endSec - startSec) / 60)
  if (minutes === 0 || minutes > MAX_PLAUSIBLE_SHIFT_MINUTES) {
    return { minutes: 0, implausible: true }
  }
  return { minutes, implausible: false }
}

// --- formatting --------------------------------------------------------------

/** 270 -> '4h 30m', 240 -> '4h', 0 -> '0h' */
export function formatMinutes(minutes: number): string {
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  if (m === 0) return `${h}h`
  return h === 0 ? `${m}m` : `${h}h ${m}m`
}

/** 4.5 hours as a number, for the Excel export where a real number is wanted. */
export function minutesToHours(minutes: number): number {
  return Math.round((minutes / 60) * 100) / 100
}

/** -200 -> '-2,00 €' (Catalan formatting: comma decimal, space before the symbol). */
export function formatCents(cents: number): string {
  return new Intl.NumberFormat('ca-ES', { style: 'currency', currency: 'EUR' }).format(
    cents / 100,
  )
}

function pad2(n: number) {
  return String(n).padStart(2, '0')
}
function pad4(n: number) {
  return String(n).padStart(4, '0')
}
function capitalise(s: string) {
  return s.charAt(0).toUpperCase() + s.slice(1)
}
