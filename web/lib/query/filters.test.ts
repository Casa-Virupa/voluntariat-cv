import assert from 'node:assert/strict'
import { test } from 'node:test'

import {
  calendarQuery,
  coordinationQuery,
  parseCalendarSearch,
  parseCoordinationSearch,
  parseTypes,
  shiftPeriod,
} from './filters.ts'
import { monthPeriod, quarterPeriod } from '../dates.ts'

test('an empty query string means "this month"', () => {
  const f = parseCalendarSearch({}, '2026-07-27')
  assert.equal(f.period.from, '2026-07-01')
  assert.equal(f.period.to, '2026-08-01')
  assert.equal(f.area, null)
})

test('a hand-edited area that does not exist is ignored, not echoed back', () => {
  assert.equal(parseCalendarSearch({ area: 'DROP TABLE' }, '2026-07-27').area, null)
  assert.equal(parseCalendarSearch({ area: 'kitchen' }, '2026-07-27').area, 'kitchen')
  assert.equal(parseCalendarSearch({ area: '__general__' }, '2026-07-27').area, '__general__')
})

test('an out-of-range month falls back to today rather than rendering an empty year', () => {
  assert.equal(parseCalendarSearch({ m: '13' }, '2026-07-27').period.index, 7)
  assert.equal(parseCalendarSearch({ m: '0' }, '2026-07-27').period.index, 7)
  assert.equal(parseCalendarSearch({ m: '3' }, '2026-07-27').period.index, 3)
})

test('selecting every volunteer type is the same as selecting none', () => {
  assert.deepEqual(parseTypes('mitra,habitual'), [])
  assert.deepEqual(parseTypes('mitra'), ['mitra'])
  assert.deepEqual(parseTypes('mitra,bogus'), ['mitra'])
  assert.deepEqual(parseTypes(undefined), [])
})

test('the quarterly selector round-trips through the query string', () => {
  const f = parseCoordinationSearch({ p: 'quarter', y: '2026', t: '2' }, '2026-07-27')
  assert.equal(f.period.kind, 'quarter')
  assert.equal(f.period.from, '2026-04-01')
  assert.equal(f.period.to, '2026-07-01')

  const again = parseCoordinationSearch(
    Object.fromEntries(new URLSearchParams(coordinationQuery(f).slice(1))),
    '2026-07-27',
  )
  assert.deepEqual(again.period, f.period)
})

test('the calendar filters round-trip through the query string', () => {
  const f = parseCalendarSearch(
    { y: '2026', m: '5', area: 'kitchen', tipus: 'mitra', torn: 'morning', meus: '1', dia: '2026-05-04' },
    '2026-07-27',
  )
  const again = parseCalendarSearch(
    Object.fromEntries(new URLSearchParams(calendarQuery(f).slice(1))),
    '2026-07-27',
  )
  assert.deepEqual(again, f)
})

test('a query override can clear a filter as well as set one', () => {
  const f = parseCalendarSearch({ area: 'kitchen', dia: '2026-05-04' }, '2026-07-27')
  assert.ok(!calendarQuery(f, { dia: null }).includes('dia='))
  assert.ok(calendarQuery(f, { area: 'temple' }).includes('area=temple'))
})

test('stepping a month crosses the year boundary in both directions', () => {
  assert.deepEqual(shiftPeriod(monthPeriod(2026, 1), -1), monthPeriod(2025, 12))
  assert.deepEqual(shiftPeriod(monthPeriod(2026, 12), 1), monthPeriod(2027, 1))
})

test('stepping a quarter crosses the year boundary in both directions', () => {
  assert.deepEqual(shiftPeriod(quarterPeriod(2026, 1), -1), quarterPeriod(2025, 4))
  assert.deepEqual(shiftPeriod(quarterPeriod(2026, 4), 1), quarterPeriod(2027, 1))
})

test('a malformed day parameter cannot open a panel for a nonexistent date', () => {
  assert.equal(parseCalendarSearch({ dia: '2026-5-4' }, '2026-07-27').day, null)
  assert.equal(parseCalendarSearch({ dia: 'ahir' }, '2026-07-27').day, null)
})
