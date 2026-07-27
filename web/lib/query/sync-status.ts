/**
 * Sync freshness for the header. Deliberately separate from `lib/sync/run.ts`: that module
 * imports firebase-admin, and the header renders on every page — there is no reason to
 * pull the Firestore SDK into a layout just to read one row.
 */

import { raw } from '../db/index.ts'

export interface SyncStatus
 {
  lastOkAt: number | null
  lastOkMode: string | null
  running: boolean
  runningSince: number | null
  lastFailedAt: number | null
  lastError: string | null
  /** Rows we hold right now, so an empty mirror is obvious rather than looking like zero activity. */
  bookings: number
}

/** Amber past this, red past `STALE_RED_HOURS` — a twice-daily sync is late by 14 h. */
export const STALE_AMBER_HOURS = 14
export const STALE_RED_HOURS = 26

export function syncStatus(): SyncStatus {
  const ok = raw()
    .prepare(
      `SELECT finished_at AS at, mode FROM sync_run
        WHERE status = 'ok' ORDER BY finished_at DESC LIMIT 1`,
    )
    .get() as { at: number | null; mode: string } | undefined

  const running = raw()
    .prepare(`SELECT started_at AS at FROM sync_run WHERE status = 'running' LIMIT 1`)
    .get() as { at: number } | undefined

  const failed = raw()
    .prepare(
      `SELECT finished_at AS at, error_message AS error FROM sync_run
        WHERE status IN ('failed', 'aborted') ORDER BY id DESC LIMIT 1`,
    )
    .get() as { at: number | null; error: string | null } | undefined

  const counts = raw()
    .prepare(`SELECT COUNT(*) AS n FROM fs_booking WHERE deleted_at IS NULL`)
    .get() as { n: number }

  return {
    lastOkAt: ok?.at ?? null,
    lastOkMode: ok?.mode ?? null,
    running: Boolean(running),
    runningSince: running?.at ?? null,
    lastFailedAt: failed?.at ?? null,
    lastError: failed?.error ?? null,
    bookings: counts.n,
  }
}

export type Freshness = 'never' | 'fresh' | 'stale' | 'very_stale'

export function freshnessOf(status: SyncStatus, nowSeconds: number): Freshness {
  if (status.lastOkAt === null) return 'never'
  const hours = (nowSeconds - status.lastOkAt) / 3600
  if (hours > STALE_RED_HOURS) return 'very_stale'
  if (hours > STALE_AMBER_HOURS) return 'stale'
  return 'fresh'
}

/** 'fa 3 h', 'fa 12 min', 'ara mateix'. */
export function relativeCa(seconds: number): string {
  if (seconds < 90) return 'ara mateix'
  const minutes = Math.round(seconds / 60)
  if (minutes < 60) return `fa ${minutes} min`
  const hours = Math.round(minutes / 60)
  if (hours < 36) return `fa ${hours} h`
  return `fa ${Math.round(hours / 24)} dies`
}
