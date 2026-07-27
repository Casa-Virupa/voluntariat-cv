import Database from 'better-sqlite3'
import { drizzle } from 'drizzle-orm/better-sqlite3'
import { mkdirSync } from 'node:fs'
import { dirname, resolve } from 'node:path'

import * as schema from './schema.ts'

/**
 * One SQLite connection per process, and the app is deliberately single-process.
 *
 * WAL lets the dashboard keep serving reads while a sync holds a write transaction.
 * busy_timeout covers the brief moments the writer holds the lock. foreign_keys is off
 * by default in SQLite and must be enabled per connection.
 */
function open() {
  const file = resolve(process.env.DATABASE_PATH ?? './data/voluntariat.db')
  mkdirSync(dirname(file), { recursive: true })

  const sqlite = new Database(file)
  sqlite.pragma('journal_mode = WAL')
  sqlite.pragma('foreign_keys = ON')
  sqlite.pragma('busy_timeout = 5000')
  sqlite.pragma('synchronous = NORMAL')

  return drizzle(sqlite, { schema })
}

// Next dev reloads modules on every edit; without the global the process would leak a
// file handle per reload.
const globalForDb = globalThis as unknown as { __db?: ReturnType<typeof open> }

export const db = globalForDb.__db ?? open()
if (process.env.NODE_ENV !== 'production') globalForDb.__db = db

export { schema }

/** The underlying better-sqlite3 handle, for pragmas, backups and raw DDL. */
export function raw() {
  // drizzle keeps the driver on a well-known symbol-free property
  return (db as unknown as { $client: Database.Database }).$client
}

export function databaseFile(): string {
  return resolve(process.env.DATABASE_PATH ?? './data/voluntariat.db')
}

export function isMigrated(): boolean {
  const row = raw()
    .prepare(`SELECT name FROM sqlite_master WHERE type='table' AND name='fs_booking'`)
    .get()
  return Boolean(row)
}
