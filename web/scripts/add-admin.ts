/**
 * Adds or updates an entry in the allowlist. There is no way to sign in until at least
 * one coordinator exists, so this is the bootstrap step after a fresh deploy.
 *
 *   node scripts/add-admin.ts oscar@example.cat --coordinator --name "Oscar"
 *   node scripts/add-admin.ts cuina@example.cat --areas kitchen,grove
 *   node scripts/add-admin.ts old@example.cat --disable
 *   node scripts/add-admin.ts --list
 */

import { parseArgs } from 'node:util'
import { eq } from 'drizzle-orm'

import { db } from '../lib/db/index.ts'
import { adminUser, adminUserArea } from '../lib/db/schema.ts'
import { normaliseEmail } from '../lib/admins.ts'
import { AREA_CODES, areaLabel } from '../lib/contract.ts'
import { runMigrations } from '../lib/db/migrate.ts'

const { values: flags, positionals } = parseArgs({
  allowPositionals: true,
  options: {
    coordinator: { type: 'boolean', default: false },
    areas: { type: 'string' },
    name: { type: 'string' },
    disable: { type: 'boolean', default: false },
    list: { type: 'boolean', default: false },
  },
})

runMigrations()

if (flags.list) {
  const rows = db.select().from(adminUser).all()
  if (rows.length === 0) {
    console.log('(cap administrador)')
  }
  for (const r of rows) {
    const areas =
      r.role === 'coordinator'
        ? 'totes'
        : db
            .select({ area: adminUserArea.area })
            .from(adminUserArea)
            .where(eq(adminUserArea.email, r.email))
            .all()
            .map((a) => areaLabel(a.area))
            .join(', ') || '(cap àrea)'
    console.log(`${r.disabledAt ? '✗' : '✓'} ${r.email}  ${r.role}  [${areas}]`)
  }
  process.exit(0)
}

const email = normaliseEmail(positionals[0] ?? '')
if (!email || !email.includes('@')) {
  console.error('Cal un correu electrònic. Fes servir --list per veure els existents.')
  process.exit(1)
}

const now = Math.floor(Date.now() / 1000)

if (flags.disable) {
  const res = db.update(adminUser).set({ disabledAt: now }).where(eq(adminUser.email, email)).run()
  console.log(res.changes ? `✓ ${email} desactivat` : `✗ ${email} no existeix`)
  process.exit(res.changes ? 0 : 1)
}

const areas = (flags.areas ?? '')
  .split(',')
  .map((a) => a.trim().toLowerCase())
  .filter(Boolean)

const unknown = areas.filter((a) => !AREA_CODES.includes(a))
if (unknown.length) {
  console.error(`Àrees desconegudes: ${unknown.join(', ')}`)
  console.error(`Vàlides: ${AREA_CODES.join(', ')}`)
  process.exit(1)
}

const role = flags.coordinator ? 'coordinator' : 'area_responsible'
if (role === 'area_responsible' && areas.length === 0) {
  console.error('Un responsable d’àrea necessita --areas, o fes-lo --coordinator.')
  process.exit(1)
}

db.insert(adminUser)
  .values({
    email,
    displayName: flags.name ?? null,
    role,
    addedBy: 'cli',
    addedAt: now,
    disabledAt: null,
  })
  .onConflictDoUpdate({
    target: adminUser.email,
    set: { role, displayName: flags.name ?? null, disabledAt: null },
  })
  .run()

db.delete(adminUserArea).where(eq(adminUserArea.email, email)).run()
for (const area of areas) {
  db.insert(adminUserArea).values({ email, area }).run()
}

console.log(
  `✓ ${email} · ${role}${areas.length ? ` · ${areas.map(areaLabel).join(', ')}` : ' · totes les àrees'}`,
)
