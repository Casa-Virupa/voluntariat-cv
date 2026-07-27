/**
 * Fills the mirror with plausible fake data so the views can be verified before Firestore
 * credentials exist.
 *
 *   node scripts/seed-demo.ts            # refuses if the mirror already has bookings
 *   node scripts/seed-demo.ts --force    # wipes fs_* first
 *
 * It builds raw Firestore-shaped documents and pushes them through the REAL parser and the
 * REAL apply step (`parseUser`/`parseBooking`/`parsePayment` + `applyAll`), so anything the
 * sync would flag as an anomaly is flagged here too, and the numbers on screen are produced
 * by exactly the code path production uses. It never touches a dashboard-owned table, so a
 * seeded database still exercises the real prices, commitments and ledger.
 *
 * Deliberately NOT importable from the app: this is the one place outside lib/sync that
 * writes fs_*, and it lives in scripts/ so that stays visible.
 */

import { raw } from '../lib/db/index.ts'
import { isMigrated } from '../lib/db/index.ts'
import { applyAll } from '../lib/sync/run.ts'
import { parseBooking, parsePayment, parseUser } from '../lib/sync/parse.ts'
import { epochSecondsAtLocalMidnight, localDate, quarterOf, firstMonthOfQuarter, firstOfMonth, addMonths } from '../lib/dates.ts'
import { AREA_CODES } from '../lib/contract.ts'

if (process.env.NODE_ENV === 'production') {
  console.error('Refuso omplir amb dades falses amb NODE_ENV=production.')
  process.exit(1)
}
if (!isMigrated()) {
  console.error('La base de dades no està migrada. Executa `npm run db:migrate` primer.')
  process.exit(1)
}

const force = process.argv.includes('--force')
const existing = (raw().prepare('SELECT COUNT(*) AS n FROM fs_booking').get() as { n: number }).n
if (existing > 0 && !force) {
  console.error(
    `El mirall ja té ${existing} reserves. Torna-ho a executar amb --force per esborrar-les.`,
  )
  process.exit(1)
}
if (force) {
  raw().exec('DELETE FROM fs_booking_shift; DELETE FROM fs_booking; DELETE FROM fs_payment; DELETE FROM fs_user_area; DELETE FROM fs_user;')
}

/** Seeded LCG: the same command always produces the same database. */
let state = 20260727
function rnd(): number {
  state = (state * 1103515245 + 12345) % 2147483648
  return state / 2147483648
}
function pick<T>(items: T[]): T {
  return items[Math.floor(rnd() * items.length)]
}

const AREAS = ['kitchen', 'gardening', 'temple', 'maintenance', 'shop', 'communication', 'grove']
for (const a of AREAS) {
  if (!AREA_CODES.includes(a)) throw new Error(`Àrea inventada: ${a}`)
}

const PEOPLE = [
  { name: 'Alba Ferrer', type: 'mitra', member: true, areas: ['kitchen', 'temple'] },
  { name: 'Bernat Puig', type: 'habitual', member: false, areas: ['gardening'] },
  { name: 'Carla Roig', type: 'habitual', member: true, areas: ['kitchen'] },
  { name: 'Dídac Solé', type: 'mitra', member: false, areas: ['maintenance', 'grove'] },
  { name: 'Elena Mas', type: 'habitual', member: false, areas: ['shop', 'communication'] },
  { name: 'Ferran Vidal', type: 'mitra', member: true, areas: ['temple'] },
  { name: 'Gemma Costa', type: 'habitual', member: false, areas: ['gardening', 'grove'] },
  { name: 'Hug Serra', type: 'habitual', member: false, areas: ['kitchen', 'shop'] },
  { name: 'Iris Camps', type: null, member: true, areas: ['volunteer_coordination'] },
]

const userDocs = PEOPLE.map((p, i) => {
  const uid = `demo-user-${String(i + 1).padStart(2, '0')}`
  return {
    id: uid,
    data: {
      id: uid,
      name: p.name,
      email: `${p.name.split(' ')[0].toLowerCase()}@example.cat`,
      role: p.type === null ? 'coordination_team' : 'volunteer',
      volunteer_type: p.type,
      onboarding_completed: true,
      specific_areas: p.areas,
      is_member: p.member,
    },
  }
})

const today = localDate(Math.floor(Date.now() / 1000))
const [ty, tm] = today.split('-').map(Number)
// Three months back from the start of the current quarter, through next month: the same
// span a real sync reads, so the quarterly selector has something to show either side.
const start = addMonths(firstOfMonth(ty, firstMonthOfQuarter(quarterOf(tm))), -3)
const end = addMonths(firstOfMonth(ty, tm), 2)

function eachDay(from: string, to: string): string[] {
  const out: string[] = []
  const d = new Date(`${from}T00:00:00Z`)
  const last = new Date(`${to}T00:00:00Z`)
  while (d < last) {
    out.push(d.toISOString().slice(0, 10))
    d.setUTCDate(d.getUTCDate() + 1)
  }
  return out
}

const MORNING = { start: '09:00', end: '13:00' }
const AFTERNOON = { start: '15:00', end: '18:30' }
const SHORT_MORNING = { start: '10:00', end: '12:00' }

const bookingDocs: Array<{ id: string; data: Record<string, unknown> }> = []
let seq = 0

for (const date of eachDay(start, end)) {
  const weekday = new Date(`${date}T00:00:00Z`).getUTCDay()
  // Sunday is quiet; Saturdays and Wednesdays are the busy days.
  const howMany = weekday === 0 ? 0 : weekday === 6 || weekday === 3 ? 3 + Math.floor(rnd() * 3) : Math.floor(rnd() * 3)

  const used = new Set<number>()
  for (let n = 0; n < howMany; n++) {
    let who = Math.floor(rnd() * PEOPLE.length)
    // One deliberate duplicate-day booking per month, since the app permits them and the
    // dashboard must count that person once.
    if (used.has(who) && !(date.endsWith('-11') && n === 1)) continue
    used.add(who)
    if (who === PEOPLE.length - 1 && rnd() < 0.8) who = 0 // the coordinator rarely books

    const person = PEOPLE[who]
    const uid = userDocs[who].id
    const shifts: Array<Record<string, unknown>> = []

    const doMorning = rnd() < 0.75
    const doAfternoon = rnd() < 0.45

    const areaFor = () =>
      rnd() < 0.25
        ? { type: 'general' }
        : { type: 'specific', specific_areas: pick(person.areas.length ? person.areas : AREAS) }

    if (doMorning) {
      shifts.push({
        shift: 'morning',
        time_range: rnd() < 0.25 ? SHORT_MORNING : MORNING,
        type: areaFor(),
      })
    }
    if (doAfternoon || shifts.length === 0) {
      shifts.push({ shift: 'afternoon', time_range: AFTERNOON, type: areaFor() })
    }

    const meals: string[] = []
    if (doMorning && rnd() < 0.8) meals.push('lunch')
    if (doAfternoon && rnd() < 0.5) meals.push('dinner')
    const sleep = doAfternoon && rnd() < 0.3

    // Local midnight in Madrid, which is what the app writes for a Spanish device. Every
    // fifth booking pretends the volunteer booked from abroad, so the FOREIGN_TIMEZONE
    // flag and the +12h rule both get exercised.
    const midnight = epochSecondsAtLocalMidnight(date)
    const tsSeconds = seq % 5 === 4 ? midnight - 6 * 3600 : midnight

    seq++
    bookingDocs.push({
      id: `demo-booking-${String(seq).padStart(4, '0')}`,
      data: {
        user_id: uid,
        timestamp: { seconds: tsSeconds, nanoseconds: 0 },
        shifts,
        meal_types: meals,
        sleep,
      },
    })
  }
}

// A handful of documents that are wrong in the ways real ones are, so the data-quality
// panel and the anomaly badges have something to show.
bookingDocs.push({
  id: 'demo-booking-anomaly-time',
  data: {
    user_id: userDocs[1].id,
    timestamp: { seconds: epochSecondsAtLocalMidnight(addMonths(firstOfMonth(ty, tm), 0)), nanoseconds: 0 },
    shifts: [{ shift: 'morning', time_range: { start: 'a les nou', end: '13:00' }, type: { type: 'general' } }],
    meal_types: ['lunch'],
    sleep: false,
  },
})
bookingDocs.push({
  id: 'demo-booking-anomaly-area',
  data: {
    user_id: userDocs[2].id,
    timestamp: { seconds: epochSecondsAtLocalMidnight(addMonths(firstOfMonth(ty, tm), 0)) + 0, nanoseconds: 0 },
    // SpecificArea.Unknown really does serialise to "".
    shifts: [{ shift: 'afternoon', time_range: AFTERNOON, type: { type: 'specific', specific_areas: '' } }],
    meal_types: ['dinner', 'breakfast'],
    sleep: true,
  },
})
bookingDocs.push({
  id: 'demo-booking-orphan',
  data: {
    user_id: 'demo-user-vanished',
    timestamp: { seconds: epochSecondsAtLocalMidnight(firstOfMonth(ty, tm)) + 0, nanoseconds: 0 },
    shifts: [{ shift: 'morning', time_range: MORNING, type: { type: 'specific', specific_areas: 'kitchen' } }],
    meal_types: ['lunch'],
    sleep: false,
  },
})

// The app's own payment docs, including the two failure modes that make `amount`
// untrustworthy: paid-while-owing, and a duplicate doc for the same month.
const paymentDocs = [
  {
    id: 'demo-payment-0001',
    data: { user_id: userDocs[0].id, year: ty, month: tm, paid: true, amount: 48 },
  },
  {
    id: 'demo-payment-0002',
    data: { user_id: userDocs[1].id, year: ty, month: tm, paid: false, amount: 24 },
  },
  {
    id: 'demo-payment-0003',
    data: { user_id: userDocs[2].id, year: ty, month: tm, paid: true, amount: 16 },
  },
  {
    id: 'demo-payment-0004',
    data: { user_id: userDocs[2].id, year: ty, month: tm, paid: false, amount: 8 },
  },
]

const users = userDocs.map((d) => parseUser(d.id, d.data as Record<string, unknown>))
const bookings = bookingDocs
  .map((d) => parseBooking(d.id, d.data))
  .filter((b): b is NonNullable<typeof b> => b !== null)
const payments = paymentDocs
  .map((d) => parsePayment(d.id, d.data as Record<string, unknown>))
  .filter((p): p is NonNullable<typeof p> => p !== null)

const stats = applyAll({
  now: Math.floor(Date.now() / 1000),
  // No window: this is a full rebuild of a mirror we just emptied.
  windowFrom: null,
  users,
  bookings,
  payments,
})

const flagged = bookings.filter((b) => b.anomalyFlags !== 0).length
const zeroMinutes = (
  raw().prepare('SELECT COUNT(*) AS n FROM fs_booking_shift WHERE minutes = 0').get() as { n: number }
).n

console.log(
  [
    `Dades de prova carregades (${start} → ${end}):`,
    `  ${users.length} voluntaris`,
    `  ${bookings.length} reserves, ${stats.inserted} inserides`,
    `  ${payments.length} pagaments de l'app (amb un duplicat i un "pagat però amb deute")`,
    `  ${flagged} reserves amb avisos, ${zeroMinutes} torns de 0 minuts`,
  ].join('\n'),
)
