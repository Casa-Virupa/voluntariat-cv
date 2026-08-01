/**
 * Firestore document -> mirror row. Pure functions over plain objects, so the whole
 * parsing surface is unit-testable without touching Firebase.
 *
 * Guiding rule: NEVER throw and NEVER silently drop. A document we cannot fully
 * understand still produces a row, with anomaly flags describing what was wrong. A
 * volunteer's hours going missing because of an unexpected string is far worse than a
 * row that admits it is odd.
 */

import { createHash } from 'node:crypto'

import {
  AREA_GENERAL,
  AREA_UNKNOWN,
  isKnownArea,
  PERSISTED_MEALS,
  USER_ROLES,
  VOLUNTEER_TYPES,
} from '../contract.ts'
import { offsetFromLocalMidnight, parseSecondOfDay, serviceDate, shiftMinutes } from '../dates.ts'
import { ANOMALY } from './anomalies.ts'

export function hashDoc(value: unknown): string {
  return createHash('sha256').update(stableStringify(value)).digest('hex').slice(0, 32)
}

/** Key-sorted JSON so an unchanged document always hashes the same. */
export function stableStringify(value: unknown): string {
  if (value === null || typeof value !== 'object') return JSON.stringify(value) ?? 'null'
  if (Array.isArray(value)) return `[${value.map(stableStringify).join(',')}]`
  const entries = Object.entries(value as Record<string, unknown>).sort(([a], [b]) =>
    a < b ? -1 : a > b ? 1 : 0,
  )
  return `{${entries.map(([k, v]) => `${JSON.stringify(k)}:${stableStringify(v)}`).join(',')}}`
}

// --- users -------------------------------------------------------------------

export interface ParsedUser {
  uid: string
  name: string
  email: string
  role: string
  volunteerType: string | null
  onboardingCompleted: boolean
  isMember: boolean
  areas: string[]
  rawJson: string
  docHash: string
}

export function parseUser(uid: string, data: Record<string, unknown>): ParsedUser {
  const roleRaw = str(data.role)
  const typeRaw = str(data.volunteer_type)

  return {
    uid,
    name: str(data.name) ?? '',
    email: (str(data.email) ?? '').toLowerCase(),
    // The app maps anything unrecognised to UserRole.Unknown; mirror that rather than
    // inventing a role.
    role: roleRaw && (USER_ROLES as readonly string[]).includes(roleRaw) ? roleRaw : 'unknown',
    volunteerType:
      typeRaw && (VOLUNTEER_TYPES as readonly string[]).includes(typeRaw) ? typeRaw : null,
    onboardingCompleted: data.onboarding_completed === true,
    isMember: data.is_member === true,
    areas: Array.isArray(data.specific_areas)
      ? [...new Set(data.specific_areas.map((a) => str(a)).filter((a): a is string => !!a))]
      : [],
    rawJson: stableStringify(data),
    docHash: hashDoc(data),
  }
}

// --- bookings ----------------------------------------------------------------

export interface ParsedShift {
  seq: number
  slot: string
  kind: string
  area: string
  startSec: number | null
  endSec: number | null
  minutes: number
}

export interface ParsedBooking {
  docId: string
  userId: string
  tsEpoch: number
  serviceDate: string
  hasLunch: boolean
  hasDinner: boolean
  sleep: boolean
  mealsRawJson: string
  shifts: ParsedShift[]
  shiftCount: number
  totalMinutes: number
  anomalyFlags: number
  rawJson: string
  docHash: string
}

/**
 * `timestamp` arrives from firebase-admin as a Timestamp instance, but tolerate the
 * shapes a raw REST read or a test fixture would produce too.
 */
export function toEpochSeconds(value: unknown): number | null {
  if (value == null) return null
  if (typeof value === 'number') return Math.floor(value > 1e11 ? value / 1000 : value)
  if (value instanceof Date) return Math.floor(value.getTime() / 1000)
  if (typeof value === 'object') {
    const v = value as Record<string, unknown>
    if (typeof v.seconds === 'number') return v.seconds
    if (typeof v._seconds === 'number') return v._seconds
    if (typeof v.toDate === 'function') {
      const d = (v.toDate as () => Date)()
      if (d instanceof Date && !Number.isNaN(d.getTime())) return Math.floor(d.getTime() / 1000)
    }
  }
  if (typeof value === 'string') {
    const t = Date.parse(value)
    if (!Number.isNaN(t)) return Math.floor(t / 1000)
  }
  return null
}

export function parseBooking(
  docId: string,
  data: Record<string, unknown>,
): ParsedBooking | null {
  const tsEpoch = toEpochSeconds(data.timestamp)
  // Without a date the row cannot be placed in any period, so it is the one case where
  // we skip rather than store. The caller counts these.
  if (tsEpoch === null) return null

  let flags = 0

  const date = serviceDate(tsEpoch)
  if (offsetFromLocalMidnight(tsEpoch) !== 0) flags |= ANOMALY.FOREIGN_TIMEZONE

  // meals
  const rawMeals = Array.isArray(data.meal_types) ? data.meal_types.map((m) => str(m) ?? '') : []
  const hasLunch = rawMeals.includes('lunch')
  const hasDinner = rawMeals.includes('dinner')
  if (rawMeals.some((m) => !(PERSISTED_MEALS as readonly string[]).includes(m))) {
    flags |= ANOMALY.UNKNOWN_MEAL
  }

  // shifts
  const rawShifts = Array.isArray(data.shifts) ? data.shifts : []
  if (rawShifts.length === 0) flags |= ANOMALY.NO_SHIFTS

  const shifts: ParsedShift[] = rawShifts.map((raw, seq) => {
    const s = (raw ?? {}) as Record<string, unknown>

    const slotRaw = str(s.shift)
    const slot = slotRaw === 'morning' || slotRaw === 'afternoon' ? slotRaw : 'unknown'
    if (slot === 'unknown') flags |= ANOMALY.UNKNOWN_SLOT

    const range = (s.time_range ?? {}) as Record<string, unknown>
    const startSec = parseSecondOfDay(range.start)
    const endSec = parseSecondOfDay(range.end)
    if (startSec === null || endSec === null) flags |= ANOMALY.UNPARSEABLE_TIME

    const { minutes, implausible } = shiftMinutes(startSec, endSec)
    if (implausible && startSec !== null && endSec !== null) flags |= ANOMALY.IMPLAUSIBLE_RANGE

    const { kind, area, unknownArea } = parseShiftType(s.type)
    if (unknownArea) flags |= ANOMALY.UNKNOWN_AREA

    return { seq, slot, kind, area, startSec, endSec, minutes }
  })

  return {
    docId,
    userId: str(data.user_id) ?? '',
    tsEpoch,
    serviceDate: date,
    hasLunch,
    hasDinner,
    sleep: data.sleep === true,
    mealsRawJson: JSON.stringify(rawMeals),
    shifts,
    shiftCount: shifts.length,
    totalMinutes: shifts.reduce((sum, s) => sum + s.minutes, 0),
    anomalyFlags: flags,
    rawJson: stableStringify(data),
    docHash: hashDoc(data),
  }
}

/**
 * The app writes `{ type: "general" }` or `{ type: "specific", specific_areas: "kitchen" }`.
 * Note the reader in FirebaseVolunteerRepository.kt treats ANY non-"general" string as
 * specific and force-unwraps the area, so odd values do reach production.
 */
function parseShiftType(value: unknown): { kind: string; area: string; unknownArea: boolean } {
  const t = (value ?? {}) as Record<string, unknown>
  const kindRaw = str(t.type)

  if (kindRaw === 'general') {
    return { kind: 'general', area: AREA_GENERAL, unknownArea: false }
  }

  const areaRaw = str(t.specific_areas)
  if (kindRaw === 'specific') {
    if (areaRaw && isKnownArea(areaRaw)) {
      return { kind: 'specific', area: areaRaw, unknownArea: false }
    }
    // SpecificArea.Unknown serialises to "" — a real, reachable value.
    return { kind: 'specific', area: AREA_UNKNOWN, unknownArea: true }
  }

  // Neither "general" nor "specific": keep whatever area we can salvage.
  if (areaRaw && isKnownArea(areaRaw)) {
    return { kind: 'unknown', area: areaRaw, unknownArea: true }
  }
  return { kind: 'unknown', area: AREA_UNKNOWN, unknownArea: true }
}

// --- the ledger ----------------------------------------------------------------

export interface ParsedLedgerEntry {
  docId: string
  userId: string
  date: string
  amountCents: number
  kind: 'payment' | 'adjustment'
  note: string | null
}

const LEDGER_DATE_RE = /^\d{4}-\d{2}-\d{2}$/

/**
 * Unlike bookings, a ledger doc we cannot fully understand is SKIPPED, not imported with
 * flags: importing money wrong is worse than importing it late, and the doc stays in
 * Firestore to be fixed and picked up by the next run.
 */
export function parseLedgerDoc(
  docId: string,
  data: Record<string, unknown>,
): ParsedLedgerEntry | null {
  const userId = str(data.user_id)
  const date = str(data.date)
  const amount = num(data.amount)
  if (!userId || !date || !LEDGER_DATE_RE.test(date)) return null
  if (amount === null || amount === 0) return null

  return {
    docId,
    userId,
    date,
    // Cents, rounded once here so nothing downstream ever compares floats.
    amountCents: Math.round(amount * 100),
    kind: str(data.kind) === 'payment' ? 'payment' : 'adjustment',
    note: str(data.note) || null,
  }
}

// --- helpers -----------------------------------------------------------------

function str(v: unknown): string | null {
  return typeof v === 'string' ? v.trim() : null
}

function num(v: unknown): number | null {
  if (typeof v === 'number' && Number.isFinite(v)) return v
  if (typeof v === 'string' && v.trim() !== '' && Number.isFinite(Number(v))) return Number(v)
  return null
}
