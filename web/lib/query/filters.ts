/**
 * Reading and writing the filter state, which lives entirely in the URL.
 *
 * That is deliberate: every filtered view is then a plain server render with no client
 * state to desynchronise, a coordinator can bookmark "Cuina, T2" and send the link to
 * someone else, and the Excel export is the same query string with `/api/export` in
 * front of it.
 *
 * Pure functions over plain objects — no `next/navigation`, so this is unit-testable and
 * usable from the export route as well as from the pages.
 */

import { AREA_CODES, AREA_GENERAL, AREA_UNKNOWN, VOLUNTEER_TYPES } from '../contract.ts'
import {
  monthPeriod,
  quarterPeriod,
  quarterOf,
  type Period,
  type PeriodKind,
} from '../dates.ts'

/** What Next hands a page as `searchParams`, once awaited. */
export type RawSearch = Record<string, string | string[] | undefined>

function one(v: string | string[] | undefined): string | null {
  if (Array.isArray(v)) return v.length ? v[v.length - 1] : null
  return v ?? null
}

function intIn(value: string | null, lo: number, hi: number): number | null {
  if (value === null) return null
  const n = Number(value)
  if (!Number.isInteger(n) || n < lo || n > hi) return null
  return n
}

/**
 * An area from the query string is only ever a label for a filter, never a permission —
 * `resolveAreaFilter` in lib/authz.ts decides what the query is actually allowed to read.
 * Still validated here so a hand-edited URL cannot put arbitrary text in a heading.
 */
export function parseArea(value: string | null): string | null {
  if (!value) return null
  if (value === AREA_GENERAL || value === AREA_UNKNOWN) return value
  return AREA_CODES.includes(value) ? value : null
}

export function parseTypes(v: string | string[] | undefined): string[] {
  const raw = Array.isArray(v) ? v : v ? v.split(',') : []
  const wanted = raw.map((s) => s.trim()).filter((s) => (VOLUNTEER_TYPES as readonly string[]).includes(s))
  // Selecting both is the same as selecting neither; normalise so the SQL has no filter.
  return wanted.length === VOLUNTEER_TYPES.length ? [] : [...new Set(wanted)]
}

export function parseSlot(value: string | null): string | null {
  return value === 'morning' || value === 'afternoon' ? value : null
}

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/

export function parseDate(value: string | null): string | null {
  return value && DATE_RE.test(value) ? value : null
}

// --- the calendar ------------------------------------------------------------

export interface CalendarFilters {
  /** Always a month: the view is a month grid. */
  period: Period
  area: string | null
  /** Empty means "every type", including volunteers with no type set. */
  types: string[]
  slot: string | null
  /** area_responsible only: restrict to volunteers currently in their areas. */
  onlyMine: boolean
  /** The day whose detail panel is open, if any. */
  day: string | null
}

export function parseCalendarSearch(search: RawSearch, today: string): CalendarFilters {
  const [ty, tm] = today.split('-').map(Number)
  const year = intIn(one(search.y), 2000, 2100) ?? ty
  const month = intIn(one(search.m), 1, 12) ?? tm

  return {
    period: monthPeriod(year, month),
    area: parseArea(one(search.area)),
    types: parseTypes(search.tipus),
    slot: parseSlot(one(search.torn)),
    onlyMine: one(search.meus) === '1',
    day: parseDate(one(search.dia)),
  }
}

export function calendarQuery(f: CalendarFilters, overrides: Partial<Record<string, string | null>> = {}): string {
  const params: Record<string, string | null> = {
    y: String(f.period.year),
    m: String(f.period.index),
    area: f.area,
    tipus: f.types.length ? f.types.join(',') : null,
    torn: f.slot,
    meus: f.onlyMine ? '1' : null,
    dia: f.day,
    ...overrides,
  }
  return toQuery(params)
}

// --- the coordination table --------------------------------------------------

export interface CoordinationFilters {
  period: Period
  area: string | null
  types: string[]
  onlyMine: boolean
  /** The volunteer whose side panel is open. */
  volunteer: string | null
  /** Hide volunteers with no hours, no charges and no commitment in the period. */
  showEmpty: boolean
}

export function parseCoordinationSearch(search: RawSearch, today: string): CoordinationFilters {
  const [ty, tm] = today.split('-').map(Number)
  const kind: PeriodKind = one(search.p) === 'quarter' ? 'quarter' : 'month'
  const year = intIn(one(search.y), 2000, 2100) ?? ty

  const period =
    kind === 'quarter'
      ? quarterPeriod(year, intIn(one(search.t), 1, 4) ?? quarterOf(tm))
      : monthPeriod(year, intIn(one(search.m), 1, 12) ?? tm)

  return {
    period,
    area: parseArea(one(search.area)),
    types: parseTypes(search.tipus),
    onlyMine: one(search.meus) === '1',
    volunteer: one(search.voluntari),
    showEmpty: one(search.tots) === '1',
  }
}

export function coordinationQuery(
  f: CoordinationFilters,
  overrides: Partial<Record<string, string | null>> = {},
): string {
  const params: Record<string, string | null> = {
    p: f.period.kind,
    y: String(f.period.year),
    m: f.period.kind === 'month' ? String(f.period.index) : null,
    t: f.period.kind === 'quarter' ? String(f.period.index) : null,
    area: f.area,
    tipus: f.types.length ? f.types.join(',') : null,
    meus: f.onlyMine ? '1' : null,
    voluntari: f.volunteer,
    tots: f.showEmpty ? '1' : null,
    ...overrides,
  }
  return toQuery(params)
}

function toQuery(params: Record<string, string | null>): string {
  const sp = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) {
    if (v !== null && v !== undefined && v !== '') sp.set(k, v)
  }
  const s = sp.toString()
  return s ? `?${s}` : ''
}

/** Step a period one unit back or forward, keeping its kind. */
export function shiftPeriod(p: Period, delta: number): Period {
  if (p.kind === 'month') {
    const total = p.year * 12 + (p.index - 1) + delta
    return monthPeriod(Math.floor(total / 12), (total % 12) + 1)
  }
  const total = p.year * 4 + (p.index - 1) + delta
  return quarterPeriod(Math.floor(total / 4), (total % 4) + 1)
}
