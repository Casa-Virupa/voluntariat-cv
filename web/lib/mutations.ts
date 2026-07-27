/**
 * Every write to a dashboard-owned table. Plain functions, no framework imports — the
 * server actions in `app/` are thin wrappers that authorise, validate and then call one of
 * these, so the rules below cannot be bypassed by a new page forgetting them.
 *
 * Three invariants:
 *   - `ledger_entry` is INSERT-ONLY. A mistake is corrected by inserting its negation with
 *     `voids_id` set. Nothing here updates or deletes a ledger row.
 *   - Dated rules (prices, commitments) are never edited in place either: superseding one
 *     closes the old row with `valid_to` and inserts a new one, so last month's books
 *     cannot change under a coordinator's feet.
 *   - Every call writes an `audit_log` row.
 */

import { and, eq, isNull, sql } from 'drizzle-orm'

import { db } from './db/index.ts'
import {
  adminUser,
  adminUserArea,
  chargeLocked,
  commitmentRule,
  ledgerEntry,
  periodClose,
  priceRule,
} from './db/schema.ts'
import { audit } from './settings.ts'
import { AREA_CODES, AREA_GENERAL, AREA_TOTAL, AREA_UNKNOWN, CHARGEABLE_ITEMS, VOLUNTEER_TYPES } from './contract.ts'
import type { Period } from './dates.ts'

function now(): number {
  return Math.floor(Date.now() / 1000)
}

export class ValidationError extends Error {}

function fail(message: string): never {
  throw new ValidationError(message)
}

const DATE_RE = /^\d{4}-\d{2}-\d{2}$/

function requireDate(value: string, label: string): string {
  if (!DATE_RE.test(value)) fail(`${label}: la data ha de tenir el format AAAA-MM-DD.`)
  return value
}

// --- the ledger --------------------------------------------------------------

export interface LedgerInput {
  userId: string
  kind: 'payment' | 'adjustment' | 'opening_balance' | 'write_off'
  effectiveDate: string
  amountCents: number
  method?: string | null
  note?: string | null
  externalRef?: string | null
}

/** Sign convention: POSITIVE = credit, i.e. it reduces what the volunteer owes. */
export function insertLedgerEntry(input: LedgerInput, actor: string): number {
  if (!input.userId) fail('Cal indicar el voluntari.')
  requireDate(input.effectiveDate, 'Data efectiva')
  if (!Number.isInteger(input.amountCents)) fail("L'import no és vàlid.")
  if (input.amountCents === 0) fail("Un import de 0 € no registra res.")

  const id = db
    .insert(ledgerEntry)
    .values({
      userId: input.userId,
      kind: input.kind,
      effectiveDate: input.effectiveDate,
      amountCents: input.amountCents,
      method: input.method ?? null,
      note: input.note?.trim() || null,
      createdBy: actor,
      createdAt: now(),
      voidsId: null,
      externalRef: input.externalRef ?? null,
    })
    .returning({ id: ledgerEntry.id })
    .get().id

  audit(actor, 'ledger.insert', 'ledger_entry', String(id), null, input)
  return id
}

/**
 * Cancels an entry by inserting its exact negation. The original stays visible in the
 * history — which is the point: a payment that was entered twice should read as
 * "entered, cancelled", not disappear.
 */
export function voidLedgerEntry(id: number, actor: string, reason: string): number {
  const original = db.select().from(ledgerEntry).where(eq(ledgerEntry.id, id)).get()
  if (!original) fail('Aquest apunt ja no existeix.')

  const already = db
    .select({ id: ledgerEntry.id })
    .from(ledgerEntry)
    .where(eq(ledgerEntry.voidsId, id))
    .get()
  if (already) fail('Aquest apunt ja estava anul·lat.')

  const newId = db
    .insert(ledgerEntry)
    .values({
      userId: original.userId,
      kind: original.kind,
      effectiveDate: original.effectiveDate,
      amountCents: -original.amountCents,
      method: original.method,
      note: reason.trim() || `Anul·la l'apunt #${id}`,
      createdBy: actor,
      createdAt: now(),
      voidsId: id,
      externalRef: null,
    })
    .returning({ id: ledgerEntry.id })
    .get().id

  audit(actor, 'ledger.void', 'ledger_entry', String(id), original, { voidedBy: newId, reason })
  return newId
}

// --- prices ------------------------------------------------------------------

export interface PriceRuleInput {
  item: string
  volunteerType: string | null
  isMember: boolean | null
  unitPriceCents: number
  validFrom: string
  note?: string | null
}

/**
 * Adds a price. Any existing rule with the SAME specificity that is still open gets closed
 * at `validFrom` rather than overwritten, so a booking made last month keeps the price it
 * was made under.
 */
export function addPriceRule(input: PriceRuleInput, actor: string): number {
  if (!(CHARGEABLE_ITEMS as readonly string[]).includes(input.item)) fail('Concepte desconegut.')
  if (input.volunteerType !== null && !(VOLUNTEER_TYPES as readonly string[]).includes(input.volunteerType)) {
    fail('Tipus de voluntari desconegut.')
  }
  if (!Number.isInteger(input.unitPriceCents) || input.unitPriceCents < 0) {
    fail('El preu ha de ser un import positiu.')
  }
  requireDate(input.validFrom, 'Vigent des de')

  const superseded = db
    .select()
    .from(priceRule)
    .where(
      and(
        eq(priceRule.item, input.item),
        input.volunteerType === null
          ? isNull(priceRule.volunteerType)
          : eq(priceRule.volunteerType, input.volunteerType),
        input.isMember === null ? isNull(priceRule.isMember) : eq(priceRule.isMember, input.isMember),
        isNull(priceRule.validTo),
      ),
    )
    .all()

  for (const old of superseded) {
    if (old.validFrom >= input.validFrom) {
      fail(
        `Ja hi ha un preu per aquest concepte vigent des de ${old.validFrom}. ` +
          'Tria una data posterior.',
      )
    }
    db.update(priceRule).set({ validTo: input.validFrom }).where(eq(priceRule.id, old.id)).run()
  }

  const id = db
    .insert(priceRule)
    .values({
      item: input.item,
      volunteerType: input.volunteerType,
      isMember: input.isMember,
      unitPriceCents: input.unitPriceCents,
      validFrom: input.validFrom,
      validTo: null,
      note: input.note?.trim() || null,
      createdBy: actor,
      createdAt: now(),
    })
    .returning({ id: priceRule.id })
    .get().id

  audit(actor, 'price.add', 'price_rule', String(id), superseded, input)
  return id
}

/** Ends a price rule. Only ever moves `valid_to`; the row itself is history. */
export function endPriceRule(id: number, validTo: string, actor: string): void {
  requireDate(validTo, 'Vigent fins a')
  const before = db.select().from(priceRule).where(eq(priceRule.id, id)).get()
  if (!before) fail('Aquest preu ja no existeix.')
  if (validTo <= before.validFrom) fail('La data de fi ha de ser posterior a la d’inici.')

  db.update(priceRule).set({ validTo }).where(eq(priceRule.id, id)).run()
  audit(actor, 'price.end', 'price_rule', String(id), before, { validTo })
}

// --- commitments -------------------------------------------------------------

export interface CommitmentInput {
  scopeKind: 'global' | 'volunteer_type' | 'user'
  scopeValue: string | null
  area: string
  periodKind: 'month' | 'quarter'
  targetMinutes: number
  validFrom: string
  note?: string | null
}

const COMMITMENT_AREAS = new Set([...AREA_CODES, AREA_GENERAL, AREA_UNKNOWN, AREA_TOTAL])

export function addCommitmentRule(input: CommitmentInput, actor: string): number {
  if (!COMMITMENT_AREAS.has(input.area)) fail('Àrea desconeguda.')
  if (input.scopeKind === 'volunteer_type') {
    if (!input.scopeValue || !(VOLUNTEER_TYPES as readonly string[]).includes(input.scopeValue)) {
      fail('Tipus de voluntari desconegut.')
    }
  } else if (input.scopeKind === 'user') {
    if (!input.scopeValue) fail('Cal triar un voluntari.')
  } else if (input.scopeValue !== null) {
    fail('Un compromís global no pot tenir cap destinatari.')
  }
  if (!Number.isInteger(input.targetMinutes) || input.targetMinutes < 0) {
    fail('Les hores han de ser un nombre positiu.')
  }
  requireDate(input.validFrom, 'Vigent des de')

  const superseded = db
    .select()
    .from(commitmentRule)
    .where(
      and(
        eq(commitmentRule.area, input.area),
        eq(commitmentRule.scopeKind, input.scopeKind),
        input.scopeValue === null
          ? isNull(commitmentRule.scopeValue)
          : eq(commitmentRule.scopeValue, input.scopeValue),
        isNull(commitmentRule.validTo),
      ),
    )
    .all()

  for (const old of superseded) {
    if (old.validFrom >= input.validFrom) {
      fail(
        `Ja hi ha un compromís equivalent vigent des de ${old.validFrom}. ` +
          'Tria una data posterior.',
      )
    }
    db.update(commitmentRule)
      .set({ validTo: input.validFrom })
      .where(eq(commitmentRule.id, old.id))
      .run()
  }

  const id = db
    .insert(commitmentRule)
    .values({
      scopeKind: input.scopeKind,
      scopeValue: input.scopeValue,
      area: input.area,
      periodKind: input.periodKind,
      targetMinutes: input.targetMinutes,
      validFrom: input.validFrom,
      validTo: null,
      note: input.note?.trim() || null,
      createdBy: actor,
      createdAt: now(),
    })
    .returning({ id: commitmentRule.id })
    .get().id

  audit(actor, 'commitment.add', 'commitment_rule', String(id), superseded, input)
  return id
}

export function endCommitmentRule(id: number, validTo: string, actor: string): void {
  requireDate(validTo, 'Vigent fins a')
  const before = db.select().from(commitmentRule).where(eq(commitmentRule.id, id)).get()
  if (!before) fail('Aquest compromís ja no existeix.')
  if (validTo <= before.validFrom) fail('La data de fi ha de ser posterior a la d’inici.')

  db.update(commitmentRule).set({ validTo }).where(eq(commitmentRule.id, id)).run()
  audit(actor, 'commitment.end', 'commitment_rule', String(id), before, { validTo })
}

// --- the allowlist -----------------------------------------------------------

const EMAIL_RE = /^[^@\s]+@[^@\s]+\.[^@\s]+$/

export function upsertAdmin(
  input: { email: string; displayName: string | null; role: 'coordinator' | 'area_responsible'; areas: string[] },
  actor: string,
): void {
  const email = input.email.trim().toLowerCase()
  if (!EMAIL_RE.test(email)) fail('Adreça de correu no vàlida.')
  if (input.role === 'area_responsible' && input.areas.length === 0) {
    fail('Un responsable d’àrea necessita almenys una àrea.')
  }
  for (const area of input.areas) {
    if (!AREA_CODES.includes(area)) fail(`Àrea desconeguda: ${area}`)
  }

  const before = db.select().from(adminUser).where(eq(adminUser.email, email)).get()

  db.insert(adminUser)
    .values({
      email,
      displayName: input.displayName?.trim() || null,
      role: input.role,
      addedBy: actor,
      addedAt: now(),
      disabledAt: null,
    })
    .onConflictDoUpdate({
      target: adminUser.email,
      set: {
        displayName: input.displayName?.trim() || null,
        role: input.role,
        disabledAt: null,
      },
    })
    .run()

  db.delete(adminUserArea).where(eq(adminUserArea.email, email)).run()
  if (input.role === 'area_responsible') {
    for (const area of input.areas) {
      db.insert(adminUserArea).values({ email, area }).onConflictDoNothing().run()
    }
  }

  audit(actor, 'admin.upsert', 'admin_user', email, before ?? null, input)
}

/**
 * Disabled rather than deleted: `added_by` on other rows and the audit log both point at
 * these addresses, and a disabled row is what makes "who used to have access?" answerable.
 * Locks out immediately — the role is re-read from the database on every request.
 */
export function setAdminDisabled(email: string, disabled: boolean, actor: string): void {
  const target = email.trim().toLowerCase()
  const before = db.select().from(adminUser).where(eq(adminUser.email, target)).get()
  if (!before) fail('Aquesta persona no és a la llista.')
  if (disabled && target === actor) fail('No et pots treure l’accés a tu mateix.')

  if (disabled) {
    const others = db
      .select({ n: sql<number>`COUNT(*)` })
      .from(adminUser)
      .where(and(eq(adminUser.role, 'coordinator'), isNull(adminUser.disabledAt)))
      .get()
    if (before.role === 'coordinator' && (others?.n ?? 0) <= 1) {
      fail('Ha de quedar almenys una persona de coordinació amb accés.')
    }
  }

  db.update(adminUser)
    .set({ disabledAt: disabled ? now() : null })
    .where(eq(adminUser.email, target))
    .run()

  audit(actor, disabled ? 'admin.disable' : 'admin.enable', 'admin_user', target, before, {
    disabled,
  })
}

// --- closing a period --------------------------------------------------------

/**
 * Freezes a volunteer's charges for a period by snapshotting them into `charge_locked`.
 * `v_charge_effective` then prefers the snapshot, so a later price correction cannot
 * rewrite an invoice that has already gone out. A booking cancelled after the close keeps
 * its locked charge — correct: the meal was eaten.
 */
export function closePeriodForUser(userId: string, period: Period, actor: string): number {
  const existing = db
    .select()
    .from(periodClose)
    .where(
      and(
        eq(periodClose.userId, userId),
        eq(periodClose.periodKind, period.kind),
        eq(periodClose.periodYear, period.year),
        eq(periodClose.periodIndex, period.index),
      ),
    )
    .get()
  if (existing) fail('Aquest període ja està tancat.')

  const rows = db.all<{
    doc_id: string
    item: string
    service_date: string
    qty: number
    unit_price_cents: number
    amount_cents: number
  }>(
    sql`SELECT doc_id, item, service_date, qty, unit_price_cents, amount_cents
          FROM v_charge
         WHERE user_id = ${userId}
           AND service_date >= ${period.from} AND service_date < ${period.to}`,
  )

  let total = 0
  const at = now()
  for (const r of rows) {
    total += r.amount_cents
    db.insert(chargeLocked)
      .values({
        docId: r.doc_id,
        item: r.item,
        userId,
        serviceDate: r.service_date,
        qty: r.qty,
        unitPriceCents: r.unit_price_cents,
        amountCents: r.amount_cents,
        lockedAt: at,
      })
      .onConflictDoNothing()
      .run()
  }

  db.insert(periodClose)
    .values({
      userId,
      periodKind: period.kind,
      periodYear: period.year,
      periodIndex: period.index,
      closedAt: at,
      closedBy: actor,
      totalCents: total,
    })
    .run()

  audit(actor, 'period.close', 'period_close', `${userId}:${period.kind}:${period.year}:${period.index}`, null, {
    items: rows.length,
    totalCents: total,
  })
  return total
}
