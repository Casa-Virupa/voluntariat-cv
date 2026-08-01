import { NextResponse, type NextRequest } from 'next/server'

import { eachDay, isIsoDate } from '@/lib/dates'
import { attendanceByDay } from '@/lib/query/attendance'
import { csvResponse, mirrorFreshness, recompteAuthorised, toCatalan, unauthorised } from '../shared'

export const dynamic = 'force-dynamic'

/** A year and a day, so "one whole year" works and a typo'd millennium does not. */
const MAX_DAYS = 366

/**
 * GET /api/recompte/interval?des=YYYY-MM-DD&fins=YYYY-MM-DD&clau=…
 *
 * One row per day from `des` to `fins`, BOTH INCLUDED — human dates for a spreadsheet,
 * unlike the half-open ranges everywhere inside the dashboard. Days with no bookings
 * come back as zeros, so the sheet always gets exactly (fins − des + 1) rows.
 * `format=csv` returns a table =IMPORTDATA can paste straight into the grid.
 */
export async function GET(request: NextRequest) {
  if (!recompteAuthorised(request)) return unauthorised()

  const des = request.nextUrl.searchParams.get('des') ?? ''
  const fins = request.nextUrl.searchParams.get('fins') ?? ''
  if (!isIsoDate(des) || !isIsoDate(fins)) {
    return NextResponse.json({ error: 'des i fins han de ser YYYY-MM-DD' }, { status: 400 })
  }
  if (fins < des) {
    return NextResponse.json({ error: 'fins ha de ser igual o posterior a des' }, { status: 400 })
  }
  if (eachDay(des, fins).length > MAX_DAYS) {
    return NextResponse.json({ error: `màxim ${MAX_DAYS} dies per consulta` }, { status: 400 })
  }

  const days = attendanceByDay(des, fins)

  if (request.nextUrl.searchParams.get('format') === 'csv') return csvResponse(days)
  return NextResponse.json({
    dies: days.map(toCatalan),
    ultima_sincronitzacio: mirrorFreshness(),
  })
}
