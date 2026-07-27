'use client'

/**
 * The only reason this file is a client component: filters should apply when you change a
 * dropdown, without a "Filtra" button.
 *
 * It is a plain `method="get"` form, so the filter state ends up in the URL — which is what
 * makes every filtered view bookmarkable, shareable and exportable (the export route reads
 * the same query string). With JavaScript disabled the submit button still works.
 */

export function AutoSubmitForm({
  children,
  className = '',
}: {
  children: React.ReactNode
  className?: string
}) {
  return (
    <form
      method="get"
      className={className}
      onChange={(event) => {
        // Text inputs would submit on every keystroke; only react to the controls whose
        // change event means "I have chosen something".
        const target = event.target as HTMLElement
        if (target instanceof HTMLSelectElement || target instanceof HTMLInputElement) {
          if (target.type === 'text' || target.type === 'number' || target.type === 'search') return
          event.currentTarget.requestSubmit()
        }
      }}
    >
      {children}
      <noscript>
        <button
          type="submit"
          className="rounded-lg bg-brand-700 px-3 py-1.5 text-xs font-medium uppercase tracking-[0.08em] text-white"
        >
          Filtra
        </button>
      </noscript>
    </form>
  )
}
