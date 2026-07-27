'use server'

/**
 * Server actions for /coordinacio: recording money and closing a period.
 *
 * All three are coordinator-only. An area_responsible's table has no payment columns at all
 * (enforced in the query, see lib/query/coordination.ts), and `requireCoordinator()` here is
 * the second lock — a form posted by hand must fail too.
 *
 * Each action carries a `returnTo` so the coordinator lands back on the same period, filters
 * and open side panel instead of at the top of an unfiltered table.
 */

import { redirect } from 'next/navigation'
import { revalidatePath } from 'next/cache'

import { requireCoordinator } from '@/lib/authz'
import { parseEurosToCents } from '@/lib/ledger'
import { closePeriodForUser, insertLedgerEntry, ValidationError, voidLedgerEntry } from '@/lib/mutations'
import { monthPeriod, quarterPeriod } from '@/lib/dates'

function str(form: FormData, key: string): string {
  const value = form.get(key)
  return typeof value === 'string' ? value.trim() : ''
}

/**
 * Only same-site paths are honoured. `returnTo` comes from a form field, and a redirect that
 * accepted an absolute URL would be an open redirect.
 */
function safeReturn(form: FormData): string {
  const raw = str(form, 'returnTo')
  return raw.startsWith('/') && !raw.startsWith('//') ? raw : '/coordinacio'
}

function withMessage(base: string, key: 'ok' | 'error', message: string): string {
  const [path, query = ''] = base.split('?')
  const sp = new URLSearchParams(query)
  sp.delete('ok')
  sp.delete('error')
  sp.set(key, message)
  return `${path}?${sp.toString()}`
}

async function guard(form: FormData, work: () => Promise<string> | string): Promise<never> {
  const back = safeReturn(form)
  let ok: string
  try {
    ok = await work()
  } catch (error) {
    if (error instanceof ValidationError) redirect(withMessage(back, 'error', error.message))
    throw error
  }
  revalidatePath('/coordinacio')
  redirect(withMessage(back, 'ok', ok))
}

/**
 * A payment or an adjustment. Sign convention: what a coordinator types is what the
 * volunteer is credited, so a 24 € payment reduces their balance by 24 €. A negative
 * adjustment is an extra charge — the form says so explicitly.
 */
export async function addLedgerEntryAction(form: FormData) {
  const admin = await requireCoordinator()

  return guard(form, () => {
    const cents = parseEurosToCents(str(form, 'amount'))
    if (cents === null) throw new ValidationError('L’import ha de ser un número com 24 o 24,50.')

    const kind = str(form, 'kind')
    if (kind !== 'payment' && kind !== 'adjustment' && kind !== 'write_off' && kind !== 'opening_balance') {
      throw new ValidationError('Tipus d’apunt desconegut.')
    }

    const method = str(form, 'method')

    insertLedgerEntry(
      {
        userId: str(form, 'uid'),
        kind,
        effectiveDate: str(form, 'effectiveDate'),
        amountCents: cents,
        method: method === '' ? null : method,
        note: str(form, 'note'),
      },
      admin.email,
    )

    return kind === 'payment' ? 'Pagament registrat.' : 'Apunt registrat.'
  })
}

export async function voidLedgerEntryAction(form: FormData) {
  const admin = await requireCoordinator()

  return guard(form, () => {
    voidLedgerEntry(Number(str(form, 'id')), admin.email, str(form, 'reason'))
    return 'Apunt anul·lat.'
  })
}

/**
 * Freezes a volunteer's charges for the period. After this, a price correction no longer
 * moves the total — which is the point, and also why it cannot be undone from the UI.
 */
export async function closePeriodAction(form: FormData) {
  const admin = await requireCoordinator()

  return guard(form, () => {
    const kind = str(form, 'periodKind') === 'quarter' ? 'quarter' : 'month'
    const year = Number(str(form, 'year'))
    const index = Number(str(form, 'index'))
    if (!Number.isInteger(year) || !Number.isInteger(index)) {
      throw new ValidationError('Període no vàlid.')
    }

    const period = kind === 'quarter' ? quarterPeriod(year, index) : monthPeriod(year, index)
    const total = closePeriodForUser(str(form, 'uid'), period, admin.email)
    return `Període tancat amb ${(total / 100).toFixed(2)} € congelats.`
  })
}
