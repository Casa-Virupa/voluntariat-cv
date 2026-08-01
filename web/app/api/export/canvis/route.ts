import { NextResponse } from 'next/server'

import { currentAdmin } from '@/lib/authz'
import { todayInMadrid } from '@/lib/dates'
import { buildAuditWorkbook } from '@/lib/export'
import { allAuditEntries } from '@/lib/query/config'

export const dynamic = 'force-dynamic'

/**
 * The whole change log as .xlsx. Takes no parameters: the screen is paged, this is the
 * archive, and an export that quietly honoured a page number would be a trap.
 *
 * Coordinator-only, re-checked here from the session — /configuracio is the only page that
 * links to it, but a link is not a permission.
 */
export async function GET() {
  const admin = await currentAdmin()
  if (!admin) return NextResponse.json({ error: 'unauthorised' }, { status: 401 })
  if (admin.role !== 'coordinator') {
    return NextResponse.json({ error: 'forbidden' }, { status: 403 })
  }

  const buffer = await buildAuditWorkbook({
    entries: allAuditEntries(),
    actor: admin.email,
    generatedAt: new Date(),
  })

  return new NextResponse(buffer, {
    headers: {
      'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'Content-Disposition': `attachment; filename="voluntariat-canvis-${todayInMadrid()}.xlsx"`,
      'Cache-Control': 'no-store',
    },
  })
}
