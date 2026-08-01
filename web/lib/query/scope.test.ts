/**
 * "Els meus voluntaris" against a real SQLite file. The definition has two halves and each
 * one is a way for the filter to go wrong: forget the fs_user_area half and a responsible
 * loses the people who actually book, forget the commitment half and the filter is useless
 * until somebody books something.
 */

import { test, before, after } from 'node:test'
import assert from 'node:assert/strict'
import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const dir = mkdtempSync(join(tmpdir(), 'cv-scope-'))
process.env.DATABASE_PATH = join(dir, 'test.db')

// Imported dynamically so DATABASE_PATH is set before the db singleton opens.
const { raw } = await import('../db/index.ts')
const { runMigrations } = await import('../db/migrate.ts')
const { myVolunteerUids } = await import('./scope.ts')

const DATE = '2026-07-01'

before(() => {
  runMigrations()
  const db = raw()

  // The seeds ship two __total__ rules (mitra 60h/quarter, habitual 8h/month). They must
  // never make anybody "mine": a whole-organisation target names no area.
  db.prepare(`DELETE FROM commitment_rule WHERE created_by != 'system:seed'`).run()

  const user = db.prepare(
    `INSERT INTO fs_user (uid, name, email, role, volunteer_type, onboarding_completed,
                          is_member, raw_json, doc_hash, first_seen_at, last_seen_at)
     VALUES (?, ?, '', 'volunteer', ?, 1, 0, '{}', '', 0, 0)`,
  )
  user.run('listed', 'Apuntada a Cuina', 'habitual')
  user.run('committed', 'Mitra sense àrea', 'mitra')
  user.run('personal', 'Compromís personal', null)
  user.run('stranger', 'Ningú meu', 'habitual')

  const area = db.prepare(`INSERT INTO fs_user_area (uid, area) VALUES (?, ?)`)
  area.run('listed', 'kitchen')
  area.run('stranger', 'garden')

  const rule = db.prepare(
    `INSERT INTO commitment_rule (scope_kind, scope_value, area, period_kind, target_minutes,
                                  valid_from, valid_to, created_by, created_at)
     VALUES (?, ?, ?, 'month', 600, ?, ?, 'test', 0)`,
  )
  // Aimed at a type, in Cuina: every mitra becomes Cuina's business.
  rule.run('volunteer_type', 'mitra', 'kitchen', '2026-01-01', null)
  // Aimed at one person, in Cuina.
  rule.run('user', 'personal', 'kitchen', '2026-01-01', null)
  // Aimed at Cuina but already closed before the period starts.
  rule.run('user', 'stranger', 'kitchen', '2026-01-01', '2026-06-01')
  // Aimed at everyone, but in someone else's area.
  rule.run('global', null, 'garden', '2026-01-01', null)
})

after(() => rmSync(dir, { recursive: true, force: true }))

test('no areas means nobody, not everybody', () => {
  assert.deepEqual(myVolunteerUids([], DATE), [])
})

test('listed in the area, or carrying a commitment aimed at it', () => {
  const mine = myVolunteerUids(['kitchen'], DATE).sort()
  assert.deepEqual(mine, ['committed', 'listed', 'personal'])
})

test('a commitment that ended before the period starts does not count', () => {
  assert.ok(!myVolunteerUids(['kitchen'], DATE).includes('stranger'))
  // ...but it did while it was open.
  assert.ok(myVolunteerUids(['kitchen'], '2026-05-01').includes('stranger'))
})

test('a global rule in an area makes every volunteer that area’s business', () => {
  const mine = myVolunteerUids(['garden'], DATE).sort()
  assert.deepEqual(mine, ['committed', 'listed', 'personal', 'stranger'])
})

test('several areas union rather than intersect', () => {
  assert.equal(myVolunteerUids(['kitchen', 'garden'], DATE).length, 4)
})

test('an unrelated area matches nobody', () => {
  assert.deepEqual(myVolunteerUids(['grants'], DATE), [])
})
