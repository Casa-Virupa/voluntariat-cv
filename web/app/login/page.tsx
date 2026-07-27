import { signIn } from '@/lib/auth'

const MESSAGES: Record<string, string> = {
  denied: 'Aquest compte no té accés al panell. Demana a coordinació que t’hi afegeixi.',
  forbidden: 'Aquesta secció només és accessible per a l’equip de coordinació.',
  AccessDenied: 'Aquest compte no té accés al panell. Demana a coordinació que t’hi afegeixi.',
  Configuration: 'El panell no està ben configurat. Avisa la persona que l’administra.',
}

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ error?: string; from?: string }>
}) {
  const { error, from } = await searchParams
  const message = error ? (MESSAGES[error] ?? 'No s’ha pogut iniciar la sessió.') : null

  return (
    <main className="flex min-h-screen items-center justify-center p-6">
      <div className="w-full max-w-sm rounded-2xl bg-surface p-8 shadow-sm ring-1 ring-line">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-4 flex size-12 items-center justify-center rounded-xl bg-brand-500 text-lg font-semibold text-ink-strong">
            CV
          </div>
          <h1 className="text-2xl font-medium text-brand-900">Voluntariat Casa Virupa</h1>
          <p className="mt-1 text-sm text-ink-soft">Panell de coordinació</p>
        </div>

        {message && (
          <p className="mb-6 rounded-lg bg-bad/5 px-4 py-3 text-sm text-bad-ink ring-1 ring-bad/15">
            {message}
          </p>
        )}

        <form
          action={async () => {
            'use server'
            // `from` comes from the URL, so only same-site relative paths are honoured —
            // otherwise this would be an open redirect.
            const target = from?.startsWith('/') && !from.startsWith('//') ? from : '/calendari'
            await signIn('google', { redirectTo: target })
          }}
        >
          <button
            type="submit"
            className="flex w-full items-center justify-center gap-3 rounded-lg bg-brand-700 px-4 py-3 text-sm font-medium uppercase tracking-[0.08em] text-white transition hover:bg-brand-600"
          >
            Entra amb Google
          </button>
        </form>

        <p className="mt-6 text-center text-xs text-ink-faint">
          Només els comptes autoritzats hi poden accedir.
        </p>
      </div>
    </main>
  )
}
