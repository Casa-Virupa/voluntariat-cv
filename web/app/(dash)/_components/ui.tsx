/**
 * The shared visual vocabulary. Server components — none of these needs interactivity, and
 * keeping them off the client means the whole dashboard ships almost no JavaScript.
 */

import Link from 'next/link'

import { formatMinutes } from '@/lib/dates'
import { STATUS_DOT_CLASS, STATUS_LABEL, type CommitmentStatus } from '@/lib/commitments'

export function Card({
  children,
  className = '',
}: {
  children: React.ReactNode
  className?: string
}) {
  return (
    <section className={`rounded-2xl bg-surface ring-1 ring-line ${className}`}>{children}</section>
  )
}

export function CardHeader({
  title,
  subtitle,
  actions,
}: {
  title: string
  subtitle?: string
  actions?: React.ReactNode
}) {
  return (
    <div className="flex flex-wrap items-center gap-3 border-b border-line px-5 py-3.5">
      <div>
        <h2 className="text-sm font-semibold text-brand-900">{title}</h2>
        {subtitle && <p className="mt-0.5 text-xs text-slate-500">{subtitle}</p>}
      </div>
      {actions && <div className="ml-auto flex items-center gap-2">{actions}</div>}
    </div>
  )
}

export function Tile({
  label,
  value,
  hint,
  tone = 'plain',
}: {
  label: string
  value: string
  hint?: string
  tone?: 'plain' | 'brand' | 'warn' | 'bad'
}) {
  const toneClass = {
    plain: 'text-brand-900',
    brand: 'text-brand-600',
    warn: 'text-warn',
    bad: 'text-bad',
  }[tone]

  return (
    <div className="rounded-xl bg-surface px-4 py-3 ring-1 ring-line">
      <p className="text-[11px] font-medium uppercase tracking-wide text-slate-400">{label}</p>
      <p className={`mt-1 text-xl font-semibold tabular-nums ${toneClass}`}>{value}</p>
      {hint && <p className="mt-0.5 text-xs text-slate-400">{hint}</p>}
    </div>
  )
}

export function Badge({
  children,
  className = 'bg-slate-100 text-slate-600 ring-slate-200',
  title,
}: {
  children: React.ReactNode
  className?: string
  title?: string
}) {
  return (
    <span
      title={title}
      className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-medium ring-1 ring-inset ${className}`}
    >
      {children}
    </span>
  )
}

export function StatusDot({ status }: { status: CommitmentStatus }) {
  return (
    <span
      title={STATUS_LABEL[status]}
      className={`inline-block size-2 shrink-0 rounded-full ${STATUS_DOT_CLASS[status]}`}
    />
  )
}

export function EmptyState({
  title,
  children,
}: {
  title: string
  children?: React.ReactNode
}) {
  return (
    <div className="px-6 py-14 text-center">
      <p className="text-sm font-medium text-slate-600">{title}</p>
      {children && <div className="mx-auto mt-2 max-w-md text-xs text-slate-400">{children}</div>}
    </div>
  )
}

/** Hours everywhere in the UI come through here, labelled "reals" where it matters. */
export function Hours({ minutes, className = '' }: { minutes: number; className?: string }) {
  return <span className={`tabular-nums ${className}`}>{formatMinutes(minutes)}</span>
}

export function PeriodNav({
  label,
  prevHref,
  nextHref,
  todayHref,
  children,
}: {
  label: string
  prevHref: string
  nextHref: string
  todayHref: string
  children?: React.ReactNode
}) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <div className="flex items-center overflow-hidden rounded-lg ring-1 ring-line">
        <Link
          href={prevHref}
          aria-label="Període anterior"
          className="bg-surface px-2.5 py-1.5 text-sm text-slate-500 transition hover:bg-canvas hover:text-brand-700"
        >
          ‹
        </Link>
        <span className="min-w-[9rem] bg-surface px-3 py-1.5 text-center text-sm font-semibold text-brand-900">
          {label}
        </span>
        <Link
          href={nextHref}
          aria-label="Període següent"
          className="bg-surface px-2.5 py-1.5 text-sm text-slate-500 transition hover:bg-canvas hover:text-brand-700"
        >
          ›
        </Link>
      </div>
      <Link
        href={todayHref}
        className="rounded-lg bg-surface px-2.5 py-1.5 text-xs text-slate-500 ring-1 ring-line transition hover:text-brand-700"
      >
        Avui
      </Link>
      {children}
    </div>
  )
}

/**
 * The right-hand detail panel used by both views. Closing it is just a link.
 *
 * `floating` makes it a drawer over the content instead of a column beside it. The
 * coordination table needs that: it can be twenty columns wide, and giving up 26rem of
 * width would push the money columns — the reason anyone opened the row — off screen.
 */
export function SidePanel({
  title,
  subtitle,
  closeHref,
  floating = false,
  children,
}: {
  title: string
  subtitle?: string
  closeHref: string
  floating?: boolean
  children: React.ReactNode
}) {
  if (floating) {
    return (
      <aside className="fixed inset-y-16 right-4 z-30 w-[26rem] max-w-[calc(100vw-2rem)] overflow-y-auto rounded-2xl bg-surface shadow-2xl ring-1 ring-line">
        <PanelHeader title={title} subtitle={subtitle} closeHref={closeHref} />
        {children}
      </aside>
    )
  }

  return (
    <aside className="w-full shrink-0 lg:w-[26rem]">
      <div className="sticky top-4 max-h-[calc(100vh-2rem)] overflow-y-auto rounded-2xl bg-surface ring-1 ring-line">
        <PanelHeader title={title} subtitle={subtitle} closeHref={closeHref} />
        {children}
      </div>
    </aside>
  )
}

function PanelHeader({
  title,
  subtitle,
  closeHref,
}: {
  title: string
  subtitle?: string
  closeHref: string
}) {
  return (
    <div className="sticky top-0 z-10 flex items-start gap-3 border-b border-line bg-surface px-5 py-3.5">
      <div>
        <h2 className="text-sm font-semibold text-brand-900">{title}</h2>
        {subtitle && <p className="mt-0.5 text-xs text-slate-500">{subtitle}</p>}
      </div>
      <Link
        href={closeHref}
        aria-label="Tanca"
        className="ml-auto rounded-md px-2 py-0.5 text-slate-400 transition hover:bg-canvas hover:text-slate-700"
      >
        ✕
      </Link>
    </div>
  )
}

/** A row of label/value pairs, used in both detail panels. */
export function DataRow({
  label,
  children,
}: {
  label: string
  children: React.ReactNode
}) {
  return (
    <div className="flex items-baseline justify-between gap-4 py-1.5 text-sm">
      <span className="text-slate-500">{label}</span>
      <span className="text-right font-medium tabular-nums text-slate-800">{children}</span>
    </div>
  )
}

export function Notice({
  tone = 'info',
  children,
}: {
  tone?: 'info' | 'warn' | 'bad'
  children: React.ReactNode
}) {
  const cls = {
    info: 'bg-brand-50 text-brand-700 ring-brand-200',
    warn: 'bg-warn/10 text-amber-800 ring-warn/30',
    bad: 'bg-bad/10 text-red-800 ring-bad/30',
  }[tone]
  return <div className={`rounded-xl px-4 py-3 text-xs ring-1 ring-inset ${cls}`}>{children}</div>
}
