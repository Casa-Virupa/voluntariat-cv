import { NextResponse, type NextRequest } from 'next/server'

import { currentAdmin, resolveAreaFilter, visibleAreas } from '@/lib/authz'
import { areaLabel } from '@/lib/contract'
import { todayInMadrid } from '@/lib/dates'
import { buildWorkbook } from '@/lib/export'
import { coordinationTable } from '@/lib/query/coordination'
import { parseCoordinationSearch } from '@/lib/query/filters'
import { atRiskRatio } from '@/lib/settings'

export const dynamic = 'force-dynamic'

/**
 * The coordination table as .xlsx. Takes exactly the same query string as /coordinacio, so
 * "export what I'm looking at" is literally the same parameters with a different path.
 *
 * The area scope and the payments-visibility rule are re-derived here from the session, not
 * taken from the request: an area_responsible who calls this endpoint directly gets their own
 * areas and no money columns, the same as on the page.
 */
export async function GET(request: NextRequest) {
  const admin = await currentAdmin()
  if (!admin) return NextResponse.json({ error: 'unauthorised' }, { status: 401 })

  const search = Object.fromEntries(request.nextUrl.searchParams.entries())
  const today = todayInMadrid()
  const filters = parseCoordinationSearch(search, today)

  const table = coordinationTable(filters, {
    allowedAreas: visibleAreas(admin),
    queryAreas: resolveAreaFilter(admin, filters.area),
    myVolunteerAreas: admin.seesAllAreas ? [] : admin.areas,
    showPayments: admin.seesAllAreas,
    today,
    atRiskRatio: atRiskRatio(),
  })

  const buffer = await buildWorkbook({
    table,
    period: filters.period,
    actor: admin.email,
    generatedAt: new Date(),
    areaFilterLabel: filters.area ? areaLabel(filters.area) : null,
  })

  const slug =
    filters.period.kind === 'month'
      ? `${filters.period.year}-${String(filters.period.index).padStart(2, '0')}`
      : `${filters.period.year}-T${filters.period.index}`

  return new NextResponse(buffer, {
    headers: {
      'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'Content-Disposition': `attachment; filename="voluntariat-${slug}.xlsx"`,
      // Always freshly generated: an .xlsx cached by the browser would silently be stale.
      'Cache-Control': 'no-store',
    },
  })
}
