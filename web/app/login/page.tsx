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
          <div className="mx-auto mb-4 flex size-12 items-center justify-center rounded-xl bg-brand-500 text-lg font-semibold text-white">
            CV
          </div>
          <h1 className="text-xl font-semibold text-brand-900">Voluntariat Casa Virupa</h1>
          <p className="mt-1 text-sm text-slate-500">Panell de coordinació</p>
        </div>

        {message && (
          <p className="mb-6 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700 ring-1 ring-red-100">
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
            className="flex w-full items-center justify-center gap-3 rounded-lg bg-brand-600 px-4 py-3 text-sm font-medium text-white transition hover:bg-brand-700"
          >
            Entra amb Google
          </button>
        </form>

        <p className="mt-6 text-center text-xs text-slate-400">
          Només els comptes autoritzats hi poden accedir.
        </p>
      </div>
    </main>
  )
}
