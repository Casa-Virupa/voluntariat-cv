import assert from 'node:assert/strict'
import { test } from 'node:test'

import { balanceOf, parseEurosToCents, settlementOf } from './ledger.ts'

test('owed is charges minus credits, with carry-over kept separate', () => {
  const b = balanceOf({ chargesCents: 2400, creditsCents: 800, paymentsCents: 800, carryInCents: 500 })
  assert.equal(b.owedPeriodCents, 1600)
  assert.equal(b.owedTotalCents, 2100)
})

test('a credit larger than the charges puts the volunteer in credit, not at zero', () => {
  const b = balanceOf({ chargesCents: 800, creditsCents: 2000, paymentsCents: 2000, carryInCents: 0 })
  assert.equal(b.owedTotalCents, -1200)
  assert.equal(settlementOf(b), 'credit')
})

test('a paid-off period reads as settled', () => {
  const b = balanceOf({ chargesCents: 2400, creditsCents: 2400, paymentsCents: 2400, carryInCents: 0 })
  assert.equal(settlementOf(b), 'settled')
})

test('carry-over alone is enough to owe money', () => {
  const b = balanceOf({ chargesCents: 0, creditsCents: 0, paymentsCents: 0, carryInCents: 1600 })
  assert.equal(settlementOf(b), 'owing')
})

test('a volunteer with no activity is neither paid nor pending', () => {
  const b = balanceOf({ chargesCents: 0, creditsCents: 0, paymentsCents: 0, carryInCents: 0 })
  assert.equal(settlementOf(b), 'nothing')
})

test('a one-cent residue from a rounded proration is not a debt', () => {
  const b = balanceOf({ chargesCents: 801, creditsCents: 800, paymentsCents: 800, carryInCents: 0 })
  assert.equal(settlementOf(b), 'settled')
})

test('euros typed by a human parse to exact cents, comma or dot', () => {
  assert.equal(parseEurosToCents('8'), 800)
  assert.equal(parseEurosToCents('8,50'), 850)
  assert.equal(parseEurosToCents('8.50'), 850)
  assert.equal(parseEurosToCents(' 12,05 € '), 1205)
  assert.equal(parseEurosToCents('-3,20'), -320)
  // 0.1 + 0.2 territory: the classic float trap must not reach the database.
  assert.equal(parseEurosToCents('0,30'), 30)
})

test('an unparseable amount is rejected rather than becoming zero', () => {
  assert.equal(parseEurosToCents(''), null)
  assert.equal(parseEurosToCents('vuit'), null)
  assert.equal(parseEurosToCents('8,555'), null)
  assert.equal(parseEurosToCents('8,5,5'), null)
})
