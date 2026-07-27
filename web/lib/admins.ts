/**
 * The allowlist. Pure database access, no framework imports — both the Auth.js config
 * and the per-request authorisation helper depend on this, and neither may depend on
 * the other.
 */

import { and, eq, isNull } from 'drizzle-orm'

import { db } from './db/index.ts'
import { adminUser, adminUserArea } from './db/schema.ts'
import { AREA_CODES } from './contract.ts'

export type AdminRole = 'coordinator' | 'area_responsible'

export interface Admin {
  email: string
  displayName: string | null
  role: AdminRole
  /** Areas this admin may see. A coordinator gets every area. */
  areas: string[]
  /** True when no area filter should be applied at all. */
  seesAllAreas: boolean
}

export function normaliseEmail(email: string): string {
  return email.trim().toLowerCase()
}

/** Returns null when the email is not on the allowlist or has been disabled. */
export function findAdmin(email: string | null | undefined): Admin | null {
  if (!email) return null

  const row = db
    .select()
    .from(adminUser)
    .where(and(eq(adminUser.email, normaliseEmail(email)), isNull(adminUser.disabledAt)))
    .get()

  if (!row) return null

  const role = row.role as AdminRole
  if (role === 'coordinator') {
    return {
      email: row.email,
      displayName: row.displayName,
      role,
      areas: AREA_CODES,
      seesAllAreas: true,
    }
  }

  const areas = db
    .select({ area: adminUserArea.area })
    .from(adminUserArea)
    .where(eq(adminUserArea.email, row.email))
    .all()
    .map((a) => a.area)

  return { email: row.email, displayName: row.displayName, role, areas, seesAllAreas: false }
}

export function isAllowed(email: string | null | undefined): boolean {
  return findAdmin(email) !== null
}
