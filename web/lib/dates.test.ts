import { test } from 'node:test'
import assert from 'node:assert/strict'

import {
  addMonths,
  epochSecondsAtLocalMidnight,
  formatMinutes,
  monthPeriod,
  monthsIn,
  offsetFromLocalMidnight,
  parseSecondOfDay,
  periodContaining,
  previousPeriod,
  quarterOf,
  quarterPeriod,
  serviceDate,
  shiftMinutes,
} from './dates.ts'

/** Exactly what the Kotlin app writes: LocalDate.atStartOfDayIn(deviceZone). */
function appWrites(date: string, tz: string): number {
  const [y, m, d] = date.split('-').map(Number)
  const guess = Date.UTC(y, m - 1, d) / 1000
  const asUTC = new Date(new Date(guess * 1000).toLocaleString('en-US', { timeZone: 'UTC' }))
  const asLocal = new Date(new Date(guess * 1000).toLocaleString('en-US', { timeZone: tz }))
  return guess - Math.round((asLocal.getTime() - asUTC.getTime()) / 1000)
}

test('a summer booking made in Spain resolves to the booked day', () => {
  // CEST (UTC+2): midnight on the 4th is stored as 22:00Z on the 3rd.
  const ts = appWrites('2026-05-04', 'Europe/Madrid')
  assert.equal(new Date(ts * 1000).toISOString(), '2026-05-03T22:00:00.000Z')
  assert.equal(serviceDate(ts), '2026-05-04')
})

test('a winter booking made in Spain resolves to the booked day', () => {
  // CET (UTC+1): midnight on the 15th is stored as 23:00Z on the 14th.
  const ts = appWrites('2026-01-15', 'Europe/Madrid')
  assert.equal(new Date(ts * 1000).toISOString(), '2026-01-14T23:00:00.000Z')
  assert.equal(serviceDate(ts), '2026-01-15')
})

test('the first of the month does not fall into the previous month', () => {
  // This is the case that silently corrupts monthly totals if read as UTC.
  const ts = appWrites('2026-05-01', 'Europe/Madrid')
  assert.equal(new Date(ts * 1000).toISOString().slice(0, 10), '2026-04-30')
  assert.equal(serviceDate(ts), '2026-05-01')
})

test('bookings made from other timezones still resolve correctly', () => {
  for (const tz of ['UTC', 'Asia/Tokyo', 'America/New_York', 'Pacific/Auckland']) {
    assert.equal(serviceDate(appWrites('2026-05-04', tz)), '2026-05-04', tz)
    assert.equal(serviceDate(appWrites('2026-01-01', tz)), '2026-01-01', tz)
  }
})

test('offsetFromLocalMidnight is 0 for a Spanish booking, non-zero otherwise', () => {
  assert.equal(offsetFromLocalMidnight(appWrites('2026-05-04', 'Europe/Madrid')), 0)
  assert.equal(offsetFromLocalMidnight(appWrites('2026-05-04', 'Asia/Tokyo')), -7 * 60)
})

test('epochSecondsAtLocalMidnight round-trips across the DST boundary', () => {
  for (const d of ['2026-01-15', '2026-03-29', '2026-03-30', '2026-10-25', '2026-07-01']) {
    assert.equal(serviceDate(epochSecondsAtLocalMidnight(d)), d, d)
  }
})

test('quarterOf matches the app formula (month + 2) / 3', () => {
  assert.deepEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map(quarterOf),
    [1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4])
})

test('addMonths crosses year boundaries', () => {
  assert.equal(addMonths('2026-11-01', 3), '2027-02-01')
  assert.equal(addMonths('2026-01-01', -1), '2025-12-01')
})

test('periods are half-open and quarters span three months', () => {
  const may = monthPeriod(2026, 5)
  assert.equal(may.from, '2026-05-01')
  assert.equal(may.to, '2026-06-01')
  assert.equal(may.label, 'Maig 2026')

  const q2 = quarterPeriod(2026, 2)
  assert.equal(q2.from, '2026-04-01')
  assert.equal(q2.to, '2026-07-01')
  assert.deepEqual(monthsIn(q2), [[2026, 4], [2026, 5], [2026, 6]])
})

test('periodContaining and previousPeriod', () => {
  assert.equal(periodContaining('quarter', '2026-05-04').index, 2)
  assert.equal(previousPeriod(monthPeriod(2026, 1)).label, 'Desembre 2025')
  assert.equal(previousPeriod(quarterPeriod(2026, 1)).label, 'T4 2025')
})

test('parseSecondOfDay accepts the plausible encodings and rejects junk', () => {
  assert.equal(parseSecondOfDay('10:00'), 36000)
  assert.equal(parseSecondOfDay('10:00:00'), 36000)
  assert.equal(parseSecondOfDay('16:30'), 59400)
  assert.equal(parseSecondOfDay('09:05:30'), 32730)
  assert.equal(parseSecondOfDay(36000), 36000)
  for (const junk of ['', 'morning', '25:00', '10:70', null, undefined, {}, 999999]) {
    assert.equal(parseSecondOfDay(junk), null, JSON.stringify(junk))
  }
})

test('shiftMinutes mirrors the app but refuses implausible ranges', () => {
  assert.deepEqual(shiftMinutes(36000, 50400), { minutes: 240, implausible: false })
  assert.deepEqual(shiftMinutes(59400, 73800), { minutes: 240, implausible: false })
  // inverted range: the app's abs() would give 20h — we refuse it
  assert.deepEqual(shiftMinutes(79200, 7200), { minutes: 0, implausible: true })
  assert.deepEqual(shiftMinutes(36000, 36000), { minutes: 0, implausible: true })
  assert.deepEqual(shiftMinutes(null, 36000), { minutes: 0, implausible: true })
})

test('formatMinutes', () => {
  assert.equal(formatMinutes(240), '4h')
  assert.equal(formatMinutes(270), '4h 30m')
  assert.equal(formatMinutes(45), '45m')
  assert.equal(formatMinutes(0), '0h')
})
