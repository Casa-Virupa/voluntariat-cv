/**
 * Integration tests for the apply phase against a real SQLite file. These cover the two
 * ways the mirror could silently lose data: the deletion diff and the mass-delete guard.
 */

import { test, before, after } from 'node:test'
import assert from 'node:assert/strict'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const dir = mkdtempSync(join(tmpdir(), 'cv-sync-'))
process.env.DATABASE_PATH = join(dir, 'test.db')

// Imported dynamically so DATABASE_PATH is set before the db singleton opens.
const { db, raw } = await import('../db/index.ts')
const { runMigrations } = await import('../db/migrate.ts')
const { fsBooking, fsBookingShift } = await import('../db/schema.ts')
const { applyAll, defaultWindowFrom } = await import('./run.ts')
const { parseBooking, parseUser } = await import('./parse.ts')
const { eq, isNull, sql } = await import('drizzle-orm')

before(() => runMigrations())
after(() => rmSync(dir, { recursive: true, force: true }))

const USER = parseUser('u1', {
  name: 'Alba',
  email: 'alba@x.cat',
  role: 'volunteer',
  volunteer_type: 'mitra',
  specific_areas: ['kitchen'],
  is_member: true,
  onboarding_completed: true,
})

function booking(id: string, date: string, area = 'kitchen') {
  const midnight = Math.floor(Date.parse(`${date}T00:00:00+02:00`) / 1000)
  return parseBooking(id, {
    user_id: 'u1',
    timestamp: { seconds: midnight },
    shifts: [
      {
        shift: 'morning',
        time_range: { start: '10:00', end: '14:00' },
        type: { type: 'specific', specific_areas: area },
      },
    ],
    meal_types: ['lunch'],
    sleep: false,
  })!
}

const NOW = 1_800_000_000
const WINDOW = '2026-04-01'

function live() {
  return db.select().from(fsBooking).where(isNull(fsBooking.deletedAt)).all()
}

/** Tests below this point run against a clean mirror rather than inheriting rows. */
function resetBookings() {
  raw().exec('DELETE FROM fs_booking_shift; DELETE FROM fs_booking;')
}

test('a first sync inserts bookings and their shift rows', () => {
  const stats = applyAll({
    now: NOW,
    windowFrom: WINDOW,
    users: [USER],
    bookings: [booking('b1', '2026-05-04'), booking('b2', '2026-05-05')],
    payments: [],
  })
  assert.equal(stats.inserted, 2)
  assert.equal(stats.deleted, 0)
  assert.equal(live().length, 2)
  assert.equal(db.select().from(fsBookingShift).all().length, 2)
})

test('re-syncing unchanged documents is a no-op, not a rewrite', () => {
  const stats = applyAll({
    now: NOW + 1,
    windowFrom: WINDOW,
    users: [USER],
    bookings: [booking('b1', '2026-05-04'), booking('b2', '2026-05-05')],
    payments: [],
  })
  assert.equal(stats.inserted, 0, 'unchanged hashes must skip the child rewrite')
  assert.equal(live().length, 2)
})

test('a booking absent from Firestore is tombstoned', () => {
  const stats = applyAll({
    now: NOW + 2,
    windowFrom: WINDOW,
    users: [USER],
    bookings: [booking('b1', '2026-05-04')],
    payments: [],
  })
  assert.equal(stats.deleted, 1)
  assert.deepEqual(live().map((b) => b.docId), ['b1'])
})

test('a tombstoned booking that reappears is resurrected', () => {
  applyAll({
    now: NOW + 3,
    windowFrom: WINDOW,
    users: [USER],
    bookings: [booking('b1', '2026-05-04'), booking('b2', '2026-05-05')],
    payments: [],
  })
  assert.equal(live().length, 2)
})

test('history OUTSIDE the window is never tombstoned', () => {
  // Seed a booking from long before the window, as a previous full resync would have.
  applyAll({
    now: NOW + 4,
    windowFrom: null, // full mode, so the old row is legitimately inserted
    users: [USER],
    bookings: [
      booking('old', '2024-02-10'),
      booking('b1', '2026-05-04'),
      booking('b2', '2026-05-05'),
    ],
    payments: [],
  })
  assert.equal(live().length, 3)

  // A normal windowed sync does not read 2024 and must not conclude it was deleted.
  const stats = applyAll({
    now: NOW + 5,
    windowFrom: WINDOW,
    users: [USER],
    bookings: [booking('b1', '2026-05-04'), booking('b2', '2026-05-05')],
    payments: [],
  })
  assert.equal(stats.deleted, 0)
  assert.ok(live().some((b) => b.docId === 'old'), 'the 2024 booking must survive')
})

test('the mass-delete guard refuses a truncated fetch and changes nothing', () => {
  // 40 live bookings in the window; a fetch that returns only one must be refused.
  const many = Array.from({ length: 40 }, (_, i) =>
    booking(`m${i}`, `2026-05-${String((i % 28) + 1).padStart(2, '0')}`),
  )
  applyAll({ now: NOW + 6, windowFrom: WINDOW, users: [USER], bookings: many, payments: [] })
  const before = live().length
  assert.ok(before >= 40)

  assert.throws(
    () =>
      applyAll({
        now: NOW + 7,
        windowFrom: WINDOW,
        users: [USER],
        bookings: [booking('m0', '2026-05-01')],
        payments: [],
      }),
    /esborrat massiu/,
  )

  assert.equal(live().length, before, 'the transaction must have rolled back entirely')
})

test('orphan and duplicate-day bookings are flagged, not dropped', () => {
  resetBookings()
  const dup1 = booking('d1', '2026-06-01')
  const dup2 = booking('d2', '2026-06-01')
  applyAll({
    now: NOW + 8,
    windowFrom: WINDOW,
    users: [], // no users at all -> every booking is an orphan
    bookings: [dup1, dup2],
    payments: [],
  })
  const rows = db
    .select()
    .from(fsBooking)
    .where(sql`${fsBooking.docId} IN ('d1','d2')`)
    .all()
  assert.equal(rows.length, 2)
  for (const r of rows) {
    assert.ok(r.anomalyFlags & (1 << 5), 'orphan user flag')
    assert.ok(r.anomalyFlags & (1 << 8), 'duplicate day flag')
  }
})

test('deleting a booking removes its charges automatically', () => {
  resetBookings()
  const total = () =>
    (
      raw()
        .prepare(`SELECT COALESCE(SUM(amount_cents),0) AS t FROM v_charge_effective WHERE user_id='u1'`)
        .get() as { t: number }
    ).t

  applyAll({
    now: NOW + 9,
    windowFrom: WINDOW,
    users: [USER],
    bookings: [booking('charge1', '2026-05-20')],
    payments: [],
  })
  const withBooking = total()
  assert.ok(withBooking > 0, 'a lunch should be charged')

  db.update(fsBooking).set({ deletedAt: NOW }).where(eq(fsBooking.docId, 'charge1')).run()
  assert.equal(total(), withBooking - 800, 'the cancelled lunch stops being charged')
})

test('defaultWindowFrom starts at the previous quarter', () => {
  assert.equal(defaultWindowFrom('2026-07-26', 1), '2026-04-01')
  assert.equal(defaultWindowFrom('2026-01-05', 1), '2025-10-01')
  assert.equal(defaultWindowFrom('2026-07-26', 0), '2026-07-01')
})
