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
import { writeLinks } from '@/lib/links'
import { setSetting } from '@/lib/settings'
import { publishLedgerEntry, publishPriceRules, pendingLedgerPublishes } from '@/lib/publish'

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

  return guard('preus', async () => {
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
    await publishPriceRules(admin.email)
    return 'Preu afegit i publicat a l’app.'
  })
}

export async function endPriceAction(form: FormData) {
  const admin = await requireCoordinator()
  return guard('preus', async () => {
    endPriceRule(Number(str(form, 'id')), str(form, 'validTo'), admin.email)
    await publishPriceRules(admin.email)
    return 'Preu tancat i publicat a l’app.'
  })
}

/** Manual re-publish, for when Firestore was unreachable during a price mutation. */
export async function publishPricesAction() {
  const admin = await requireCoordinator()
  return guard('firebase', async () => {
    const result = await publishPriceRules(admin.email)
    return `Preus publicats: ${result.written} docs escrits, ${result.deleted} eliminats.`
  })
}

/** Retries every SQLite ledger row whose Firestore doc is missing. */
export async function republishLedgerAction() {
  const admin = await requireCoordinator()
  return guard('firebase', async () => {
    const pending = await pendingLedgerPublishes()
    let published = 0
    for (const id of pending) {
      if (await publishLedgerEntry(id, admin.email)) published++
    }
    return published === 0
      ? 'No hi havia cap apunt pendent de publicar.'
      : `Publicats ${published} apunts a l’app.`
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

// --- the app's links text ------------------------------------------------------

export async function saveLinksAction(form: FormData) {
  const admin = await requireCoordinator()

  return guard('enllacos', async () => {
    const value = form.get('text')
    // trim() only at the ends — the inner newlines ARE the content the app renders
    const text = typeof value === 'string' ? value.trim() : ''
    await writeLinks(text, admin.email)
    return 'Enllaços guardats. Els voluntaris els veuran en obrir l’app.'
  })
}

