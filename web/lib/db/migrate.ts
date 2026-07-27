/**
 * Applies migrations, (re)creates the derived views, and seeds the defaults needed for
 * the dashboard to be usable at all. Idempotent — safe to run on every deploy.
 *
 *   npm run db:migrate
 */

import { migrate } from 'drizzle-orm/better-sqlite3/migrator'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

import { db, raw, databaseFile } from './index.ts'
import { appSetting, commitmentRule, priceRule } from './schema.ts'
import { APP_DEFAULT_COMMITMENT, APP_DEFAULT_PRICES_CENTS } from '../contract.ts'

const here = dirname(fileURLToPath(import.meta.url))

export function runMigrations(): void {
  migrate(db, { migrationsFolder: join(here, '../../drizzle') })
  raw().exec(readFileSync(join(here, 'views.sql'), 'utf8'))
  seed()
}

/**
 * Seeds reproduce the app's current behaviour exactly, so that on day one the dashboard
 * and the phone agree. Coordinators then change them from /configuracio.
 */
function seed(): void {
  const now = Math.floor(Date.now() / 1000)
  const SYSTEM = 'system:seed'
  const FOREVER = '1970-01-01'

  const existingPrices = db.select().from(priceRule).all()
  if (existingPrices.length === 0) {
    for (const [item, cents] of Object.entries(APP_DEFAULT_PRICES_CENTS)) {
      db.insert(priceRule)
        .values({
          item,
          volunteerType: null,
          isMember: null,
          unitPriceCents: cents,
          validFrom: FOREVER,
          validTo: null,
          note: "Preu per defecte de l'app (Volunteer.kt)",
          createdBy: SYSTEM,
          createdAt: now,
        })
        .run()
    }
  }

  const existingCommitments = db.select().from(commitmentRule).all()
  if (existingCommitments.length === 0) {
    // The app's only notion of a commitment: a global total, per volunteer type, with
    // Mitra measured per quarter and Habitual per month. Per-area rules are added by
    // coordinators on top of these.
    for (const [type, c] of Object.entries(APP_DEFAULT_COMMITMENT)) {
      db.insert(commitmentRule)
        .values({
          scopeKind: 'volunteer_type',
          scopeValue: type,
          area: '__total__',
          periodKind: c.period,
          targetMinutes: c.minutes,
          validFrom: FOREVER,
          validTo: null,
          note: "Compromís per defecte de l'app (User.getMonthHours)",
          createdBy: SYSTEM,
          createdAt: now,
        })
        .run()
    }
  }

  const defaults: Record<string, unknown> = {
    // Write-back stays OFF until a coordinator has reviewed the reconciliation report.
    // Turning it on rewrites amounts volunteers have already seen on their phones.
    writeback_enabled: false,
    // How far back a normal sync reads. Full resync ignores it.
    sync_history_quarters: 1,
    // Status thresholds for the commitment columns.
    at_risk_ratio: 0.8,
  }
  for (const [key, value] of Object.entries(defaults)) {
    db.insert(appSetting)
      .values({ key, valueJson: JSON.stringify(value), updatedAt: now, updatedBy: SYSTEM })
      .onConflictDoNothing()
      .run()
  }
}

if (import.meta.url === `file://${process.argv[1]}`) {
  runMigrations()
  console.log(`migrated + seeded: ${databaseFile()}`)
}
