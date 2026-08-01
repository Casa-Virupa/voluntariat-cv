import { test } from 'node:test'
import assert from 'node:assert/strict'

import { buildPriceRuleDocs, centsToEuros, toAppLedgerKind } from './publish.ts'
import type { PriceRuleRow } from './query/config.ts'

let nextId = 1
function rule(partial: Partial<PriceRuleRow> & Pick<PriceRuleRow, 'item' | 'unitPriceCents' | 'validFrom'>): PriceRuleRow {
  return {
    id: nextId++,
    volunteerType: null,
    isMember: null,
    validTo: null,
    note: null,
    createdBy: 'test',
    ...partial,
  }
}

test('a single open generic rule yields one doc, with app defaults for the other items', () => {
  const docs = buildPriceRuleDocs([rule({ item: 'lunch', unitPriceCents: 850, validFrom: '1970-01-01' })])
  assert.equal(docs.size, 1)
  const doc = docs.get('1970-01-01')!
  assert.equal(doc.lunch, 8.5) // cents → euros happens exactly once
  assert.equal(doc.dinner, 8)
  assert.equal(doc.breakfast, 0)
  assert.equal(doc.sleep, 10)
})

test('a superseded rule produces one doc per boundary, each resolving the price in force', () => {
  const docs = buildPriceRuleDocs([
    rule({ item: 'lunch', unitPriceCents: 800, validFrom: '1970-01-01', validTo: '2026-09-01' }),
    rule({ item: 'lunch', unitPriceCents: 900, validFrom: '2026-09-01' }),
  ])
  assert.deepEqual([...docs.keys()], ['1970-01-01', '2026-09-01'])
  assert.equal(docs.get('1970-01-01')!.lunch, 8)
  assert.equal(docs.get('2026-09-01')!.lunch, 9)
})

test('a rule ended with no successor falls back to the app default from valid_to on', () => {
  const docs = buildPriceRuleDocs([
    rule({ item: 'sleep', unitPriceCents: 1500, validFrom: '2026-01-01', validTo: '2026-06-01' }),
  ])
  assert.equal(docs.get('2026-01-01')!.sleep, 15)
  assert.equal(docs.get('2026-06-01')!.sleep, 10) // back to the default, not free and not 15
})

test('future-dated rules are published now; the app resolves them by date, no cron needed', () => {
  const docs = buildPriceRuleDocs([
    rule({ item: 'dinner', unitPriceCents: 800, validFrom: '1970-01-01', validTo: '2030-01-01' }),
    rule({ item: 'dinner', unitPriceCents: 1000, validFrom: '2030-01-01' }),
  ])
  assert.equal(docs.get('2030-01-01')!.dinner, 10)
})

test('specific rules (volunteer type / member) are dashboard-only and never published', () => {
  const docs = buildPriceRuleDocs([
    rule({ item: 'lunch', unitPriceCents: 800, validFrom: '1970-01-01' }),
    rule({ item: 'lunch', unitPriceCents: 400, validFrom: '1970-01-01', volunteerType: 'mitra' }),
    rule({ item: 'lunch', unitPriceCents: 0, validFrom: '1970-01-01', isMember: 1 }),
  ])
  assert.equal(docs.size, 1)
  assert.equal(docs.get('1970-01-01')!.lunch, 8)
})

test('boundaries that resolve to identical prices collapse into one doc', () => {
  const docs = buildPriceRuleDocs([
    rule({ item: 'lunch', unitPriceCents: 800, validFrom: '1970-01-01', validTo: '2026-06-01' }),
    rule({ item: 'lunch', unitPriceCents: 800, validFrom: '2026-06-01' }), // same price re-added
  ])
  assert.deepEqual([...docs.keys()], ['1970-01-01'])
})

test('no generic rules at all yields no docs (the app then uses its own defaults)', () => {
  assert.equal(buildPriceRuleDocs([]).size, 0)
})

test('money and kind conversions', () => {
  assert.equal(centsToEuros(2610), 26.1)
  assert.equal(centsToEuros(0), 0)
  assert.equal(toAppLedgerKind('payment'), 'payment')
  assert.equal(toAppLedgerKind('opening_balance'), 'adjustment')
  assert.equal(toAppLedgerKind('write_off'), 'adjustment')
})
