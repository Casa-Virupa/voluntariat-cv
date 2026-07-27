import assert from 'node:assert/strict'
import { test } from 'node:test'

import {
  progressOf,
  resolveCommitment,
  ruleMatchesVolunteer,
  scaleTarget,
  type CommitmentRule,
} from './commitments.ts'
import { AREA_TOTAL } from './contract.ts'
import { monthPeriod, quarterPeriod } from './dates.ts'

let nextId = 1
function rule(over: Partial<CommitmentRule> = {}): CommitmentRule {
  return {
    id: nextId++,
    scopeKind: 'global',
    scopeValue: null,
    area: AREA_TOTAL,
    periodKind: 'month',
    targetMinutes: 480,
    validFrom: '1970-01-01',
    validTo: null,
    ...over,
  }
}

const MITRA = { uid: 'u1', volunteerType: 'mitra' }
const HABITUAL = { uid: 'u2', volunteerType: 'habitual' }
const NO_TYPE = { uid: 'u3', volunteerType: null }

test('the most specific rule wins: user beats type beats global', () => {
  const rules = [
    rule({ scopeKind: 'global', targetMinutes: 100 }),
    rule({ scopeKind: 'volunteer_type', scopeValue: 'mitra', targetMinutes: 200 }),
    rule({ scopeKind: 'user', scopeValue: 'u1', targetMinutes: 300 }),
  ]
  const r = resolveCommitment(rules, MITRA, AREA_TOTAL, monthPeriod(2026, 5))
  assert.equal(r?.targetMinutes, 300)

  // Someone else only matches the type rule, and a third person only the global one.
  assert.equal(resolveCommitment(rules, HABITUAL, AREA_TOTAL, monthPeriod(2026, 5))?.targetMinutes, 100)
})

test('a volunteer_type rule never leaks onto someone with no type', () => {
  const r = rule({ scopeKind: 'volunteer_type', scopeValue: 'mitra' })
  assert.equal(ruleMatchesVolunteer(r, NO_TYPE), false)
  assert.equal(resolveCommitment([r], NO_TYPE, AREA_TOTAL, monthPeriod(2026, 5)), null)
})

test('validity is judged at the period start, so a July edit cannot rewrite May', () => {
  const rules = [
    rule({ targetMinutes: 480, validFrom: '1970-01-01', validTo: '2026-07-01' }),
    rule({ targetMinutes: 600, validFrom: '2026-07-01' }),
  ]
  assert.equal(resolveCommitment(rules, HABITUAL, AREA_TOTAL, monthPeriod(2026, 5))?.targetMinutes, 480)
  assert.equal(resolveCommitment(rules, HABITUAL, AREA_TOTAL, monthPeriod(2026, 7))?.targetMinutes, 600)
})

test('a rule that starts mid-period does not apply to that period', () => {
  // Valid from 15 May: the May period starts on the 1st, so May is still the old rule's.
  const rules = [rule({ targetMinutes: 999, validFrom: '2026-05-15' })]
  assert.equal(resolveCommitment(rules, HABITUAL, AREA_TOTAL, monthPeriod(2026, 5)), null)
  assert.equal(resolveCommitment(rules, HABITUAL, AREA_TOTAL, monthPeriod(2026, 6))?.targetMinutes, 999)
})

test("Mitra's 60h/quarter shows as 20h when viewed a month at a time", () => {
  const rules = [
    rule({ scopeKind: 'volunteer_type', scopeValue: 'mitra', periodKind: 'quarter', targetMinutes: 3600 }),
  ]

  const perQuarter = resolveCommitment(rules, MITRA, AREA_TOTAL, quarterPeriod(2026, 2))
  assert.equal(perQuarter?.targetMinutes, 3600)
  assert.equal(perQuarter?.scaled, false)

  const perMonth = resolveCommitment(rules, MITRA, AREA_TOTAL, monthPeriod(2026, 5))
  assert.equal(perMonth?.targetMinutes, 1200)
  assert.equal(perMonth?.scaled, true)
  assert.equal(perMonth?.nativeTargetMinutes, 3600)
})

test('a monthly rule viewed per quarter is multiplied, not left alone', () => {
  assert.deepEqual(scaleTarget(480, 'month', 'quarter'), { minutes: 1440, scaled: true })
  assert.deepEqual(scaleTarget(3600, 'quarter', 'month'), { minutes: 1200, scaled: true })
  assert.deepEqual(scaleTarget(480, 'month', 'month'), { minutes: 480, scaled: false })
})

test('status bands: complete, at risk, not complete', () => {
  const c = resolveCommitment([rule({ targetMinutes: 480 })], HABITUAL, AREA_TOTAL, monthPeriod(2026, 5))
  assert.equal(progressOf(480, c).status, 'complete')
  assert.equal(progressOf(600, c).status, 'complete')
  assert.equal(progressOf(400, c).status, 'at_risk') // 83 %
  assert.equal(progressOf(300, c).status, 'incomplete') // 62 %
  assert.equal(progressOf(0, c).status, 'incomplete')
})

test('the at-risk band is configurable', () => {
  const c = resolveCommitment([rule({ targetMinutes: 480 })], HABITUAL, AREA_TOTAL, monthPeriod(2026, 5))
  assert.equal(progressOf(300, c, 0.5).status, 'at_risk')
})

test('a scaled target never reports "no complert" — its deadline has not arrived', () => {
  const rules = [
    rule({ scopeKind: 'volunteer_type', scopeValue: 'mitra', periodKind: 'quarter', targetMinutes: 3600 }),
  ]
  const perMonth = resolveCommitment(rules, MITRA, AREA_TOTAL, monthPeriod(2026, 4))

  // 0 of a prorated 20h in the first month of the quarter is "en curs", not a failure.
  assert.equal(progressOf(0, perMonth).status, 'in_progress')
  assert.equal(progressOf(1100, perMonth).status, 'at_risk')
  assert.equal(progressOf(1200, perMonth).status, 'complete')

  // Judged over the whole quarter, the same shortfall IS a failure.
  const perQuarter = resolveCommitment(rules, MITRA, AREA_TOTAL, quarterPeriod(2026, 2))
  assert.equal(progressOf(0, perQuarter).status, 'incomplete')
})

test('no rule means "sense compromís", not 0 %', () => {
  const p = progressOf(240, null)
  assert.equal(p.status, 'none')
  assert.equal(p.ratio, null)
})

test('a zero-minute target is treated as no commitment, not as a division by zero', () => {
  const c = resolveCommitment([rule({ targetMinutes: 0 })], HABITUAL, AREA_TOTAL, monthPeriod(2026, 5))
  const p = progressOf(0, c)
  assert.equal(p.ratio, null)
  assert.equal(p.status, 'none')
})

test('area rules are independent of the total rule', () => {
  const rules = [
    rule({ area: AREA_TOTAL, targetMinutes: 480 }),
    rule({ area: 'kitchen', targetMinutes: 240 }),
  ]
  assert.equal(resolveCommitment(rules, HABITUAL, 'kitchen', monthPeriod(2026, 5))?.targetMinutes, 240)
  assert.equal(resolveCommitment(rules, HABITUAL, 'gardening', monthPeriod(2026, 5)), null)
})

test('among equally specific rules the latest start wins', () => {
  const rules = [
    rule({ targetMinutes: 100, validFrom: '2026-01-01' }),
    rule({ targetMinutes: 200, validFrom: '2026-03-01' }),
  ]
  assert.equal(resolveCommitment(rules, HABITUAL, AREA_TOTAL, monthPeriod(2026, 5))?.targetMinutes, 200)
})
