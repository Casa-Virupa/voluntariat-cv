import { NextResponse, type NextRequest } from 'next/server'

import { isIsoDate, todayInMadrid } from '@/lib/dates'
import { attendanceByDay } from '@/lib/query/attendance'
import { csvResponse, mirrorFreshness, recompteAuthorised, toCatalan, unauthorised } from '../shared'

export const dynamic = 'force-dynamic'

/**
 * GET /api/recompte/dia?data=YYYY-MM-DD&clau=…
 *
 * How many distinct people have lunch, dinner or an overnight stay booked on one day.
 * `data` defaults to today in Europe/Madrid. `format=csv` returns a two-line CSV for
 * =IMPORTDATA; the JSON default carries `ultima_sincronitzacio` so the sheet can tell
 * how fresh the mirror is.
 */
export async function GET(request: NextRequest) {
  if (!recompteAuthorised(request)) return unauthorised()

  const date = request.nextUrl.searchParams.get('data') ?? todayInMadrid()
  if (!isIsoDate(date)) {
    return NextResponse.json({ error: 'data ha de ser YYYY-MM-DD' }, { status: 400 })
  }

  const [day] = attendanceByDay(date, date)

  if (request.nextUrl.searchParams.get('format') === 'csv') return csvResponse([day])
  return NextResponse.json({ ...toCatalan(day), ultima_sincronitzacio: mirrorFreshness() })
}
