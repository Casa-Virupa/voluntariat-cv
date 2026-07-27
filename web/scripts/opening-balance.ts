/**
 * Zeroes every volunteer's balance as of a date, by recording what they owed at that moment
 * as an explicit `opening_balance` ledger entry.
 *
 *   node scripts/opening-balance.ts --as-of 2026-08-01            # dry run, prints the list
 *   node scripts/opening-balance.ts --as-of 2026-08-01 --yes      # writes it
 *
 * WHY THIS EXISTS. Charges are derived from the mirror: on day one the dashboard computes
 * every meal and overnight stay in the whole mirrored history, while the ledger — which is
 * dashboard-owned and brand new — is empty. So every volunteer appears to owe months of
 * meals they in fact paid for in cash long ago, and "Saldo anterior" is a large number that
 * is arithmetically right and factually nonsense.
 *
 * The fix is not to hide those charges but to record, once, that they were already settled.
 * The entry is dated the day BEFORE the cut-off so it lands in the carry-over rather than in
 * the first live period, and it is signed so a volunteer who was genuinely in credit stays
 * in credit. `external_ref` is unique, so running this twice is a no-op rather than a
 * double correction.
 *
 * This is deliberately not automatic. Someone has to choose the date the books start.
 */

import { raw, databaseFile, isMigrated } from '../lib/db/index.ts'
import { insertLedgerEntry, ValidationError } from '../lib/mutations.ts'
import { formatCents } from '../lib/dates.ts'

const argv = process.argv.slice(2)
const flag = (name: string): string | null => {
  const i = argv.indexOf(name)
  return i >= 0 && argv[i + 1] && !argv[i + 1].startsWith('--') ? argv[i + 1] : null
}

const asOf = flag('--as-of')
const yes = argv.includes('--yes')
const actor = flag('--by') ?? 'system:opening-balance'

if (!asOf || !/^\d{4}-\d{2}-\d{2}$/.test(asOf)) {
  console.error(
    [
      'Ús: node scripts/opening-balance.ts --as-of AAAA-MM-DD [--yes] [--by correu]',
      '',
      'Posa a zero el saldo de tots els voluntaris just abans d’aquesta data, registrant',
      'el que devien com un apunt de «saldo inicial». Sense --yes només ho mostra.',
    ].join('\n'),
  )
  process.exit(1)
}

if (!isMigrated()) {
  console.error('La base de dades no està migrada. Executa `npm run db:migrate` primer.')
  process.exit(1)
}

/** The day before the cut-off, so the entry lands in the carry-over, not in the first period. */
function dayBefore(date: string): string {
  const [y, m, d] = date.split('-').map(Number)
  const t = new Date(Date.UTC(y, m - 1, d) - 86400000)
  return t.toISOString().slice(0, 10)
}

const effectiveDate = dayBefore(asOf)

/**
 * Outstanding = derived charges before the cut-off, minus whatever the ledger already
 * credits before it. Both halves are needed: this may be run after some payments have
 * already been recorded by hand.
 */
const rows = raw()
  .prepare(
    `WITH charges AS (
         SELECT user_id, SUM(amount_cents) AS cents
           FROM v_charge_effective WHERE service_date < ? GROUP BY user_id
       ),
       credits AS (
         SELECT user_id, SUM(amount_cents) AS cents
           FROM ledger_entry WHERE effective_date < ? GROUP BY user_id
       )
       SELECT COALESCE(ch.user_id, cr.user_id)                          AS uid,
              COALESCE(NULLIF(u.name, ''), u.email, ch.user_id, cr.user_id) AS name,
              COALESCE(ch.cents, 0) - COALESCE(cr.cents, 0)             AS outstanding
         FROM charges ch
         FULL OUTER JOIN credits cr ON cr.user_id = ch.user_id
         LEFT JOIN fs_user u ON u.uid = COALESCE(ch.user_id, cr.user_id)
        ORDER BY name COLLATE NOCASE`,
  )
  .all(asOf, asOf) as Array<{ uid: string; name: string; outstanding: number }>

const actionable = rows.filter((r) => r.outstanding !== 0)

console.log(`Base de dades: ${databaseFile()}`)
console.log(`Saldos a zero abans de ${asOf} (apunt datat ${effectiveDate}):\n`)

if (actionable.length === 0) {
  console.log('  Cap voluntari té saldo pendent abans d’aquesta data. Res a fer.')
  process.exit(0)
}

let total = 0
for (const r of actionable) {
  total += r.outstanding
  const direction = r.outstanding > 0 ? 'devia' : 'tenia a favor'
  console.log(
    `  ${r.name.padEnd(28)} ${direction} ${formatCents(Math.abs(r.outstanding)).padStart(12)}`,
  )
}
console.log(`\n  ${actionable.length} voluntaris · net ${formatCents(total)}`)

if (!yes) {
  console.log('\nNo s’ha escrit res. Torna-ho a executar amb --yes.')
  process.exit(0)
}

let written = 0
let skipped = 0
for (const r of actionable) {
  try {
    insertLedgerEntry(
      {
        userId: r.uid,
        kind: 'opening_balance',
        effectiveDate,
        // Signed so it cancels the outstanding amount in either direction.
        amountCents: r.outstanding,
        method: null,
        note: `Saldo inicial: el que hi havia pendent abans de ${asOf}, ja liquidat fora del panell.`,
        externalRef: `opening_balance:${r.uid}:${asOf}`,
      },
      actor,
    )
    written++
  } catch (error) {
    // The unique index on external_ref makes a second run a no-op rather than a double
    // correction, which is the whole point of having it.
    skipped++
    if (!(error instanceof ValidationError)) {
      console.log(`  ja existia o no s’ha pogut escriure: ${r.name}`)
    }
  }
}

console.log(`\n${written} apunts escrits, ${skipped} omesos (ja existien).`)
console.log('Comprova-ho a /coordinacio: «Saldo anterior» hauria de ser 0 € per a tothom.')
