'use client'

import { useRouter } from 'next/navigation'
import { useState, useTransition } from 'react'

/**
 * "Sincronitza ara". Posts to /api/sync with the session cookie — the route accepts either
 * that or the cron shared key, and re-checks the allowlist itself.
 *
 * A full resync is a separate, coordinator-only call because it re-reads every booking ever
 * made; it is the only way a deletion older than the sync window ever propagates.
 */
export function SyncButton({ full = false, label }: { full?: boolean; label?: string }) {
  const router = useRouter()
  const [pending, startTransition] = useTransition()
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [failed, setFailed] = useState(false)

  async function run() {
    if (full && !confirm('Vols rellegir TOTES les reserves des de l’inici? Pot trigar uns minuts.')) {
      return
    }
    setBusy(true)
    setMessage(null)
    setFailed(false)
    try {
      const response = await fetch(`/api/sync${full ? '?full=1' : ''}`, { method: 'POST' })
      const body = (await response.json()) as Record<string, unknown>

      if (!response.ok) {
        setFailed(true)
        setMessage(typeof body.error === 'string' ? body.error : `Error ${response.status}`)
        return
      }
      setMessage(
        `${body.readBookings ?? 0} reserves llegides, ${body.insertedBookings ?? 0} noves o modificades` +
          (Number(body.deletedBookings ?? 0) > 0 ? `, ${body.deletedBookings} esborrades` : ''),
      )
      startTransition(() => router.refresh())
    } catch (error) {
      setFailed(true)
      setMessage(error instanceof Error ? error.message : 'No s’ha pogut sincronitzar')
    } finally {
      setBusy(false)
    }
  }

  const working = busy || pending

  return (
    <span className="inline-flex flex-wrap items-center gap-2">
      <button
        type="button"
        onClick={run}
        disabled={working}
        className={
          full
            ? 'rounded-lg px-3 py-1.5 text-xs font-medium text-ink-soft ring-1 ring-line transition hover:bg-canvas disabled:opacity-50'
            : 'rounded-lg bg-brand-700 px-3 py-1.5 text-xs font-medium uppercase tracking-[0.08em] text-white transition hover:bg-brand-600 disabled:opacity-50'
        }
      >
        {working ? 'Sincronitzant…' : (label ?? 'Sincronitza ara')}
      </button>
      {message && (
        <span className={`text-xs ${failed ? 'text-bad' : 'text-ink-soft'}`}>{message}</span>
      )}
    </span>
  )
}
