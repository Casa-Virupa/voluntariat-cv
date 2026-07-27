/**
 * Per-request authorisation. This — not the proxy — is the real enforcement point.
 *
 * Every page, server action and route handler that touches data must start with
 * `requireAdmin()` or `requireCoordinator()`. Filtering the UI is not access control:
 * an area_responsible who edits the URL must still get nothing back, which is why the
 * area scope is applied in the query layer via `visibleAreas()`.
 */

// `unauthorized()` / `forbidden()` are canary-only in Next 16, so plain redirects it is.
import { redirect } from 'next/navigation'

import { auth } from './auth.ts'
import { findAdmin, type Admin } from './admins.ts'

export type { Admin } from './admins.ts'

/** The signed-in admin, or null. Never throws — for optional UI like the header. */
export async function currentAdmin(): Promise<Admin | null> {
  const session = await auth()
  return findAdmin(session?.user?.email)
}

/** Redirects to the login page unless the caller is a live allowlisted admin. */
export async function requireAdmin(): Promise<Admin> {
  const admin = await currentAdmin()
  if (!admin) redirect('/login?error=denied')
  return admin
}

/** Coordination-only surfaces: prices, commitments, the allowlist, full resync. */
export async function requireCoordinator(): Promise<Admin> {
  const admin = await requireAdmin()
  if (admin.role !== 'coordinator') redirect('/calendari?error=forbidden')
  return admin
}

/**
 * The area codes an admin may see, or null meaning "no filter".
 * Pass the result straight into the query layer — never trust a client-supplied area.
 */
export function visibleAreas(admin: Admin): string[] | null {
  return admin.seesAllAreas ? null : admin.areas
}

/**
 * Narrows a requested area filter to what the admin is actually allowed to see.
 * Returns the areas to query. An area_responsible asking for someone else's area gets
 * their own areas back, not an error page — the filter is a view, not a permission.
 */
export function resolveAreaFilter(admin: Admin, requested: string | null): string[] | null {
  const allowed = visibleAreas(admin)
  if (!requested) return allowed
  if (allowed === null) return [requested]
  return allowed.includes(requested) ? [requested] : allowed
}
