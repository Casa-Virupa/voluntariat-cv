/**
 * Destructive cleanup. Two very different things live behind two different flags, because
 * confusing them would be expensive:
 *
 *   --mirror   wipes fs_* only. Disposable by design: a sync rebuilds it from Firestore.
 *              This is what clears demo data, and it is safe on a live install.
 *
 *   --ledger   wipes the dashboard-owned MONEY: ledger entries, period closes, frozen
 *              charges, write-back history. This data exists NOWHERE ELSE — not in
 *              Firestore, not in the app. There is no way to rebuild it.
 *
 *   --rules    resets prices and commitments to the app's defaults (8 €/àpat, 10 €/pernocta,
 *              60 h per quarter for mitres, 8 h per month for habituals).
 *
 *   --all      all three. Never touches `admin_user`: removing your own access would leave
 *              nobody able to sign in, and `scripts/add-admin.ts` would be the only way back.
 *
 * Usage:
 *   node scripts/reset.ts --mirror --yes
 *   node scripts/reset.ts --all --yes
 *
 * Nothing happens without --yes. Row counts are printed before and after so the log says
 * what was destroyed.
 */

import { raw, databaseFile, isMigrated } from '../lib/db/index.ts'
import { runMigrations } from '../lib/db/migrate.ts'

const args = new Set(process.argv.slice(2))
const yes = args.has('--yes')
const all = args.has('--all')
const doMirror = all || args.has('--mirror')
const doLedger = all || args.has('--ledger')
const doRules = all || args.has('--rules')

if (!doMirror && !doLedger && !doRules) {
  console.error(
    [
      'Cal indicar què vols esborrar:',
      '  --mirror   les taules fs_* (es reconstrueixen amb una sincronització)',
      '  --ledger   pagaments, ajustos, tancaments i historial d’escriptures  ← IRRECUPERABLE',
      '  --rules    preus i compromisos, tornen als valors per defecte de l’app',
      '  --all      les tres coses',
      '',
      'Afegeix --yes per executar-ho de debò. Mai s’esborra admin_user.',
    ].join('\n'),
  )
  process.exit(1)
}

if (!isMigrated()) {
  console.error('La base de dades no està migrada. Executa `npm run db:migrate` primer.')
  process.exit(1)
}

const db = raw()

function count(table: string): number {
  return (db.prepare(`SELECT COUNT(*) AS n FROM ${table}`).get() as { n: number }).n
}

const TABLES = {
  mirror: ['fs_booking_shift', 'fs_booking', 'fs_payment', 'fs_user_area', 'fs_user'],
  ledger: ['ledger_entry', 'charge_locked', 'period_close', 'writeback_log'],
  rules: ['price_rule', 'commitment_rule'],
}

const planned: string[] = [
  ...(doMirror ? TABLES.mirror : []),
  ...(doLedger ? TABLES.ledger : []),
  ...(doRules ? TABLES.rules : []),
]

console.log(`Base de dades: ${databaseFile()}`)
for (const table of planned) console.log(`  ${table}: ${count(table)} files`)

if (!yes) {
  console.log('\nNo s’ha esborrat res. Torna-ho a executar amb --yes.')
  process.exit(0)
}

// One transaction: a half-cleared ledger would be worse than either state.
db.transaction(() => {
  for (const table of planned) db.prepare(`DELETE FROM ${table}`).run()
})()

if (doRules) {
  // The seeds are idempotent and only insert when the tables are empty, which they now are.
  runMigrations()
  console.log('\nPreus i compromisos tornats als valors per defecte de l’app.')
}

console.log('\nDesprés:')
for (const table of planned) console.log(`  ${table}: ${count(table)} files`)

db.prepare('VACUUM').run()

if (doMirror) {
  console.log('\nEl mirall és buit. Executa una sincronització completa per reconstruir-lo:')
  console.log("  curl -X POST -H \"X-Sync-Key: $SYNC_KEY\" 'http://127.0.0.1:3003/api/sync?full=1'")
}
if (doLedger) {
  console.log(
    '\nEl llibre major és buit. Abans d’ensenyar /coordinacio a algú, posa els saldos a zero:',
  )
  console.log('  node scripts/opening-balance.ts --as-of AAAA-MM-DD --yes')
}
