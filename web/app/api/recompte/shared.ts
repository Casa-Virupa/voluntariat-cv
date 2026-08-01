import { NextResponse, type NextRequest } from 'next/server'
import { timingSafeEqual } from 'node:crypto'

import { lastSuccessfulSync } from '@/lib/sync/run'
import type { AttendanceDay } from '@/lib/query/attendance'

/**
 * Shared key for the /api/recompte endpoints, meant for a Google Sheets formula. It
 * travels in the URL (`?clau=`) because IMPORTDATA cannot set headers; `X-Recompte-Key`
 * is also accepted for curl. The counts are not sensitive — the key exists so the
 * numbers are not literally world-readable, nothing more.
 *
 * Excluded from the proxy matcher, like /api/sync: a spreadsheet has no cookie.
 */
export function recompteAuthorised(request: NextRequest): boolean {
  const expected = process.env.RECOMPTE_KEY
  if (!expected) return false

  const provided =
    request.nextUrl.searchParams.get('clau') ?? request.headers.get('x-recompte-key')
  if (!provided) return false

  const ba = Buffer.from(provided)
  const bb = Buffer.from(expected)
  return ba.length === bb.length && timingSafeEqual(ba, bb)
}

export function unauthorised(): NextResponse {
  // Same body whether the key is wrong or the env var is unset — no oracle.
  return NextResponse.json({ error: 'no autoritzat' }, { status: 401 })
}

/** ISO instant of the last successful mirror sync, so the sheet can show data age. */
export function mirrorFreshness(): string | null {
  const last = lastSuccessfulSync()
  return last?.finishedAt ? new Date(last.finishedAt * 1000).toISOString() : null
}

/**
 * The same rows as CSV, for `=IMPORTDATA(...)&format=csv` — one formula, no Apps Script.
 * Catalan headers to match what the coordinators will read in the sheet.
 */
export function csvResponse(days: AttendanceDay[]): NextResponse {
  const lines = ['data,dinars,sopars,pernoctes']
  for (const d of days) lines.push(`${d.date},${d.lunch},${d.dinner},${d.sleep}`)
  return new NextResponse(lines.join('\n') + '\n', {
    headers: { 'content-type': 'text/csv; charset=utf-8' },
  })
}

export function toCatalan(d: AttendanceDay) {
  return { data: d.date, dinars: d.lunch, sopars: d.dinner, pernoctes: d.sleep }
}
