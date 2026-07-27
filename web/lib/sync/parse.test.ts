import { test } from 'node:test'
import assert from 'node:assert/strict'

import { hashDoc, parseBooking, parsePayment, parseUser, toEpochSeconds } from './parse.ts'
import { ANOMALY } from './anomalies.ts'
import { AREA_GENERAL, AREA_UNKNOWN } from '../contract.ts'

/** What the Kotlin app writes for a normal two-shift day, booked from Spain in May. */
const TS_2026_05_04 = Math.floor(Date.parse('2026-05-03T22:00:00Z') / 1000)

const NORMAL_BOOKING = {
  user_id: 'u1',
  timestamp: { seconds: TS_2026_05_04, nanoseconds: 0 },
  shifts: [
    {
      shift: 'morning',
      time_range: { start: '10:00', end: '14:00' },
      type: { type: 'specific', specific_areas: 'kitchen' },
    },
    {
      shift: 'afternoon',
      time_range: { start: '16:30', end: '20:30' },
      type: { type: 'general', specific_areas: null },
    },
  ],
  meal_types: ['lunch', 'dinner'],
  sleep: true,
}

test('a normal booking parses with no anomalies', () => {
  const b = parseBooking('b1', NORMAL_BOOKING)!
  assert.equal(b.serviceDate, '2026-05-04')
  assert.equal(b.userId, 'u1')
  assert.equal(b.hasLunch, true)
  assert.equal(b.hasDinner, true)
  assert.equal(b.sleep, true)
  assert.equal(b.shiftCount, 2)
  assert.equal(b.totalMinutes, 480)
  assert.equal(b.anomalyFlags, 0)
})

test('morning and afternoon can carry different areas', () => {
  const b = parseBooking('b1', NORMAL_BOOKING)!
  assert.deepEqual(
    b.shifts.map((s) => [s.slot, s.kind, s.area, s.minutes]),
    [
      ['morning', 'specific', 'kitchen', 240],
      ['afternoon', 'general', AREA_GENERAL, 240],
    ],
  )
})

test('a general shift maps to the __general__ bucket, not to a real area', () => {
  const b = parseBooking('b1', NORMAL_BOOKING)!
  const general = b.shifts.find((s) => s.kind === 'general')!
  assert.equal(general.area, AREA_GENERAL)
})

test('SpecificArea.Unknown serialises to "" and must not vanish', () => {
  const b = parseBooking('b2', {
    ...NORMAL_BOOKING,
    shifts: [
      {
        shift: 'morning',
        time_range: { start: '10:00', end: '14:00' },
        type: { type: 'specific', specific_areas: '' },
      },
    ],
  })!
  assert.equal(b.shifts[0].area, AREA_UNKNOWN)
  assert.equal(b.shifts[0].minutes, 240, 'hours must still be counted')
  assert.ok(b.anomalyFlags & ANOMALY.UNKNOWN_AREA)
})

test('an unparseable time yields 0 minutes and a flag, never a wrong number', () => {
  const b = parseBooking('b3', {
    ...NORMAL_BOOKING,
    shifts: [
      {
        shift: 'morning',
        time_range: { start: 'de matí', end: null },
        type: { type: 'general' },
      },
    ],
  })!
  assert.equal(b.totalMinutes, 0)
  assert.ok(b.anomalyFlags & ANOMALY.UNPARSEABLE_TIME)
})

test('an inverted range is refused rather than inflating hours', () => {
  // The app's abs() would report 20h for 22:00 -> 02:00.
  const b = parseBooking('b4', {
    ...NORMAL_BOOKING,
    shifts: [
      {
        shift: 'afternoon',
        time_range: { start: '22:00', end: '02:00' },
        type: { type: 'general' },
      },
    ],
  })!
  assert.equal(b.totalMinutes, 0)
  assert.ok(b.anomalyFlags & ANOMALY.IMPLAUSIBLE_RANGE)
})

test('unknown slot and unknown meal are flagged but not fatal', () => {
  const b = parseBooking('b5', {
    ...NORMAL_BOOKING,
    shifts: [
      { shift: 'night', time_range: { start: '20:00', end: '23:00' }, type: { type: 'general' } },
    ],
    meal_types: ['lunch', 'breakfast', ''],
  })!
  assert.equal(b.shifts[0].slot, 'unknown')
  assert.equal(b.shifts[0].minutes, 180, 'the hours still count')
  assert.ok(b.anomalyFlags & ANOMALY.UNKNOWN_SLOT)
  assert.ok(b.anomalyFlags & ANOMALY.UNKNOWN_MEAL)
  assert.equal(b.hasLunch, true)
  assert.equal(b.hasDinner, false)
  assert.equal(b.mealsRawJson, '["lunch","breakfast",""]', 'raw value preserved')
})

test('a booking with no shifts is stored and flagged', () => {
  const b = parseBooking('b6', { ...NORMAL_BOOKING, shifts: [] })!
  assert.equal(b.shiftCount, 0)
  assert.ok(b.anomalyFlags & ANOMALY.NO_SHIFTS)
})

test('a booking made from another timezone is flagged but still lands on the right day', () => {
  // Tokyo (UTC+9) midnight on the 4th = 15:00Z on the 3rd.
  const tokyo = Math.floor(Date.parse('2026-05-03T15:00:00Z') / 1000)
  const b = parseBooking('b7', { ...NORMAL_BOOKING, timestamp: { seconds: tokyo } })!
  assert.equal(b.serviceDate, '2026-05-04')
  assert.ok(b.anomalyFlags & ANOMALY.FOREIGN_TIMEZONE)
})

test('a booking with no timestamp is skipped rather than mis-dated', () => {
  assert.equal(parseBooking('b8', { ...NORMAL_BOOKING, timestamp: null }), null)
})

test('toEpochSeconds accepts every plausible timestamp encoding', () => {
  const expected = TS_2026_05_04
  assert.equal(toEpochSeconds({ seconds: expected, nanoseconds: 0 }), expected)
  assert.equal(toEpochSeconds({ _seconds: expected }), expected)
  assert.equal(toEpochSeconds(new Date(expected * 1000)), expected)
  assert.equal(toEpochSeconds(expected * 1000), expected)
  assert.equal(toEpochSeconds('2026-05-03T22:00:00Z'), expected)
  assert.equal(toEpochSeconds({ toDate: () => new Date(expected * 1000) }), expected)
  assert.equal(toEpochSeconds(undefined), null)
  assert.equal(toEpochSeconds('not a date'), null)
})

test('the doc hash is stable under key reordering but not under value change', () => {
  const a = hashDoc({ x: 1, y: [1, 2], z: { b: 2, a: 1 } })
  const b = hashDoc({ z: { a: 1, b: 2 }, y: [1, 2], x: 1 })
  assert.equal(a, b)
  assert.notEqual(a, hashDoc({ x: 1, y: [2, 1], z: { a: 1, b: 2 } }))
})

test('users parse, with unknown roles and types normalised the way the app does', () => {
  const u = parseUser('u1', {
    id: 'u1',
    name: 'Alba Giró',
    email: 'Alba@Example.CAT',
    role: 'volunteer',
    volunteer_type: 'mitra',
    onboarding_completed: true,
    specific_areas: ['kitchen', 'temple', 'kitchen'],
    is_member: true,
  })
  assert.equal(u.email, 'alba@example.cat')
  assert.equal(u.role, 'volunteer')
  assert.equal(u.volunteerType, 'mitra')
  assert.deepEqual(u.areas, ['kitchen', 'temple'], 'deduplicated')

  const odd = parseUser('u2', { role: 'admin', volunteer_type: 'super', specific_areas: 'nope' })
  assert.equal(odd.role, 'unknown')
  assert.equal(odd.volunteerType, null)
  assert.deepEqual(odd.areas, [])
  assert.equal(odd.isMember, false)
})

test('payments convert to cents exactly once, with no float drift', () => {
  const p = parsePayment('p1', { user_id: 'u1', year: 2026, month: 5, paid: false, amount: 26.1 })!
  assert.equal(p.amountCents, 2610)
  assert.equal(p.amountRaw, 26.1)
  assert.equal(parsePayment('p2', { year: 2026, month: 13 }), null)
  assert.equal(parsePayment('p3', { year: 2026 }), null)
})
