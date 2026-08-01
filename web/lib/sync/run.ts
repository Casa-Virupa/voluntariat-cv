/**
 * Firestore -> SQLite mirror.
 *
 * Firestore has no updated_at on any collection and `volunteers` docs are immutable but
 * deletable, so "fetch what changed" is impossible. Instead we fully re-read a bounded
 * window, which makes deletion detection a free set-difference.
 *
 * Two invariants worth stating plainly, because breaking either loses data silently:
 *
 *   1. FETCH EVERYTHING BEFORE WRITING ANYTHING. A fetch that throws must never reach
 *      the apply step, or a truncated page would tombstone live bookings.
 *   2. DELETION DETECTION IS SCOPED TO THE WINDOW WE ACTUALLY READ. Tombstoning rows
 *      outside it would delete history we simply did not ask Firestore about.
 */

import { and, eq, gte, inArray, isNull, like, lt, sql } from 'drizzle-orm'
import { Timestamp } from 'firebase-admin/firestore'

import { db, raw } from '../db/index.ts'
import {
  appSetting,
  fsBooking,
  fsBookingShift,
  fsUser,
  fsUserArea,
  ledgerEntry,
  syncRun,
} from '../db/schema.ts'
import { firestore } from '../firebase.ts'
import { COLLECTION_LEDGER, COLLECTION_USERS, COLLECTION_VOLUNTEERS } from '../contract.ts'
import { insertLedgerEntry } from '../mutations.ts'
import { FIRESTORE_IMPORT_REF_PREFIX, LEDGER_DOC_PREFIX } from '../publish.ts'
import { addMonths, epochSecondsAtLocalMidnight, firstOfMonth, firstMonthOfQuarter, localDate, quarterOf } from '../dates.ts'
import { ANOMALY } from './anomalies.ts'
import {
  parseBooking,
  parseLedgerDoc,
  parseUser,
  type ParsedBooking,
  type ParsedLedgerEntry,
  type ParsedUser,
} from './parse.ts'

const PAGE_SIZE = 1000
const STALE_LOCK_SECONDS = 10 * 60

/** Refuse to tombstone more than this share of the window in one run. */
const MASS_DELETE_RATIO = 0.25
const MASS_DELETE_FLOOR = 20

export interface SyncResult {
  runId: number
  mode: 'window' | 'full'
  windowFrom: string | null
  readUsers: number
  readBookings: number
  readLedger: number
  importedLedger: number
  insertedBookings: number
  deletedBookings: number
  anomalies: number
  skipped: number
  durationMs: number
}

export class SyncBusyError extends Error {
  // Declared explicitly, not as a TS parameter property: files under lib/ are executed
  // directly by `node --test` in strip-only mode, which cannot transform those.
  readonly runningId: number

  constructor(runningId: number) {
    super('Ja hi ha una sincronització en curs')
    this.runningId = runningId
  }
}

/** First day of the quarter N quarters before today — the default read window. */
export function defaultWindowFrom(today: string, quartersBack: number): string {
  const [y, m] = today.split('-').map(Number)
  const startOfThisQuarter = firstOfMonth(y, firstMonthOfQuarter(quarterOf(m)))
  return addMonths(startOfThisQuarter, -3 * quartersBack)
}

function setting<T>(key: string, fallback: T): T {
  const row = db.select().from(appSetting).where(eq(appSetting.key, key)).get()
  if (!row) return fallback
  try {
    return JSON.parse(row.valueJson) as T
  } catch {
    return fallback
  }
}

export async function runSync(opts: {
  mode?: 'window' | 'full'
  trigger: 'cron' | 'manual'
  triggeredBy?: string | null
}): Promise<SyncResult> {
  const started = Date.now()
  const now = Math.floor(started / 1000)
  const mode = opts.mode ?? 'window'

  const windowFrom =
    mode === 'full'
      ? null
      : defaultWindowFrom(localDate(now), setting('sync_history_quarters', 1))

  const runId = acquireLock(now, mode, opts.trigger, opts.triggeredBy ?? null, windowFrom)

  try {
    // ---- 1. FETCH (no database writes in this phase) ------------------------
    const fs = firestore()

    const userDocs = await fs.collection(COLLECTION_USERS).get()
    const users: ParsedUser[] = userDocs.docs.map((d) =>
      parseUser(d.id, d.data() as Record<string, unknown>),
    )
    heartbeat(runId)

    const bookings: ParsedBooking[] = []
    let skipped = 0
    {
      const base: FirebaseFirestore.Query = windowFrom
        ? fs
            .collection(COLLECTION_VOLUNTEERS)
            .where(
              'timestamp',
              '>=',
              Timestamp.fromMillis(epochSecondsAtLocalMidnight(windowFrom) * 1000),
            )
        : fs.collection(COLLECTION_VOLUNTEERS)
      const query: FirebaseFirestore.Query = base.orderBy('timestamp').limit(PAGE_SIZE)

      let cursor: FirebaseFirestore.QueryDocumentSnapshot | null = null
      for (;;) {
        const page: FirebaseFirestore.QuerySnapshot = await (
          cursor ? query.startAfter(cursor) : query
        ).get()
        if (page.empty) break
        for (const d of page.docs) {
          const parsed = parseBooking(d.id, d.data() as Record<string, unknown>)
          if (parsed) bookings.push(parsed)
          else skipped++
        }
        heartbeat(runId)
        if (page.size < PAGE_SIZE) break
        cursor = page.docs[page.docs.length - 1]
      }
    }

    // The app's money facts. Docs the dashboard itself published (dash-*) come back here
    // too; the apply step skips them by ID, and everything else is imported once, keyed
    // by external_ref.
    const ledgerDocs = await fs.collection(COLLECTION_LEDGER).get()
    const ledger: ParsedLedgerEntry[] = []
    for (const d of ledgerDocs.docs) {
      const parsed = parseLedgerDoc(d.id, d.data() as Record<string, unknown>)
      if (parsed) ledger.push(parsed)
      else skipped++
    }

    // ---- 2. APPLY (one transaction, nothing partial survives) ---------------
    const stats = applyAll({ now, windowFrom, users, bookings, ledger })

    const result: SyncResult = {
      runId,
      mode,
      windowFrom,
      readUsers: users.length,
      readBookings: bookings.length,
      readLedger: ledger.length,
      importedLedger: stats.importedLedger,
      insertedBookings: stats.inserted,
      deletedBookings: stats.deleted,
      anomalies: stats.anomalies,
      skipped,
      durationMs: Date.now() - started,
    }

    db.update(syncRun)
      .set({
        status: 'ok',
        finishedAt: Math.floor(Date.now() / 1000),
        heartbeatAt: Math.floor(Date.now() / 1000),
        readUsers: result.readUsers,
        readBookings: result.readBookings,
        readLedger: result.readLedger,
        insertedBookings: result.insertedBookings,
        deletedBookings: result.deletedBookings,
        anomalies: result.anomalies,
      })
      .where(eq(syncRun.id, runId))
      .run()

    return result
  } catch (error) {
    db.update(syncRun)
      .set({
        status: 'failed',
        finishedAt: Math.floor(Date.now() / 1000),
        errorMessage: error instanceof Error ? error.message : String(error),
      })
      .where(eq(syncRun.id, runId))
      .run()
    throw error
  }
}

/**
 * The mutex is a partial unique index on sync_run(status) WHERE status='running', so the
 * database itself refuses a second concurrent run. That survives a restart and a second
 * process, which an in-process JS lock would not.
 */
function acquireLock(
  now: number,
  mode: 'window' | 'full',
  trigger: 'cron' | 'manual',
  triggeredBy: string | null,
  windowFrom: string | null,
): number {
  const insert = () =>
    db
      .insert(syncRun)
      .values({
        mode,
        trigger,
        triggeredBy,
        status: 'running',
        startedAt: now,
        heartbeatAt: now,
        windowFrom,
      })
      .returning({ id: syncRun.id })
      .get().id

  try {
    return insert()
  } catch {
    const running = db.select().from(syncRun).where(eq(syncRun.status, 'running')).get()
    if (running && now - running.heartbeatAt > STALE_LOCK_SECONDS) {
      // The previous run died without releasing. Reap it and take over.
      db.update(syncRun)
        .set({ status: 'aborted', finishedAt: now, errorMessage: 'stale lock reaped' })
        .where(eq(syncRun.id, running.id))
        .run()
      return insert()
    }
    throw new SyncBusyError(running?.id ?? -1)
  }
}

function heartbeat(runId: number): void {
  db.update(syncRun)
    .set({ heartbeatAt: Math.floor(Date.now() / 1000) })
    .where(eq(syncRun.id, runId))
    .run()
}

export function applyAll(input: {
  now: number
  windowFrom: string | null
  users: ParsedUser[]
  bookings: ParsedBooking[]
  ledger: ParsedLedgerEntry[]
}): { inserted: number; deleted: number; anomalies: number; importedLedger: number } {
  const { now, windowFrom, users, bookings, ledger } = input

  const knownUsers = new Set(users.map((u) => u.uid))

  // Duplicate-day and orphan flags need the whole set, so they are computed here rather
  // than in the per-document parser.
  const perUserDay = new Map<string, number>()
  for (const b of bookings) {
    const key = `${b.userId}|${b.serviceDate}`
    perUserDay.set(key, (perUserDay.get(key) ?? 0) + 1)
  }
  for (const b of bookings) {
    if (!knownUsers.has(b.userId)) b.anomalyFlags |= ANOMALY.ORPHAN_USER
    if ((perUserDay.get(`${b.userId}|${b.serviceDate}`) ?? 0) > 1) {
      b.anomalyFlags |= ANOMALY.DUPLICATE_DAY
    }
  }

  const tx = raw().transaction(() => {
    // --- users ---------------------------------------------------------------
    for (const u of users) {
      db.insert(fsUser)
        .values({
          uid: u.uid,
          name: u.name,
          email: u.email,
          role: u.role,
          volunteerType: u.volunteerType,
          onboardingCompleted: u.onboardingCompleted,
          isMember: u.isMember,
          rawJson: u.rawJson,
          docHash: u.docHash,
          firstSeenAt: now,
          lastSeenAt: now,
          deletedAt: null,
        })
        .onConflictDoUpdate({
          target: fsUser.uid,
          set: {
            name: u.name,
            email: u.email,
            role: u.role,
            volunteerType: u.volunteerType,
            onboardingCompleted: u.onboardingCompleted,
            isMember: u.isMember,
            rawJson: u.rawJson,
            docHash: u.docHash,
            lastSeenAt: now,
            deletedAt: null,
          },
        })
        .run()

      db.delete(fsUserArea).where(eq(fsUserArea.uid, u.uid)).run()
      for (const area of u.areas) {
        db.insert(fsUserArea).values({ uid: u.uid, area }).onConflictDoNothing().run()
      }
    }

    // `users` is read in full every run, so anything missing really is gone. Soft
    // delete: bookings must keep resolving to a name.
    if (knownUsers.size > 0) {
      db.update(fsUser)
        .set({ deletedAt: now })
        .where(and(isNull(fsUser.deletedAt), notIn(fsUser.uid, [...knownUsers])))
        .run()
    }

    // --- bookings ------------------------------------------------------------
    let inserted = 0
    for (const b of bookings) {
      const existing = db
        .select({ hash: fsBooking.docHash })
        .from(fsBooking)
        .where(eq(fsBooking.docId, b.docId))
        .get()

      // Volunteer docs are immutable, so an unchanged hash is the common case and lets
      // us skip rewriting the child rows entirely.
      if (existing?.hash === b.docHash) {
        db.update(fsBooking)
          .set({ lastSeenAt: now, deletedAt: null })
          .where(eq(fsBooking.docId, b.docId))
          .run()
        continue
      }

      db.insert(fsBooking)
        .values({
          docId: b.docId,
          userId: b.userId,
          tsEpoch: b.tsEpoch,
          serviceDate: b.serviceDate,
          hasLunch: b.hasLunch,
          hasDinner: b.hasDinner,
          sleep: b.sleep,
          mealsRawJson: b.mealsRawJson,
          shiftCount: b.shiftCount,
          totalMinutes: b.totalMinutes,
          anomalyFlags: b.anomalyFlags,
          rawJson: b.rawJson,
          docHash: b.docHash,
          firstSeenAt: now,
          lastSeenAt: now,
          deletedAt: null,
        })
        .onConflictDoUpdate({
          target: fsBooking.docId,
          set: {
            userId: b.userId,
            tsEpoch: b.tsEpoch,
            serviceDate: b.serviceDate,
            hasLunch: b.hasLunch,
            hasDinner: b.hasDinner,
            sleep: b.sleep,
            mealsRawJson: b.mealsRawJson,
            shiftCount: b.shiftCount,
            totalMinutes: b.totalMinutes,
            anomalyFlags: b.anomalyFlags,
            rawJson: b.rawJson,
            docHash: b.docHash,
            lastSeenAt: now,
            deletedAt: null,
          },
        })
        .run()

      db.delete(fsBookingShift).where(eq(fsBookingShift.docId, b.docId)).run()
      for (const s of b.shifts) {
        db.insert(fsBookingShift)
          .values({
            docId: b.docId,
            seq: s.seq,
            slot: s.slot,
            kind: s.kind,
            area: s.area,
            startSec: s.startSec,
            endSec: s.endSec,
            minutes: s.minutes,
          })
          .run()
      }
      inserted++
    }

    // --- deletion detection, strictly inside the window we read ---------------
    const seen = new Set(bookings.map((b) => b.docId))
    const liveInWindow = db
      .select({ docId: fsBooking.docId })
      .from(fsBooking)
      .where(
        windowFrom
          ? and(isNull(fsBooking.deletedAt), gte(fsBooking.serviceDate, windowFrom))
          : isNull(fsBooking.deletedAt),
      )
      .all()

    const toDelete = liveInWindow.filter((r) => !seen.has(r.docId)).map((r) => r.docId)

    // A truncated page or a transient Firestore error must never wipe a quarter.
    const ceiling = Math.max(MASS_DELETE_FLOOR, Math.floor(liveInWindow.length * MASS_DELETE_RATIO))
    if (toDelete.length > ceiling) {
      throw new Error(
        `Protecció d'esborrat massiu: la sincronització esborraria ${toDelete.length} de ` +
          `${liveInWindow.length} reserves (màxim ${ceiling}). No s'ha aplicat cap canvi.`,
      )
    }

    for (const chunk of chunks(toDelete, 500)) {
      db.update(fsBooking)
        .set({ deletedAt: now })
        .where(inArray(fsBooking.docId, chunk))
        .run()
    }

    // --- the ledger ------------------------------------------------------------
    // Import money entries written outside the dashboard (app-side bootstrap, the
    // Firebase console) into `ledger_entry`, exactly once each: the doc ID becomes the
    // external_ref and ux_ledger_external_ref refuses a second copy. The dashboard's own
    // publishes (dash-*) are skipped — they ARE ledger_entry rows already. ledger_entry
    // is insert-only, so a doc later deleted from Firestore stays on the books here.
    let importedLedger = 0
    {
      const existingRefs = new Set(
        db
          .select({ ref: ledgerEntry.externalRef })
          .from(ledgerEntry)
          .where(like(ledgerEntry.externalRef, `${FIRESTORE_IMPORT_REF_PREFIX}%`))
          .all()
          .map((r) => r.ref),
      )
      for (const e of ledger) {
        if (e.docId.startsWith(LEDGER_DOC_PREFIX)) continue
        const ref = `${FIRESTORE_IMPORT_REF_PREFIX}${e.docId}`
        if (existingRefs.has(ref)) continue
        insertLedgerEntry(
          {
            userId: e.userId,
            kind: e.kind,
            effectiveDate: e.date,
            amountCents: e.amountCents,
            method: null,
            note: e.note,
            externalRef: ref,
          },
          'firestore-sync',
        )
        importedLedger++
      }
    }

    const anomalies = bookings.filter((b) => b.anomalyFlags !== 0).length
    return { inserted, deleted: toDelete.length, anomalies, importedLedger }
  })

  return tx()
}

/** drizzle has no notInArray helper that handles an empty list safely. */
function notIn(column: Parameters<typeof inArray>[0], values: string[]) {
  if (values.length === 0) return sql`1=1`
  return sql`${column} NOT IN (${sql.join(values.map((v) => sql`${v}`), sql`, `)})`
}

function* chunks<T>(items: T[], size: number): Generator<T[]> {
  for (let i = 0; i < items.length; i += size) yield items.slice(i, i + size)
}

/** For the "Actualitzat fa X" indicator in the header. */
export function lastSuccessfulSync() {
  return db
    .select()
    .from(syncRun)
    .where(eq(syncRun.status, 'ok'))
    .orderBy(sql`${syncRun.finishedAt} DESC`)
    .limit(1)
    .get()
}

export function currentlyRunning() {
  return db.select().from(syncRun).where(eq(syncRun.status, 'running')).get()
}

// re-exported for the route handler's window preview
export { lt }
