import Link from 'next/link'

import { signOut } from '@/lib/auth'
import { requireAdmin } from '@/lib/authz'
import { nowSeconds } from '@/lib/dates'
import { freshnessOf, relativeCa, syncStatus } from '@/lib/query/sync-status'

import { SyncButton } from './_components/SyncButton'

export default async function DashboardLayout({ children }: { children: React.ReactNode }) {
  const admin = await requireAdmin()
  const status = syncStatus()
  const now = nowSeconds()
  const freshness = freshnessOf(status, now)

  return (
    <div className="min-h-screen">
      <header className="border-b border-line bg-surface">
        <div className="mx-auto flex max-w-[1600px] flex-wrap items-center gap-x-6 gap-y-2 px-6 py-3">
          <Link href="/calendari" className="flex items-center gap-2.5">
            <span className="flex size-8 items-center justify-center rounded-lg bg-brand-500 text-xs font-bold text-white">
              CV
            </span>
            <span className="text-sm font-semibold text-brand-900">Voluntariat</span>
          </Link>

          <nav className="flex items-center gap-1 text-sm">
            <NavLink href="/calendari">Calendari</NavLink>
            <NavLink href="/coordinacio">Coordinació</NavLink>
            {admin.role === 'coordinator' && (
              <NavLink href="/configuracio">Configuració</NavLink>
            )}
          </nav>

          <div className="ml-auto flex flex-wrap items-center gap-4">
            <SyncIndicator
              freshness={freshness}
              lastOkAt={status.lastOkAt}
              running={status.running}
              now={now}
            />
            <SyncButton />
            <span className="text-xs text-slate-500">
              {admin.displayName ?? admin.email}
              {admin.role === 'area_responsible' && (
                <span className="ml-1 text-slate-400">· responsable d’àrea</span>
              )}
            </span>
            <form
              action={async () => {
                'use server'
                await signOut({ redirectTo: '/login' })
              }}
            >
              <button
                type="submit"
                className="rounded-md px-2 py-1 text-xs text-slate-500 transition hover:bg-canvas hover:text-slate-800"
              >
                Surt
              </button>
            </form>
          </div>
        </div>

        {status.lastError && freshness !== 'fresh' && (
          <div className="border-t border-bad/20 bg-bad/5 px-6 py-1.5 text-center text-xs text-red-800">
            L’última sincronització va fallar: {status.lastError}
          </div>
        )}
      </header>

      <main className="mx-auto max-w-[1600px] p-6">{children}</main>
    </div>
  )
}

function NavLink({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <Link
      href={href}
      className="rounded-md px-3 py-1.5 text-slate-600 transition hover:bg-canvas hover:text-brand-700"
    >
      {children}
    </Link>
  )
}

/**
 * How stale the mirror is. With a twice-daily sync, "14 h ago" is already suspicious and
 * "26 h ago" means a run was missed — every number on every page is that old.
 */
function SyncIndicator({
  freshness,
  lastOkAt,
  running,
  now,
}: {
  freshness: ReturnType<typeof freshnessOf>
  lastOkAt: number | null
  running: boolean
  now: number
}) {
  if (running) {
    return <span className="text-xs text-brand-600">Sincronitzant…</span>
  }

  const dot = {
    never: 'bg-bad',
    fresh: 'bg-ok',
    stale: 'bg-warn',
    very_stale: 'bg-bad',
  }[freshness]

  const text = lastOkAt === null ? 'Mai sincronitzat' : `Actualitzat ${relativeCa(now - lastOkAt)}`

  return (
    <span
      className="flex items-center gap-1.5 text-xs text-slate-500"
      title="Estat del mirall de Firestore"
    >
      <span className={`inline-block size-2 rounded-full ${dot}`} />
      {text}
    </span>
  )
}
