'use server'

/**
 * Server actions for /configuracio. Each one is the same four steps and nothing else:
 * authorise, parse, call a function in lib/mutations.ts, redirect back with a message.
 *
 * The rules live in lib/mutations.ts rather than here so a future page cannot bypass them,
 * and the outcome travels in the query string rather than in component state — the whole
 * page stays a server render with no client-side form library.
 */

import { redirect } from 'next/navigation'
import { revalidatePath } from 'next/cache'

import { requireCoordinator } from '@/lib/authz'
import { parseEurosToCents } from '@/lib/ledger'
import {
  addCommitmentRule,
  addPriceRule,
  endCommitmentRule,
  endPriceRule,
  setAdminDisabled,
  upsertAdmin,
  ValidationError,
} from '@/lib/mutations'
import { setSetting } from '@/lib/settings'
import { monthPeriod } from '@/lib/dates'
import { applyWriteback } from '@/lib/writeback'

function back(section: string, params: Record<string, string>): never {
  const sp = new URLSearchParams({ seccio: section, ...params })
  redirect(`/configuracio?${sp.toString()}`)
}

/**
 * Turns a ValidationError into a message on the page and lets anything else propagate —
 * an unexpected failure should reach the error boundary, not be flattened into a toast.
 */
async function guard(section: string, work: () => Promise<string> | string): Promise<never> {
  let ok: string
  try {
    ok = await work()
  } catch (error) {
    if (error instanceof ValidationError) back(section, { error: error.message })
    throw error
  }
  revalidatePath('/configuracio')
  revalidatePath('/coordinacio')
  back(section, { ok })
}

function str(form: FormData, key: string): string {
  const value = form.get(key)
  return typeof value === 'string' ? value.trim() : ''
}

// --- prices ------------------------------------------------------------------

export async function addPriceAction(form: FormData) {
  const admin = await requireCoordinator()

  return guard('preus', () => {
    const cents = parseEurosToCents(str(form, 'price'))
    if (cents === null) throw new ValidationError('El preu ha de ser un import com 8 o 8,50.')

    const type = str(form, 'volunteerType')
    const member = str(form, 'isMember')

    addPriceRule(
      {
        item: str(form, 'item'),
        volunteerType: type === '' ? null : type,
        isMember: member === '' ? null : member === 'yes',
        unitPriceCents: cents,
        validFrom: str(form, 'validFrom'),
        note: str(form, 'note'),
      },
      admin.email,
    )
    return 'Preu afegit.'
  })
}

export async function endPriceAction(form: FormData) {
  const admin = await requireCoordinator()
  return guard('preus', () => {
    endPriceRule(Number(str(form, 'id')), str(form, 'validTo'), admin.email)
    return 'Preu tancat.'
  })
}

// --- commitments -------------------------------------------------------------

export async function addCommitmentAction(form: FormData) {
  const admin = await requireCoordinator()

  return guard('compromisos', () => {
    const hours = Number(str(form, 'hours').replace(',', '.'))
    if (!Number.isFinite(hours) || hours < 0) {
      throw new ValidationError('Les hores han de ser un nombre com 8 o 20,5.')
    }

    const scopeKind = str(form, 'scopeKind') as 'global' | 'volunteer_type' | 'user'
    const scopeValue =
      scopeKind === 'volunteer_type'
        ? str(form, 'volunteerType')
        : scopeKind === 'user'
          ? str(form, 'uid')
          : null

    addCommitmentRule(
      {
        scopeKind,
        scopeValue: scopeValue === '' ? null : scopeValue,
        area: str(form, 'area'),
        periodKind: str(form, 'periodKind') === 'quarter' ? 'quarter' : 'month',
        targetMinutes: Math.round(hours * 60),
        validFrom: str(form, 'validFrom'),
        note: str(form, 'note'),
      },
      admin.email,
    )
    return 'Compromís afegit.'
  })
}

export async function endCommitmentAction(form: FormData) {
  const admin = await requireCoordinator()
  return guard('compromisos', () => {
    endCommitmentRule(Number(str(form, 'id')), str(form, 'validTo'), admin.email)
    return 'Compromís tancat.'
  })
}

// --- the allowlist -----------------------------------------------------------

export async function saveAdminAction(form: FormData) {
  const admin = await requireCoordinator()

  return guard('accessos', () => {
    const role = str(form, 'role') === 'coordinator' ? 'coordinator' : 'area_responsible'
    const areas = form.getAll('areas').filter((a): a is string => typeof a === 'string')

    upsertAdmin(
      {
        email: str(form, 'email'),
        displayName: str(form, 'displayName') || null,
        role,
        areas: role === 'coordinator' ? [] : areas,
      },
      admin.email,
    )
    return 'Accés guardat.'
  })
}

export async function toggleAdminAction(form: FormData) {
  const admin = await requireCoordinator()
  return guard('accessos', () => {
    const disable = str(form, 'disable') === '1'
    setAdminDisabled(str(form, 'email'), disable, admin.email)
    return disable ? 'Accés retirat.' : 'Accés restablert.'
  })
}

// --- settings ----------------------------------------------------------------

export async function saveSettingsAction(form: FormData) {
  const admin = await requireCoordinator()

  return guard('sync', () => {
    const ratio = Number(str(form, 'atRiskRatio').replace(',', '.'))
    if (!Number.isFinite(ratio) || ratio <= 0 || ratio > 1) {
      throw new ValidationError('El llindar «a prop» ha de ser entre 0 i 1 (per exemple 0,8).')
    }
    const quarters = Number(str(form, 'historyQuarters'))
    if (!Number.isInteger(quarters) || quarters < 1 || quarters > 12) {
      throw new ValidationError('L’historial ha de ser entre 1 i 12 trimestres.')
    }

    setSetting('at_risk_ratio', ratio, admin.email)
    setSetting('sync_history_quarters', quarters, admin.email)
    return 'Preferències guardades.'
  })
}

/**
 * The write-back gate. Turning it on is the moment volunteers' phones start showing our
 * numbers instead of the app's, so it is a deliberate, audited, single-purpose action.
 */
export async function setWritebackAction(form: FormData) {
  const admin = await requireCoordinator()
  return guard('firebase', () => {
    const enabled = str(form, 'enabled') === '1'
    setSetting('writeback_enabled', enabled, admin.email)
    return enabled
      ? 'Escriptura cap a Firebase ACTIVADA.'
      : 'Escriptura cap a Firebase desactivada.'
  })
}

export async function runWritebackAction(form: FormData) {
  const admin = await requireCoordinator()

  return guard('firebase', async () => {
    const year = Number(str(form, 'year'))
    const month = Number(str(form, 'month'))
    if (!Number.isInteger(year) || !Number.isInteger(month) || month < 1 || month > 12) {
      throw new ValidationError('Cal triar un mes vàlid.')
    }

    const result = await applyWriteback(monthPeriod(year, month), admin.email, {
      allowCreate: str(form, 'allowCreate') === '1',
    })
    return (
      `Escrits ${result.applied}, creats ${result.created}, sense canvis ` +
      `${result.skippedNoChange}, conflictes ${result.skippedConflict}, errors ${result.failed}.`
    )
  })
}
