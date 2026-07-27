import { NextResponse, type NextRequest } from 'next/server'
import { timingSafeEqual } from 'node:crypto'

import { currentAdmin } from '@/lib/authz'
import { runSync, SyncBusyError, currentlyRunning, lastSuccessfulSync } from '@/lib/sync/run'

export const dynamic = 'force-dynamic'

/**
 * Triggered two ways:
 *   - cron on the VPS, authenticating with a shared key header
 *   - the "sincronitzar ara" button, authenticating with the admin session
 *
 * Excluded from the proxy matcher precisely because cron has no cookie.
 */
async function authorise(request: NextRequest): Promise<
  { ok: true; who: string; isCoordinator: boolean } | { ok: false; status: number }
> {
  const provided = request.headers.get('x-sync-key')
  const expected = process.env.SYNC_KEY

  if (provided && expected && constantTimeEqual(provided, expected)) {
    return { ok: true, who: 'cron', isCoordinator: true }
  }

  const admin = await currentAdmin()
  if (admin) {
    return { ok: true, who: admin.email, isCoordinator: admin.role === 'coordinator' }
  }

  // Don't distinguish "wrong key" from "no session" — both are just unauthorised.
  return { ok: false, status: 401 }
}

function constantTimeEqual(a: string, b: string): boolean {
  const ba = Buffer.from(a)
  const bb = Buffer.from(b)
  if (ba.length !== bb.length) return false
  return timingSafeEqual(ba, bb)
}

export async function POST(request: NextRequest) {
  const auth = await authorise(request)
  if (!auth.ok) return NextResponse.json({ error: 'unauthorised' }, { status: auth.status })

  const full = request.nextUrl.searchParams.get('full') === '1'
  if (full && !auth.isCoordinator) {
    return NextResponse.json(
      { error: 'Una resincronització completa només la pot fer coordinació.' },
      { status: 403 },
    )
  }

  try {
    const result = await runSync({
      mode: full ? 'full' : 'window',
      trigger: auth.who === 'cron' ? 'cron' : 'manual',
      triggeredBy: auth.who,
    })
    return NextResponse.json({ ok: true, ...result })
  } catch (error) {
    if (error instanceof SyncBusyError) {
      return NextResponse.json(
        { error: error.message, runningId: error.runningId },
        { status: 409 },
      )
    }
    return NextResponse.json(
      { error: error instanceof Error ? error.message : String(error) },
      { status: 500 },
    )
  }
}

/** Status only — cron uses POST. Handy for `curl` and for the header indicator. */
export async function GET(request: NextRequest) {
  const auth = await authorise(request)
  if (!auth.ok) return NextResponse.json({ error: 'unauthorised' }, { status: auth.status })

  return NextResponse.json({
    running: currentlyRunning() ?? null,
    lastSuccessful: lastSuccessfulSync() ?? null,
  })
}
