/**
 * Publishing the dashboard's money facts to Firestore, where the app reads them live.
 *
 * Since 2026-08 the app derives every balance from two collections the dashboard owns:
 *
 *   price_rules/<valid_from>          — one doc per change of the GENERIC price set
 *   ├── valid_from  string YYYY-MM-DD — the app resolves "rule in force on date D" as the
 *   │                                   doc with the greatest valid_from <= D
 *   └── lunch/dinner/breakfast/sleep  number, EUROS (the app never sees cents)
 *
 *   ledger/dash-<ledger_entry.id>     — one doc per SQLite ledger row the dashboard wrote
 *   ├── user_id  string               — Firebase uid
 *   ├── date     string YYYY-MM-DD    — effective_date
 *   ├── amount   number, EUROS        — POSITIVE = money received (same sign as SQLite)
 *   ├── kind     "payment"|"adjustment" — the only kinds the app models; anything else
 *   │                                     maps to "adjustment" with the original preserved
 *   ├── dashboard_kind, recorded_by   — extra fields, ignored by the app
 *   └── note     string | absent
 *
 * Deterministic doc IDs make every publish idempotent: re-running overwrites the same
 * docs. Ledger rows imported FROM Firestore by the sync (external_ref "fs:<docId>") are
 * never published back — they already live there.
 *
 * Like lib/links.ts, nothing here authorises: the server actions do.
 */

import { eq } from 'drizzle-orm'

import { db } from './db/index.ts'
import { ledgerEntry } from './db/schema.ts'
import { firestore } from './firebase.ts'
import { audit } from './settings.ts'
import { priceRules, type PriceRuleRow } from './query/config.ts'
import {
  APP_DEFAULT_PRICES_CENTS,
  CHARGEABLE_ITEMS,
  COLLECTION_LEDGER,
  COLLECTION_PRICE_RULES,
  type ChargeableItem,
} from './contract.ts'

export const FIRESTORE_IMPORT_REF_PREFIX = 'fs:'
export const LEDGER_DOC_PREFIX = 'dash-'

export function centsToEuros(cents: number): number {
  return Math.round(cents) / 100
}

// --- price rules ---------------------------------------------------------------

export interface PriceRulesDoc {
  valid_from: string
  lunch: number
  dinner: number
  breakfast: number
  sleep: number
}

/**
 * Materialises the GENERIC price timeline (volunteer_type IS NULL AND is_member IS NULL —
 * the app knows one flat price per item) into the docs the app expects. Pure function.
 *
 * Boundaries are every generic valid_from and valid_to; at each one the price per item is
 * resolved the same way v_charge does for generic rows (greatest valid_from wins), and an
 * item with no rule in force falls back to the app's own defaults — including after a rule
 * is ended with no successor, which the app's "greatest valid_from <= D" resolution could
 * not express otherwise. Consecutive identical docs are collapsed.
 */
export function buildPriceRuleDocs(rules: PriceRuleRow[]): Map<string, PriceRulesDoc> {
  const generic = rules.filter((r) => r.volunteerType === null && r.isMember === null)

  const boundaries = [
    ...new Set(generic.flatMap((r) => (r.validTo === null ? [r.validFrom] : [r.validFrom, r.validTo]))),
  ].sort()

  const priceAt = (item: ChargeableItem, date: string): number => {
    const inForce = generic
      .filter((r) => r.item === item && r.validFrom <= date && (r.validTo === null || r.validTo > date))
      .sort((a, b) => (a.validFrom === b.validFrom ? a.id - b.id : a.validFrom.localeCompare(b.validFrom)))
    const winner = inForce[inForce.length - 1]
    return centsToEuros(winner ? winner.unitPriceCents : APP_DEFAULT_PRICES_CENTS[item])
  }

  const docs = new Map<string, PriceRulesDoc>()
  let previous: PriceRulesDoc | null = null
  for (const date of boundaries) {
    const doc: PriceRulesDoc = {
      valid_from: date,
      lunch: priceAt('lunch', date),
      dinner: priceAt('dinner', date),
      breakfast: priceAt('breakfast', date),
      sleep: priceAt('sleep', date),
    }
    const samePrices =
      previous !== null &&
      CHARGEABLE_ITEMS.every((item) => doc[item] === (previous as PriceRulesDoc)[item])
    if (!samePrices) {
      docs.set(date, doc)
      previous = doc
    }
  }
  return docs
}

/**
 * Upserts the computed timeline into Firestore (doc ID = valid_from) and removes docs no
 * longer in it, so the collection always equals the dashboard's generic history.
 */
export async function publishPriceRules(actor: string): Promise<{ written: number; deleted: number }> {
  const docs = buildPriceRuleDocs(priceRules())
  const collection = firestore().collection(COLLECTION_PRICE_RULES)

  const existing = await collection.get()
  let written = 0
  let deleted = 0

  for (const [id, doc] of docs) {
    const current = existing.docs.find((d) => d.id === id)
    if (current && sameDoc(current.data(), doc)) continue
    await collection.doc(id).set(doc)
    written++
  }
  for (const d of existing.docs) {
    if (!docs.has(d.id)) {
      await collection.doc(d.id).delete()
      deleted++
    }
  }

  if (written > 0 || deleted > 0) {
    audit(actor, 'prices.publish', 'firestore_prices', COLLECTION_PRICE_RULES, null, {
      rules: docs.size,
      written,
      deleted,
    })
  }
  return { written, deleted }
}

function sameDoc(current: Record<string, unknown>, doc: PriceRulesDoc): boolean {
  return (
    current.valid_from === doc.valid_from &&
    CHARGEABLE_ITEMS.every((item) => current[item] === doc[item])
  )
}

export interface PriceRulesPublishState {
  /** Docs the dashboard's history materialises to. */
  expected: number
  /** Docs that differ, are missing, or should not be there. */
  drift: number
  /** True when specificity rules exist that the app cannot see. */
  hasSpecificRules: boolean
}

/** Read-only comparison for the /configuracio status card. Throws on credential errors. */
export async function priceRulesPublishState(): Promise<PriceRulesPublishState> {
  const rules = priceRules()
  const docs = buildPriceRuleDocs(rules)
  const existing = await firestore().collection(COLLECTION_PRICE_RULES).get()

  let drift = 0
  for (const [id, doc] of docs) {
    const current = existing.docs.find((d) => d.id === id)
    if (!current || !sameDoc(current.data(), doc)) drift++
  }
  drift += existing.docs.filter((d) => !docs.has(d.id)).length

  return {
    expected: docs.size,
    drift,
    hasSpecificRules: rules.some((r) => r.volunteerType !== null || r.isMember !== null),
  }
}

// --- the ledger ----------------------------------------------------------------

export function toAppLedgerKind(kind: string): 'payment' | 'adjustment' {
  return kind === 'payment' ? 'payment' : 'adjustment'
}

/**
 * Publishes one SQLite ledger row to `ledger/dash-<id>`. Rows the sync imported from
 * Firestore are skipped — publishing them back would double the money.
 */
export async function publishLedgerEntry(id: number, actor: string): Promise<boolean> {
  const row = db
    .select()
    .from(ledgerEntry)
    .where(eq(ledgerEntry.id, id))
    .get()
  if (!row) return false
  if (row.externalRef?.startsWith(FIRESTORE_IMPORT_REF_PREFIX)) return false

  const doc: Record<string, unknown> = {
    user_id: row.userId,
    date: row.effectiveDate,
    amount: centsToEuros(row.amountCents),
    kind: toAppLedgerKind(row.kind),
    dashboard_kind: row.kind,
    recorded_by: row.createdBy,
  }
  if (row.note) doc.note = row.note

  const docId = `${LEDGER_DOC_PREFIX}${row.id}`
  await firestore().collection(COLLECTION_LEDGER).doc(docId).set(doc)
  audit(actor, 'ledger.publish', 'firestore_ledger', docId, null, doc)
  return true
}

/**
 * SQLite ledger rows that should exist in Firestore but do not — the retry queue after a
 * publish failed mid-action. `listDocuments()` reads IDs only, no document data.
 */
export async function pendingLedgerPublishes(): Promise<number[]> {
  const rows = db
    .select({ id: ledgerEntry.id, externalRef: ledgerEntry.externalRef })
    .from(ledgerEntry)
    .all()
  const publishable = rows.filter((r) => !r.externalRef?.startsWith(FIRESTORE_IMPORT_REF_PREFIX))

  const refs = await firestore().collection(COLLECTION_LEDGER).listDocuments()
  const published = new Set(
    refs
      .map((r) => r.id)
      .filter((docId) => docId.startsWith(LEDGER_DOC_PREFIX))
      .map((docId) => Number(docId.slice(LEDGER_DOC_PREFIX.length))),
  )

  return publishable.map((r) => r.id).filter((id) => !published.has(id))
}
