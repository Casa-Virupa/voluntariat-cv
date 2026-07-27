/**
 * The single exception to "the dashboard never writes to Firestore": pushing `amount` and
 * `paid` back onto the app's own `payments` documents, so a volunteer's phone eventually
 * agrees with the ledger.
 *
 * This is the most dangerous code in the project. `payments.amount` is already wrong in
 * production for anyone who ever cancelled a booking (`addPayment` increments and never
 * decrements, and `pay()` flips `paid` without clearing the amount), so enabling write-back
 * will visibly change numbers volunteers have already seen. Hence the rules, in priority
 * order — all six are enforced below and none is optional:
 *
 *   1. WRITE ONLY `amount` AND `paid`. `FirebasePayment` is a @Serializable Kotlin class;
 *      an unknown key risks breaking decoding on users' phones. Our metadata goes in
 *      `writeback_log`, never in the document.
 *   2. `amount` IS THAT MONTH'S DERIVED CHARGES ONLY — never a carry-over balance. The app
 *      does `amount += delta` on the next booking, so a balance would compound.
 *   3. COMPARE-AND-SET INSIDE A TRANSACTION, with the value observed during the sync that
 *      produced the target as the precondition. If the app moved in between, skip; the next
 *      sync recomputes. This is what stops two writers fighting.
 *   4. ONLY ON DRIFT. If the document already matches, perform no write at all — the steady
 *      state is zero Firestore writes.
 *   5. TARGET THE CANONICAL DOC (MIN(doc_id)) — the one `.firstOrNull()` returns on the
 *      phone. Never write to a duplicate.
 *   6. GATED on `app_setting.writeback_enabled`, which ships false.
 *
 * `reconcile()` is read-only and safe to call at any time; it is what a coordinator reviews
 * before the gate is ever opened.
 */

import { raw } from './db/index.ts'
import { db } from './db/index.ts'
import { writebackLog } from './db/schema.ts'
import { firestore } from './firebase.ts'
import { COLLECTION_PAYMENTS } from './contract.ts'
import { monthsIn, type Period } from './dates.ts'
import { writebackEnabled } from './settings.ts'

export interface ReconciliationRow {
  uid: string
  name: string
  year: number
  month: number
  /** What we derive for that month: charges only, no carry-over. */
  derivedCents: number
  /** Credits dated inside that month. */
  creditsCents: number
  /** Our verdict on whether the month is settled. */
  derivedPaid: boolean
  /** What the canonical Firestore doc says right now, or null if there is no doc. */
  appAmountCents: number | null
  appPaid: boolean | null
  appDocId: string | null
  /** How many payment docs exist for this (user, month). >1 means the app made duplicates. */
  docCount: number
  driftCents: number
  paidDiffers: boolean
}

/**
 * Compares the derived month charges against the app's own numbers, month by month. Pure
 * SQL over the mirror and the ledger — no Firestore access, no writes.
 */
export function reconcile(period: Period): ReconciliationRow[] {
  const months = monthsIn(period)
  const out: ReconciliationRow[] = []

  for (const [year, month] of months) {
    const from = `${year}-${String(month).padStart(2, '0')}-01`
    const to = month === 12 ? `${year + 1}-01-01` : `${year}-${String(month + 1).padStart(2, '0')}-01`

    const rows = raw()
      .prepare(
        `WITH charges AS (
             SELECT user_id, SUM(amount_cents) AS cents
               FROM v_charge_effective
              WHERE service_date >= ? AND service_date < ?
              GROUP BY user_id
           ),
           credits AS (
             SELECT user_id, SUM(amount_cents) AS cents
               FROM ledger_entry
              WHERE effective_date >= ? AND effective_date < ?
              GROUP BY user_id
           ),
           docs AS (
             SELECT user_id,
                    MIN(doc_id) AS doc_id,
                    COUNT(*)    AS doc_count
               FROM v_payment_canonical
              WHERE year = ? AND month = ?
              GROUP BY user_id
           )
           SELECT COALESCE(ch.user_id, cr.user_id, d.user_id)      AS uid,
                  COALESCE(NULLIF(u.name, ''), u.email, 'Voluntari desconegut') AS name,
                  COALESCE(ch.cents, 0)                            AS derivedCents,
                  COALESCE(cr.cents, 0)                            AS creditsCents,
                  d.doc_id                                         AS appDocId,
                  COALESCE(d.doc_count, 0)                         AS docCount,
                  p.amount_cents                                   AS appAmountCents,
                  p.paid                                           AS appPaid
             FROM charges ch
             FULL OUTER JOIN credits cr ON cr.user_id = ch.user_id
             FULL OUTER JOIN docs   d   ON d.user_id  = COALESCE(ch.user_id, cr.user_id)
             LEFT JOIN fs_payment p     ON p.doc_id   = d.doc_id
             LEFT JOIN fs_user u        ON u.uid      = COALESCE(ch.user_id, cr.user_id, d.user_id)
            ORDER BY name COLLATE NOCASE`,
      )
      .all(from, to, from, to, year, month) as Array<{
      uid: string
      name: string
      derivedCents: number
      creditsCents: number
      appDocId: string | null
      docCount: number
      appAmountCents: number | null
      appPaid: number | null
    }>

    for (const r of rows) {
      // "Settled" only means anything when there was something to settle.
      const derivedPaid = r.derivedCents > 0 && r.creditsCents >= r.derivedCents
      out.push({
        uid: r.uid,
        name: r.name,
        year,
        month,
        derivedCents: r.derivedCents,
        creditsCents: r.creditsCents,
        derivedPaid,
        appAmountCents: r.appAmountCents,
        appPaid: r.appPaid === null ? null : Boolean(r.appPaid),
        appDocId: r.appDocId,
        docCount: r.docCount,
        driftCents: r.derivedCents - (r.appAmountCents ?? 0),
        paidDiffers: r.appPaid !== null && Boolean(r.appPaid) !== derivedPaid,
      })
    }
  }

  return out
}

export interface WritebackPlanItem extends ReconciliationRow {
  action: 'update' | 'create' | 'skip_nochange' | 'skip_no_doc'
}

/**
 * What write-back WOULD do. Called by the config page so the plan is reviewable before
 * anything is executed, and by `applyWriteback` so the two can never disagree.
 */
export function planWriteback(period: Period, allowCreate = false): WritebackPlanItem[] {
  return reconcile(period).map((r) => {
    if (!r.appDocId) {
      // Rule 2 in spirit: a month with no charges needs no document at all.
      const action = r.derivedCents > 0 && allowCreate ? 'create' : 'skip_no_doc'
      return { ...r, action }
    }
    const matches = r.driftCents === 0 && !r.paidDiffers
    return { ...r, action: matches ? 'skip_nochange' : 'update' }
  })
}

export interface WritebackResult {
  applied: number
  created: number
  skippedNoChange: number
  skippedConflict: number
  skippedNoDoc: number
  failed: number
  messages: string[]
}

/**
 * Executes the plan. Every item is its own Firestore transaction with a compare-and-set
 * precondition, so a booking made from the app between the sync and this call causes that
 * one volunteer to be skipped rather than overwritten.
 */
export async function applyWriteback(
  period: Period,
  actor: string,
  opts: { allowCreate?: boolean; runId?: number | null } = {},
): Promise<WritebackResult> {
  if (!writebackEnabled()) {
    throw new Error(
      'L’escriptura cap a Firebase està desactivada. Activa-la a Configuració després de ' +
        'revisar l’informe de conciliació.',
    )
  }

  const result: WritebackResult = {
    applied: 0,
    created: 0,
    skippedNoChange: 0,
    skippedConflict: 0,
    skippedNoDoc: 0,
    failed: 0,
    messages: [],
  }

  const fs = firestore()
  const plan = planWriteback(period, opts.allowCreate ?? false)

  for (const item of plan) {
    if (item.action === 'skip_nochange') {
      result.skippedNoChange++
      continue
    }
    if (item.action === 'skip_no_doc') {
      result.skippedNoDoc++
      continue
    }

    const newAmountEuros = item.derivedCents / 100
    const newPaid = item.derivedPaid

    try {
      if (item.action === 'create') {
        // A brand-new document is the one case with no precondition to check, so it is only
        // reachable with allowCreate explicitly set.
        const ref = await fs.collection(COLLECTION_PAYMENTS).add({
          user_id: item.uid,
          year: item.year,
          month: item.month,
          paid: newPaid,
          amount: newAmountEuros,
        })
        logWriteback(item, ref.id, 'created', null, opts.runId ?? null, actor)
        result.created++
        continue
      }

      const ref = fs.collection(COLLECTION_PAYMENTS).doc(item.appDocId!)
      const outcome = await fs.runTransaction(async (tx) => {
        const snap = await tx.get(ref)
        if (!snap.exists) return 'conflict'

        const data = snap.data() as Record<string, unknown>
        const currentCents = Math.round(Number(data.amount ?? 0) * 100)
        const currentPaid = data.paid === true

        // The precondition: the document must still hold exactly what the sync observed.
        if (currentCents !== (item.appAmountCents ?? 0) || currentPaid !== (item.appPaid ?? false)) {
          return 'conflict'
        }
        if (currentCents === item.derivedCents && currentPaid === newPaid) return 'nochange'

        // Rule 1: only these two fields, ever.
        tx.update(ref, { amount: newAmountEuros, paid: newPaid })
        return 'applied'
      })

      if (outcome === 'applied') {
        logWriteback(item, item.appDocId, 'applied', null, opts.runId ?? null, actor)
        result.applied++
      } else if (outcome === 'conflict') {
        logWriteback(item, item.appDocId, 'skipped_conflict', null, opts.runId ?? null, actor)
        result.skippedConflict++
        result.messages.push(
          `${item.name} (${item.year}-${String(item.month).padStart(2, '0')}): l’app ha canviat el ` +
            'document des de l’última sincronització, s’ha omès.',
        )
      } else {
        logWriteback(item, item.appDocId, 'skipped_nochange', null, opts.runId ?? null, actor)
        result.skippedNoChange++
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error)
      logWriteback(item, item.appDocId, 'failed', message, opts.runId ?? null, actor)
      result.failed++
      result.messages.push(`${item.name}: ${message}`)
    }
  }

  return result
}

function logWriteback(
  item: WritebackPlanItem,
  docId: string | null,
  status: string,
  error: string | null,
  runId: number | null,
  actor: string,
): void {
  db.insert(writebackLog)
    .values({
      runId,
      paymentDocId: docId,
      userId: item.uid,
      year: item.year,
      month: item.month,
      expectedAmountCents: item.appAmountCents,
      expectedPaid: item.appPaid,
      newAmountCents: item.derivedCents,
      newPaid: item.derivedPaid,
      status,
      error: error ? `${actor}: ${error}` : null,
      createdAt: Math.floor(Date.now() / 1000),
    })
    .run()
}

export function recentWritebacks(limit = 20) {
  return raw()
    .prepare(
      `SELECT w.id, w.user_id AS uid, COALESCE(NULLIF(u.name, ''), w.user_id) AS name,
              w.year, w.month, w.expected_amount_cents AS expectedAmountCents,
              w.new_amount_cents AS newAmountCents, w.new_paid AS newPaid,
              w.status, w.error, w.created_at AS createdAt
         FROM writeback_log w
         LEFT JOIN fs_user u ON u.uid = w.user_id
        ORDER BY w.id DESC LIMIT ?`,
    )
    .all(limit) as Array<{
    id: number
    uid: string
    name: string
    year: number
    month: number
    expectedAmountCents: number | null
    newAmountCents: number
    newPaid: number
    status: string
    error: string | null
    createdAt: number
  }>
}
