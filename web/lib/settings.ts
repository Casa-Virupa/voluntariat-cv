/**
 * `app_setting` access. Values are JSON so a setting can grow from a boolean into an
 * object without a migration; a malformed value falls back to the default rather than
 * throwing on a page render.
 */

import { eq } from 'drizzle-orm'

import { db } from './db/index.ts'
import { appSetting, auditLog } from './db/schema.ts'
import { DEFAULT_AT_RISK_RATIO } from './commitments.ts'

export function getSetting<T>(key: string, fallback: T): T {
  const row = db.select().from(appSetting).where(eq(appSetting.key, key)).get()
  if (!row) return fallback
  try {
    return JSON.parse(row.valueJson) as T
  } catch {
    return fallback
  }
}

export function setSetting(key: string, value: unknown, actor: string): void {
  const before = db.select().from(appSetting).where(eq(appSetting.key, key)).get()
  const now = Math.floor(Date.now() / 1000)

  db.insert(appSetting)
    .values({ key, valueJson: JSON.stringify(value), updatedAt: now, updatedBy: actor })
    .onConflictDoUpdate({
      target: appSetting.key,
      set: { valueJson: JSON.stringify(value), updatedAt: now, updatedBy: actor },
    })
    .run()

  audit(actor, 'setting.set', 'app_setting', key, before ?? null, { value })
}

/** Every dashboard-owned mutation goes through here, so "who changed this price?" is answerable. */
export function audit(
  actor: string,
  action: string,
  entity: string,
  entityId: string | null,
  before: unknown,
  after: unknown,
): void {
  db.insert(auditLog)
    .values({
      actorEmail: actor,
      action,
      entity,
      entityId,
      beforeJson: before === null || before === undefined ? null : JSON.stringify(before),
      afterJson: after === null || after === undefined ? null : JSON.stringify(after),
      at: Math.floor(Date.now() / 1000),
    })
    .run()
}

/**
 * Catalan for every action string `audit()` is called with. Keep in step with the calls in
 * lib/mutations.ts — an unknown key falls back to the raw string rather than to an empty
 * cell, so a forgotten label is visible instead of silently hiding a change.
 */
export const AUDIT_ACTION_LABEL: Record<string, string> = {
  'ledger.insert': 'Apunt registrat',
  'ledger.void': 'Apunt anul·lat',
  'price.add': 'Preu nou',
  'price.end': 'Preu tancat',
  'commitment.add': 'Compromís nou',
  'commitment.end': 'Compromís tancat',
  'commitment.delete': 'Compromís esborrat',
  'admin.upsert': 'Accés desat',
  'admin.disable': 'Accés desactivat',
  'admin.enable': 'Accés reactivat',
  'period.close': 'Període tancat',
  'setting.set': 'Preferència canviada',
}

export const AUDIT_ENTITY_LABEL: Record<string, string> = {
  ledger_entry: 'Apunt',
  price_rule: 'Preu',
  commitment_rule: 'Compromís',
  admin_user: 'Accés',
  period_close: 'Tancament',
  app_setting: 'Preferència',
}

export function atRiskRatio(): number {
  const value = getSetting('at_risk_ratio', DEFAULT_AT_RISK_RATIO)
  return typeof value === 'number' && value > 0 && value <= 1 ? value : DEFAULT_AT_RISK_RATIO
}

export function writebackEnabled(): boolean {
  return getSetting<boolean>('writeback_enabled', false) === true
}
