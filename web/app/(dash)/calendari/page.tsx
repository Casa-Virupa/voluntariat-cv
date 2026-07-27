/**
 * View 1 — the month grid.
 *
 * Reads only the Firestore mirror: no prices, no commitments, no ledger. Every filter lives
 * in the URL (see lib/query/filters.ts), so this page is a pure function of its query string
 * and the day-detail panel is just another parameter.
 *
 * The area scope of an area_responsible is applied in the SQL by `resolveAreaFilter`, not by
 * hiding rows — someone editing the URL must get nothing back rather than a filtered page.
 */

import Link from 'next/link'

import { requireAdmin, resolveAreaFilter, visibleAreas } from '@/lib/authz'
import { areaLabel, AREA_GENERAL, AREA_UNKNOWN, SHIFT_SLOT_LABEL, VOLUNTEER_TYPE_LABEL } from '@/lib/contract'
import { formatMinutes, todayInMadrid } from '@/lib/dates'
import { affectsNumbers, describeAnomalies } from '@/lib/sync/anomalies'
import {
  areasInPeriod,
  dayDetail,
  formatRange,
  monthGrid,
  monthSummary,
  monthWeeks,
  WEEKDAYS_CA,
} from '@/lib/query/calendar'
import { calendarQuery, parseCalendarSearch, shiftPeriod, type RawSearch } from '@/lib/query/filters'

import { AutoSubmitForm } from '../_components/AutoSubmitForm'
import { Badge, Card, EmptyState, Notice, SidePanel, Tile } from '../_components/ui'

export const dynamic = 'force-dynamic'

export default async function CalendariPage({
  searchParams,
}: {
  searchParams: Promise<RawSearch>
}) {
  const admin = await requireAdmin()
  const search = await searchParams
  const today = todayInMadrid()

  const filters = parseCalendarSearch(search, today)

  // Two different area lists, and confusing them is the bug worth avoiding: `scope` is what
  // this admin is allowed to see, `queryAreas` is that narrowed by their chosen filter.
  const scope = visibleAreas(admin)
  const queryAreas = resolveAreaFilter(admin, filters.area)
  const myVolunteerAreas = admin.seesAllAreas ? [] : admin.areas

  const cells = monthGrid(filters, queryAreas, myVolunteerAreas)
  const summary = monthSummary(filters, queryAreas, myVolunteerAreas)
  const available = areasInPeriod(filters, scope)
  const weeks = monthWeeks(filters.period.year, filters.period.index)

  const detail = filters.day ? dayDetail(filters.day, filters, queryAreas, myVolunteerAreas) : []

  const prev = shiftPeriod(filters.period, -1)
  const next = shiftPeriod(filters.period, 1)
  const href = (over: Partial<Record<string, string | null>>) =>
    `/calendari${calendarQuery(filters, over)}`

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-lg font-semibold text-brand-900">Calendari de voluntariat</h1>
          <p className="mt-0.5 text-xs text-slate-500">
            Hores reals (fi − inici de cada torn), no les 4 h fixes que mostra el perfil de l’app.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <div className="flex items-center overflow-hidden rounded-lg ring-1 ring-line">
            <Link
              href={href({ y: String(prev.year), m: String(prev.index), dia: null })}
              aria-label="Mes anterior"
              className="bg-surface px-2.5 py-1.5 text-sm text-slate-500 transition hover:bg-canvas hover:text-brand-700"
            >
              ‹
            </Link>
            <span className="min-w-[9rem] bg-surface px-3 py-1.5 text-center text-sm font-semibold text-brand-900">
              {filters.period.label}
            </span>
            <Link
              href={href({ y: String(next.year), m: String(next.index), dia: null })}
              aria-label="Mes següent"
              className="bg-surface px-2.5 py-1.5 text-sm text-slate-500 transition hover:bg-canvas hover:text-brand-700"
            >
              ›
            </Link>
          </div>
          <Link
            href={href({ y: today.slice(0, 4), m: String(Number(today.slice(5, 7))), dia: null })}
            className="rounded-lg bg-surface px-2.5 py-1.5 text-xs text-slate-500 ring-1 ring-line transition hover:text-brand-700"
          >
            Avui
          </Link>
        </div>
      </div>

      <FilterBar filters={filters} available={available} canFilterOwn={!admin.seesAllAreas} />

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <Tile label="Hores reals" value={formatMinutes(summary.minutes)} tone="brand" />
        <Tile label="Persones" value={String(summary.people)} hint="voluntaris diferents" />
        <Tile label="Dies amb activitat" value={String(summary.activeDays)} />
        <Tile
          label="Reserves"
          value={String(summary.bookings)}
          hint={summary.anomalies > 0 ? `${summary.anomalies} amb avisos` : undefined}
          tone={summary.anomalies > 0 ? 'warn' : 'plain'}
        />
      </div>

      {!admin.seesAllAreas && (
        <Notice>
          Veus només les teves àrees: {admin.areas.map(areaLabel).join(', ') || 'cap assignada'}. Els
          torns de voluntariat general no s’hi inclouen.
        </Notice>
      )}

      <div className="flex flex-col gap-4 lg:flex-row">
        <Card className="min-w-0 flex-1 self-start overflow-hidden">
          {summary.bookings === 0 ? (
            <EmptyState title="Cap reserva en aquest mes">
              Si esperaves veure’n, comprova que la sincronització amb Firebase s’hagi executat
              correctament i que els filtres no siguin massa restrictius.
            </EmptyState>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[46rem] table-fixed border-collapse">
                <thead>
                  <tr>
                    {WEEKDAYS_CA.map((d) => (
                      <th
                        key={d}
                        className="border-b border-line px-2 py-2 text-[11px] font-medium uppercase tracking-wide text-slate-400"
                      >
                        {d}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {weeks.map((week, i) => (
                    <tr key={i} className="align-top">
                      {week.map((date, j) => (
                        <td
                          key={j}
                          className="h-28 w-[14.28%] border-b border-r border-line p-0 last:border-r-0"
                        >
                          {date && (
                            <DayCellView
                              date={date}
                              today={today}
                              selected={filters.day === date}
                              cell={cells.get(date)}
                              href={href({ dia: filters.day === date ? null : date })}
                            />
                          )}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>

        {filters.day && (
          <SidePanel
            title={formatDayCa(filters.day)}
            subtitle={
              detail.length === 0
                ? 'Cap torn'
                : `${detail.length} torn${detail.length === 1 ? '' : 's'} · ${formatMinutes(
                    detail.reduce((sum, s) => sum + s.minutes, 0),
                  )}`
            }
            closeHref={href({ dia: null })}
          >
            <DayPanel shifts={detail} />
          </SidePanel>
        )}
      </div>
    </div>
  )
}

// --- the grid ---------------------------------------------------------------

function DayCellView({
  date,
  today,
  selected,
  cell,
  href,
}: {
  date: string
  today: string
  selected: boolean
  cell:
    | {
        minutes: number
        people: number
        persons: Array<{ uid: string; name: string; minutes: number; anomalies: number }>
      }
    | undefined
  href: string
}) {
  const dayNumber = Number(date.slice(8, 10))
  const isToday = date === today

  return (
    <Link
      href={href}
      scroll={false}
      className={`flex h-full flex-col gap-1 p-1.5 transition ${
        selected ? 'bg-brand-50 ring-1 ring-inset ring-brand-200' : 'hover:bg-canvas'
      }`}
    >
      <div className="flex items-center gap-1.5">
        <span
          className={`flex size-5 items-center justify-center rounded-full text-[11px] font-semibold ${
            isToday ? 'bg-brand-500 text-white' : 'text-slate-500'
          }`}
        >
          {dayNumber}
        </span>
        {cell && (
          <>
            <span className="ml-auto text-[11px] font-semibold tabular-nums text-brand-700">
              {formatMinutes(cell.minutes)}
            </span>
            <span className="text-[11px] tabular-nums text-slate-400">· {cell.people}p</span>
          </>
        )}
      </div>

      {cell && (
        <ul className="min-h-0 flex-1 space-y-0.5 overflow-hidden">
          {cell.persons.slice(0, 3).map((p) => (
            <li key={p.uid} className="truncate text-[11px] leading-tight text-slate-600">
              {affectsNumbers(p.anomalies) && <span className="text-warn">⚠ </span>}
              {firstName(p.name)}{' '}
              <span className="tabular-nums text-slate-400">({formatMinutes(p.minutes)})</span>
            </li>
          ))}
          {cell.persons.length > 3 && (
            <li className="text-[11px] leading-tight text-slate-400">
              +{cell.persons.length - 3} més
            </li>
          )}
        </ul>
      )}
    </Link>
  )
}

/** Grid cells are narrow; a full name wraps badly and the panel has the full one anyway. */
function firstName(name: string): string {
  const [first] = name.split(/\s+/)
  return first || name
}

// --- the day panel ----------------------------------------------------------

function DayPanel({
  shifts,
}: {
  shifts: Array<{
    docId: string
    uid: string
    name: string
    volunteerType: string | null
    slot: string
    kind: string
    area: string
    startSec: number | null
    endSec: number | null
    minutes: number
    hasLunch: boolean
    hasDinner: boolean
    sleep: boolean
    anomalies: number
  }>
}) {
  if (shifts.length === 0) {
    return <EmptyState title="Cap torn aquest dia" />
  }

  // Grouped by time slot, which is how the day is actually run.
  const grouped = ['morning', 'afternoon', 'unknown']
    .map((slot) => ({ slot, items: shifts.filter((s) => s.slot === slot) }))
    .filter((g) => g.items.length > 0)

  return (
    <div className="divide-y divide-line">
      {grouped.map((group) => (
        <div key={group.slot} className="px-5 py-3">
          <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">
            {SHIFT_SLOT_LABEL[group.slot as keyof typeof SHIFT_SLOT_LABEL] ?? group.slot} ·{' '}
            {formatMinutes(group.items.reduce((s, i) => s + i.minutes, 0))}
          </p>
          <ul className="mt-2 space-y-2.5">
            {group.items.map((s) => (
              <li key={`${s.docId}-${s.slot}-${s.startSec}`} className="text-sm">
                <div className="flex items-baseline gap-2">
                  <span className="font-medium text-slate-800">{s.name}</span>
                  {s.volunteerType && (
                    <span className="text-[11px] text-slate-400">
                      {VOLUNTEER_TYPE_LABEL[s.volunteerType as 'mitra' | 'habitual']}
                    </span>
                  )}
                  <span className="ml-auto tabular-nums text-slate-500">
                    {formatRange(s.startSec, s.endSec)}
                  </span>
                  <span className="w-14 text-right font-medium tabular-nums text-brand-700">
                    {formatMinutes(s.minutes)}
                  </span>
                </div>

                <div className="mt-1 flex flex-wrap items-center gap-1.5">
                  <Badge
                    className={
                      s.area === AREA_GENERAL
                        ? 'bg-brand-50 text-brand-700 ring-brand-200'
                        : s.area === AREA_UNKNOWN
                          ? 'bg-warn/10 text-amber-800 ring-warn/30'
                          : 'bg-slate-100 text-slate-600 ring-slate-200'
                    }
                  >
                    {areaLabel(s.area)}
                  </Badge>
                  {s.hasLunch && <Badge>Dinar</Badge>}
                  {s.hasDinner && <Badge>Sopar</Badge>}
                  {s.sleep && <Badge>Pernocta</Badge>}
                </div>

                {s.anomalies !== 0 && (
                  <p className="mt-1 text-[11px] text-warn">
                    ⚠ {describeAnomalies(s.anomalies).join(' · ')}
                  </p>
                )}
              </li>
            ))}
          </ul>
        </div>
      ))}
    </div>
  )
}

// --- filters ----------------------------------------------------------------

function FilterBar({
  filters,
  available,
  canFilterOwn,
}: {
  filters: ReturnType<typeof parseCalendarSearch>
  available: Array<{ area: string; minutes: number }>
  canFilterOwn: boolean
}) {
  const selectClass =
    'rounded-lg border-0 bg-surface px-2.5 py-1.5 text-xs text-slate-700 ring-1 ring-line focus:ring-2 focus:ring-brand-500'

  return (
    <AutoSubmitForm className="flex flex-wrap items-center gap-3 rounded-2xl bg-surface p-3 ring-1 ring-line">
      {/* The month travels as hidden fields so changing a filter keeps the period. */}
      <input type="hidden" name="y" value={filters.period.year} />
      <input type="hidden" name="m" value={filters.period.index} />

      <label className="flex items-center gap-1.5 text-xs text-slate-500">
        Àrea
        <select name="area" defaultValue={filters.area ?? ''} className={selectClass}>
          <option value="">Totes</option>
          {available.map((a) => (
            <option key={a.area} value={a.area}>
              {areaLabel(a.area)} ({formatMinutes(a.minutes)})
            </option>
          ))}
        </select>
      </label>

      <label className="flex items-center gap-1.5 text-xs text-slate-500">
        Tipus
        <select name="tipus" defaultValue={filters.types.join(',')} className={selectClass}>
          <option value="">Tots</option>
          <option value="mitra">Només mitres</option>
          <option value="habitual">Només habituals</option>
        </select>
      </label>

      <label className="flex items-center gap-1.5 text-xs text-slate-500">
        Torn
        <select name="torn" defaultValue={filters.slot ?? ''} className={selectClass}>
          <option value="">Matí i tarda</option>
          <option value="morning">Matí</option>
          <option value="afternoon">Tarda</option>
        </select>
      </label>

      {canFilterOwn && (
        <label className="flex items-center gap-1.5 text-xs text-slate-500">
          <input
            type="checkbox"
            name="meus"
            value="1"
            defaultChecked={filters.onlyMine}
            className="size-3.5 rounded border-line text-brand-500 focus:ring-brand-500"
          />
          Només els meus voluntaris
        </label>
      )}

      {(filters.area || filters.types.length > 0 || filters.slot || filters.onlyMine) && (
        <Link
          href={`/calendari?y=${filters.period.year}&m=${filters.period.index}`}
          className="ml-auto text-xs text-slate-400 underline-offset-2 hover:text-brand-700 hover:underline"
        >
          Neteja els filtres
        </Link>
      )}
    </AutoSubmitForm>
  )
}

const WEEKDAY_NAMES_CA = [
  'diumenge', 'dilluns', 'dimarts', 'dimecres', 'dijous', 'divendres', 'dissabte',
]
const MONTH_NAMES_CA = [
  'gener', 'febrer', 'març', 'abril', 'maig', 'juny',
  'juliol', 'agost', 'setembre', 'octubre', 'novembre', 'desembre',
]

/** '2026-05-04' -> 'dilluns, 4 de maig'. Built by hand: Intl has no Catalan date skeleton. */
function formatDayCa(date: string): string {
  const [y, m, d] = date.split('-').map(Number)
  const weekday = WEEKDAY_NAMES_CA[new Date(Date.UTC(y, m - 1, d)).getUTCDay()]
  const month = MONTH_NAMES_CA[m - 1]
  const preposition = 'aeiouàèéíòóú'.includes(month[0]) ? 'd’' : 'de '
  return `${weekday}, ${d} ${preposition}${month}`
}
