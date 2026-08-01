/**
 * View 2 — the coordination table. One row per volunteer: hours per area against the
 * commitment resolved for that person and period, then the meal/overnight ledger.
 *
 * What is derived and what is stored matters here. Hours come from the mirror. Charges come
 * from the mirror times the dated price table and are NEVER stored, so a re-sync cannot
 * double-count and a cancelled booking stops being charged. Only the credits — payments and
 * adjustments — are dashboard-owned facts, and they are insert-only.
 *
 * An area_responsible gets the hour columns for their own areas and no money at all.
 */

import Link from 'next/link'

import { requireAdmin, resolveAreaFilter, visibleAreas } from '@/lib/authz'
import { STATUS_LABEL, STATUS_TEXT_CLASS, type Progress } from '@/lib/commitments'
import { areaLabel, AREA_GENERAL, ITEM_LABEL, VOLUNTEER_TYPE_LABEL } from '@/lib/contract'
import { formatCents, formatMinutes, minutesToHours, todayInMadrid } from '@/lib/dates'
import {
  formatSignedCents,
  LEDGER_KIND_LABEL,
  LEDGER_METHOD_LABEL,
  SETTLEMENT_CLASS,
  SETTLEMENT_LABEL,
} from '@/lib/ledger'
import {
  closedFor,
  coordinationTable,
  volunteerDetail,
  type VolunteerRow,
} from '@/lib/query/coordination'
import { coordinationQuery, parseCoordinationSearch, shiftPeriod, type RawSearch } from '@/lib/query/filters'
import { atRiskRatio } from '@/lib/settings'
import { describeAnomalies } from '@/lib/sync/anomalies'

import { AutoSubmitForm } from '../_components/AutoSubmitForm'
import { Badge, Card, DataRow, EmptyState, Notice, SidePanel, StatusDot, Tile } from '../_components/ui'
import { addLedgerEntryAction, closePeriodAction, voidLedgerEntryAction } from './actions'

export const dynamic = 'force-dynamic'

const thClass =
  'sticky top-0 z-10 bg-surface px-3 py-2 text-left text-[11px] font-medium uppercase tracking-[0.08em] text-ink-faint'
const tdClass = 'border-t border-line px-3 py-2 align-middle'
const inputClass =
  'rounded-lg border-0 bg-canvas px-2.5 py-1.5 text-xs text-ink-strong ring-1 ring-line focus:ring-2 focus:ring-brand-500'
const buttonClass =
  'rounded-lg bg-brand-700 px-3 py-1.5 text-xs font-medium uppercase tracking-[0.08em] text-white transition hover:bg-brand-600'

export default async function CoordinacioPage({
  searchParams,
}: {
  searchParams: Promise<RawSearch>
}) {
  const admin = await requireAdmin()
  const search = await searchParams
  const today = todayInMadrid()
  const filters = parseCoordinationSearch(search, today)

  const table = coordinationTable(filters, {
    allowedAreas: visibleAreas(admin),
    queryAreas: resolveAreaFilter(admin, filters.area),
    myVolunteerAreas: admin.seesAllAreas ? [] : admin.areas,
    // Meals, overnights and money are coordination's business, not an area's.
    showPayments: admin.seesAllAreas,
    today,
    atRiskRatio: atRiskRatio(),
  })

  const closed = table.showPayments ? closedFor(filters.period) : new Map()
  const selected = filters.volunteer ? table.rows.find((r) => r.uid === filters.volunteer) : undefined
  const detail = selected ? volunteerDetail(selected.uid, filters.period) : null

  const href = (over: Partial<Record<string, string | null>>) =>
    `/coordinacio${coordinationQuery(filters, over)}`
  const prev = shiftPeriod(filters.period, -1)
  const next = shiftPeriod(filters.period, 1)
  const returnTo = href({})

  const ok = one(search.ok)
  const error = one(search.error)

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-xl font-medium text-brand-900">Coordinació</h1>
          <p className="mt-0.5 text-xs text-ink-soft">
            Hores reals per àrea contra el compromís, i els àpats i pernoctes pendents de pagar.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <div className="flex items-center overflow-hidden rounded-lg ring-1 ring-line">
            <Link
              href={href(periodParams(prev))}
              aria-label="Període anterior"
              className="bg-surface px-2.5 py-1.5 text-sm text-ink-soft transition hover:bg-canvas hover:text-brand-700"
            >
              ‹
            </Link>
            <span className="min-w-[8rem] bg-surface px-3 py-1.5 text-center text-sm font-semibold text-brand-900">
              {filters.period.label}
            </span>
            <Link
              href={href(periodParams(next))}
              aria-label="Període següent"
              className="bg-surface px-2.5 py-1.5 text-sm text-ink-soft transition hover:bg-canvas hover:text-brand-700"
            >
              ›
            </Link>
          </div>

          <div className="flex items-center overflow-hidden rounded-lg text-xs ring-1 ring-line">
            <Link
              href={href({ p: 'month', m: String(Number(today.slice(5, 7))), t: null, y: today.slice(0, 4) })}
              className={`px-2.5 py-1.5 transition ${
                filters.period.kind === 'month'
                  ? 'bg-brand-700 text-white'
                  : 'bg-surface text-ink-soft hover:bg-canvas'
              }`}
            >
              Mes
            </Link>
            <Link
              href={href({
                p: 'quarter',
                t: String(Math.floor((Number(today.slice(5, 7)) + 2) / 3)),
                m: null,
                y: today.slice(0, 4),
              })}
              className={`px-2.5 py-1.5 transition ${
                filters.period.kind === 'quarter'
                  ? 'bg-brand-700 text-white'
                  : 'bg-surface text-ink-soft hover:bg-canvas'
              }`}
            >
              Trimestre
            </Link>
          </div>

          <a
            href={`/api/export${coordinationQuery(filters, { voluntari: null })}`}
            className="rounded-lg bg-surface px-2.5 py-1.5 text-xs text-ink-soft ring-1 ring-line transition hover:text-brand-700"
          >
            Excel
          </a>
        </div>
      </div>

      {ok && <Notice>{ok}</Notice>}
      {error && <Notice tone="bad">{error}</Notice>}

      <FilterBar filters={filters} areaColumns={table.areaColumns} canFilterOwn={!admin.seesAllAreas} />

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <Tile label="Voluntaris" value={String(table.rows.length)} />
        <Tile label="Hores reals" value={formatMinutes(table.totals.totalMinutes)} tone="brand" />
        {table.showPayments && (
          <>
            <Tile
              label="Càrrecs del període"
              value={formatCents(table.totals.chargesCents)}
              hint={`${table.totals.items.get('lunch')?.done ?? 0} dinars · ${
                table.totals.items.get('dinner')?.done ?? 0
              } sopars · ${table.totals.items.get('sleep')?.done ?? 0} pernoctes`}
            />
            <Tile
              label="Pendent de cobrar"
              value={formatCents(table.totals.owedTotalCents)}
              hint="inclou el saldo anterior"
              tone={table.totals.owedTotalCents > 0 ? 'warn' : 'plain'}
            />
          </>
        )}
      </div>

      {!admin.seesAllAreas && (
        <Notice>
          Com a responsable d’àrea veus les hores de {admin.areas.map(areaLabel).join(', ') || 'cap àrea'}.
          Els àpats i els pagaments només els gestiona coordinació.
        </Notice>
      )}

      <div className="flex flex-col gap-4">
        <Card className="min-w-0 flex-1 overflow-hidden">
          {table.rows.length === 0 ? (
            <EmptyState title="Cap voluntari amb activitat en aquest període">
              Prova d’ampliar el període, de treure els filtres o de marcar «mostra els
              voluntaris sense activitat».
            </EmptyState>
          ) : (
            <div className="max-h-[70vh] overflow-auto">
              <table className="w-full border-separate border-spacing-0 text-xs">
                <thead>
                  <tr>
                    <th className={`${thClass} left-0 z-20 min-w-[12rem]`}>Voluntari</th>
                    {table.areaColumns.map((area) => (
                      <th key={area} className={`${thClass} min-w-[6.5rem]`}>
                        {area === AREA_GENERAL ? 'Vol. general' : areaLabel(area)}
                      </th>
                    ))}
                    <th className={`${thClass} min-w-[6.5rem] text-brand-700`}>Total</th>
                    {table.showPayments && (
                      <>
                        <th className={`${thClass} min-w-[7rem]`}>Àpats</th>
                        <th className={`${thClass} min-w-[6rem]`}>Pernoctes</th>
                        <th className={`${thClass} min-w-[6rem] text-right`}>Saldo</th>
                        <th className={`${thClass} min-w-[5rem]`}>Estat</th>
                      </>
                    )}
                  </tr>
                </thead>
                <tbody>
                  {table.rows.map((row) => (
                    <tr
                      key={row.uid}
                      className={`transition hover:bg-canvas ${
                        filters.volunteer === row.uid ? 'bg-brand-100' : ''
                      }`}
                    >
                      <td className={`${tdClass} sticky left-0 z-10 bg-inherit`}>
                        <Link
                          href={href({ voluntari: filters.volunteer === row.uid ? null : row.uid })}
                          scroll={false}
                          className="block"
                        >
                          <span className="font-medium text-ink-strong">{row.name}</span>
                          <span className="ml-1.5 text-[11px] text-ink-faint">
                            {row.volunteerType
                              ? VOLUNTEER_TYPE_LABEL[row.volunteerType as 'mitra' | 'habitual']
                              : '—'}
                            {row.isMember ? ' · soci' : ''}
                          </span>
                          {row.userMissing && (
                            <Badge className="ml-1.5 bg-warn/10 text-warn-ink ring-warn/30">
                              sense fitxa
                            </Badge>
                          )}
                        </Link>
                      </td>

                      {table.areaColumns.map((area) => (
                        <td key={area} className={tdClass}>
                          <ProgressCell progress={row.progressByArea.get(area)} />
                        </td>
                      ))}

                      <td className={`${tdClass} bg-brand-100/50`}>
                        <ProgressCell progress={row.totalProgress} strong />
                      </td>

                      {table.showPayments && (
                        <>
                          <td className={tdClass}>
                            <MealCell row={row} />
                          </td>
                          <td className={tdClass}>
                            <ItemCell counts={row.items.get('sleep')} />
                          </td>
                          <td className={`${tdClass} text-right`}>
                            <span
                              className={`font-medium tabular-nums ${
                                row.balance.owedTotalCents > 0 ? 'text-bad' : 'text-ink-soft'
                              }`}
                            >
                              {formatCents(row.balance.owedTotalCents)}
                            </span>
                            {row.balance.carryInCents !== 0 && (
                              <span className="block text-[10px] text-ink-faint">
                                anterior {formatCents(row.balance.carryInCents)}
                              </span>
                            )}
                          </td>
                          <td className={tdClass}>
                            <Badge className={SETTLEMENT_CLASS[row.settlement]}>
                              {SETTLEMENT_LABEL[row.settlement]}
                            </Badge>
                            {closed.has(row.uid) && (
                              <Badge className="ml-1 bg-canvas text-ink-soft ring-line">
                                tancat
                              </Badge>
                            )}
                          </td>
                        </>
                      )}
                    </tr>
                  ))}
                </tbody>
                <tfoot>
                  <tr className="bg-canvas font-medium">
                    <td className={`${tdClass} sticky left-0 z-10 bg-canvas`}>Total</td>
                    {table.areaColumns.map((area) => (
                      <td key={area} className={`${tdClass} tabular-nums text-ink-soft`}>
                        {formatMinutes(table.totals.minutesByArea.get(area) ?? 0)}
                      </td>
                    ))}
                    <td className={`${tdClass} tabular-nums text-brand-700`}>
                      {formatMinutes(table.totals.totalMinutes)}
                    </td>
                    {table.showPayments && (
                      <>
                        <td className={`${tdClass} tabular-nums text-ink-soft`}>
                          {(table.totals.items.get('lunch')?.done ?? 0) +
                            (table.totals.items.get('dinner')?.done ?? 0)}
                        </td>
                        <td className={`${tdClass} tabular-nums text-ink-soft`}>
                          {table.totals.items.get('sleep')?.done ?? 0}
                        </td>
                        <td className={`${tdClass} text-right tabular-nums text-ink-strong`}>
                          {formatCents(table.totals.owedTotalCents)}
                        </td>
                        <td className={tdClass} />
                      </>
                    )}
                  </tr>
                </tfoot>
              </table>
            </div>
          )}
        </Card>

        {selected && detail && (
          <SidePanel
            title={selected.name}
            subtitle={`${filters.period.label} · ${formatMinutes(selected.totalMinutes)} reals`}
            closeHref={href({ voluntari: null })}
            floating
          >
            <VolunteerPanel
              row={selected}
              detail={detail}
              period={filters.period}
              showPayments={table.showPayments}
              closed={closed.get(selected.uid)}
              today={today}
              returnTo={returnTo}
            />
          </SidePanel>
        )}
      </div>

      <p className="text-[11px] text-ink-faint">
        Les hores són reals (fi − inici de cada torn), com a l’historial de l’app. La pantalla de
        perfil de l’app compta 4 h fixes per torn, així que pot no coincidir. Un compromís marcat
        amb ~ està proratejat des d’un període diferent i mai es marca com a no complert.
      </p>
    </div>
  )
}

function one(v: string | string[] | undefined): string | null {
  if (Array.isArray(v)) return v[v.length - 1] ?? null
  return v ?? null
}

function periodParams(p: { kind: string; year: number; index: number }) {
  return {
    p: p.kind,
    y: String(p.year),
    m: p.kind === 'month' ? String(p.index) : null,
    t: p.kind === 'quarter' ? String(p.index) : null,
  }
}

// --- cells -------------------------------------------------------------------

function ProgressCell({ progress, strong = false }: { progress?: Progress; strong?: boolean }) {
  if (!progress) return <span className="text-ink-ghost">—</span>

  const { doneMinutes, commitment, ratio, status } = progress

  if (!commitment) {
    return (
      <span
        className={`tabular-nums ${strong ? 'font-semibold text-brand-700' : 'text-ink-soft'}`}
        title="Sense compromís definit per aquesta àrea"
      >
        {doneMinutes === 0 ? '—' : formatMinutes(doneMinutes)}
      </span>
    )
  }

  return (
    <span className="flex items-center gap-1.5" title={STATUS_LABEL[status]}>
      <StatusDot status={status} />
      <span className={`tabular-nums ${strong ? 'font-semibold' : ''} ${STATUS_TEXT_CLASS[status]}`}>
        {formatMinutes(doneMinutes)}
      </span>
      <span className="tabular-nums text-ink-faint">
        /{commitment.scaled ? '~' : ''}
        {formatMinutes(commitment.targetMinutes)}
      </span>
      {ratio !== null && (
        <span className="ml-auto text-[10px] tabular-nums text-ink-faint">
          {Math.round(ratio * 100)}%
        </span>
      )}
    </span>
  )
}

function MealCell({ row }: { row: VolunteerRow }) {
  const lunch = row.items.get('lunch')
  const dinner = row.items.get('dinner')
  const done = (lunch?.done ?? 0) + (dinner?.done ?? 0)
  const upcoming = (lunch?.upcoming ?? 0) + (dinner?.upcoming ?? 0)

  if (done === 0 && upcoming === 0) return <span className="text-ink-ghost">—</span>
  return (
    <span className="tabular-nums text-ink" title="Fets · previstos">
      {done}
      {upcoming > 0 && <span className="text-ink-faint"> + {upcoming}</span>}
    </span>
  )
}

function ItemCell({ counts }: { counts?: { done: number; upcoming: number } }) {
  if (!counts || (counts.done === 0 && counts.upcoming === 0)) {
    return <span className="text-ink-ghost">—</span>
  }
  return (
    <span className="tabular-nums text-ink">
      {counts.done}
      {counts.upcoming > 0 && <span className="text-ink-faint"> + {counts.upcoming}</span>}
    </span>
  )
}

// --- the side panel ----------------------------------------------------------

function VolunteerPanel({
  row,
  detail,
  period,
  showPayments,
  closed,
  today,
  returnTo,
}: {
  row: VolunteerRow
  detail: ReturnType<typeof volunteerDetail>
  period: { kind: string; year: number; index: number; label: string; from: string; to: string }
  showPayments: boolean
  closed: { closedAt: number; totalCents: number } | undefined
  today: string
  returnTo: string
}) {
  return (
    <div className="divide-y divide-line text-sm">
      <div className="px-5 py-3">
        <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-ink-faint">Hores</p>
        <div className="mt-1">
          {[...row.progressByArea.entries()].length === 0 && (
            <p className="text-xs text-ink-faint">Cap hora en aquest període.</p>
          )}
          {[...row.progressByArea.entries()].map(([area, p]) => (
            <DataRow key={area} label={area === AREA_GENERAL ? 'Vol. general' : areaLabel(area)}>
              <ProgressCell progress={p} />
            </DataRow>
          ))}
          <DataRow label="Total">
            <ProgressCell progress={row.totalProgress} strong />
          </DataRow>
          <DataRow label="Hores (decimal, per a l’Excel)">
            {minutesToHours(row.totalMinutes)} h
          </DataRow>
        </div>
      </div>

      <div className="px-5 py-3">
        <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-ink-faint">
          Dies ({detail.days.length})
        </p>
        <ul className="mt-1.5 space-y-1">
          {detail.days.map((d) => (
            <li key={d.serviceDate} className="flex items-baseline gap-2 text-xs">
              <span className="tabular-nums text-ink-soft">{d.serviceDate.slice(5)}</span>
              <span className="tabular-nums font-medium text-ink">
                {formatMinutes(d.minutes)}
              </span>
              <span className="truncate text-ink-faint">
                {d.areas.map((a) => (a === AREA_GENERAL ? 'general' : areaLabel(a))).join(', ')}
              </span>
              <span className="ml-auto shrink-0 text-ink-faint">
                {d.hasLunch && 'D'}
                {d.hasDinner && 'S'}
                {d.sleep && 'P'}
              </span>
              {d.anomalies !== 0 && (
                <span className="text-warn" title={describeAnomalies(d.anomalies).join(' · ')}>
                  ⚠
                </span>
              )}
            </li>
          ))}
          {detail.days.length === 0 && <li className="text-xs text-ink-faint">Cap dia.</li>}
        </ul>
      </div>

      {showPayments && (
        <>
          <div className="px-5 py-3">
            <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-ink-faint">
              Àpats i pernoctes
            </p>
            <div className="mt-1">
              {(['lunch', 'dinner', 'sleep'] as const).map((item) => {
                const counts = row.items.get(item)
                if (!counts) return null
                const cents = detail.charges
                  .filter((c) => c.item === item)
                  .reduce((sum, c) => sum + c.amountCents, 0)
                return (
                  <DataRow key={item} label={ITEM_LABEL[item]}>
                    {counts.done}
                    {counts.upcoming > 0 && (
                      <span className="text-ink-faint"> + {counts.upcoming} previstos</span>
                    )}{' '}
                    <span className="text-ink-faint">· {formatCents(cents)}</span>
                  </DataRow>
                )
              })}
              {row.unpricedItems > 0 && (
                <p className="mt-1 text-[11px] text-warn">
                  ⚠ {row.unpricedItems} conceptes sense preu aplicable: compten 0 €.
                </p>
              )}
              <div className="mt-2 border-t border-line pt-2">
                <DataRow label="Càrrecs del període">{formatCents(row.balance.chargesCents)}</DataRow>
                <DataRow label="Saldo anterior">{formatCents(row.balance.carryInCents)}</DataRow>
                {/* Negated and explicitly signed: these three lines have to add up to the
                    deute below them, so a payment must read as a subtraction and a negative
                    adjustment (an extra charge) as an addition. */}
                <DataRow label="Pagaments i ajustos">
                  {formatSignedCents(-row.balance.creditsCents, formatCents)}
                </DataRow>
                <DataRow label="Deute total">
                  <span className={row.balance.owedTotalCents > 0 ? 'text-bad' : 'text-ok'}>
                    {formatCents(row.balance.owedTotalCents)}
                  </span>
                </DataRow>
              </div>
            </div>
          </div>

          <div className="px-5 py-3">
            <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-ink-faint">
              Registra un pagament o un ajust
            </p>
            <form action={addLedgerEntryAction} className="mt-2 space-y-2">
              <input type="hidden" name="uid" value={row.uid} />
              <input type="hidden" name="returnTo" value={returnTo} />

              <div className="flex gap-2">
                <select name="kind" defaultValue="payment" className={`${inputClass} flex-1`}>
                  <option value="payment">Pagament</option>
                  <option value="adjustment">Ajust</option>
                  <option value="write_off">Condonació</option>
                  <option value="opening_balance">Saldo inicial</option>
                </select>
                <select name="method" defaultValue="cash" className={`${inputClass} flex-1`}>
                  <option value="cash">Efectiu</option>
                  <option value="transfer">Transferència</option>
                  <option value="app">Des de l’app</option>
                  <option value="">—</option>
                </select>
              </div>

              <div className="flex gap-2">
                <input
                  name="amount"
                  placeholder="24,00"
                  required
                  className={`${inputClass} w-24`}
                  defaultValue={
                    row.balance.owedTotalCents > 0
                      ? (row.balance.owedTotalCents / 100).toFixed(2).replace('.', ',')
                      : ''
                  }
                />
                <input
                  type="date"
                  name="effectiveDate"
                  defaultValue={today}
                  required
                  className={`${inputClass} flex-1`}
                />
              </div>

              <input name="note" placeholder="Nota (opcional)" className={`${inputClass} w-full`} />

              <p className="text-[11px] text-ink-faint">
                Un import positiu redueix el que deu. Per carregar-li alguna cosa extra, fes servir
                un ajust negatiu (per exemple −5).
              </p>
              <button type="submit" className={buttonClass}>
                Registra
              </button>
            </form>
          </div>

          <div className="px-5 py-3">
            <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-ink-faint">
              Historial de pagaments
            </p>
            <ul className="mt-1.5 space-y-1.5">
              {detail.ledger.length === 0 && (
                <li className="text-xs text-ink-faint">Cap apunt registrat mai.</li>
              )}
              {detail.ledger.map((entry) => (
                <li
                  key={entry.id}
                  className={`text-xs ${entry.voided || entry.voidsId !== null ? 'text-ink-faint' : ''}`}
                >
                  <div className="flex items-baseline gap-2">
                    <span className="tabular-nums">{entry.effectiveDate}</span>
                    <span className="font-medium">
                      {LEDGER_KIND_LABEL[entry.kind] ?? entry.kind}
                    </span>
                    {entry.method && (
                      <span className="text-ink-faint">
                        {LEDGER_METHOD_LABEL[entry.method] ?? entry.method}
                      </span>
                    )}
                    <span
                      className={`ml-auto tabular-nums font-medium ${
                        entry.voided ? 'line-through' : ''
                      }`}
                    >
                      {formatCents(entry.amountCents)}
                    </span>
                  </div>
                  {entry.note && <p className="text-ink-faint">{entry.note}</p>}
                  <div className="flex items-baseline gap-2 text-[10px] text-ink-faint">
                    <span>{entry.createdBy}</span>
                    {entry.voided && <span>· anul·lat</span>}
                    {!entry.voided && entry.voidsId === null && (
                      <form action={voidLedgerEntryAction} className="contents">
                        <input type="hidden" name="id" value={entry.id} />
                        <input type="hidden" name="returnTo" value={returnTo} />
                        <input type="hidden" name="reason" value={`Anul·lat des del panell`} />
                        <button
                          type="submit"
                          className="ml-auto underline-offset-2 hover:text-bad hover:underline"
                        >
                          anul·la
                        </button>
                      </form>
                    )}
                  </div>
                </li>
              ))}
            </ul>
            <p className="mt-2 text-[11px] text-ink-faint">
              Els apunts no s’editen ni s’esborren: anul·lar-ne un afegeix el seu contrari, de
              manera que l’historial sempre explica què va passar.
            </p>
          </div>

          <div className="px-5 py-3">
            <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-ink-faint">
              Tancament del període
            </p>
            {closed ? (
              <p className="mt-1 text-xs text-ink-soft">
                Tancat amb {formatCents(closed.totalCents)} congelats. Un canvi de preu ja no mou
                aquest període.
              </p>
            ) : (
              <form action={closePeriodAction} className="mt-1.5 space-y-2">
                <input type="hidden" name="uid" value={row.uid} />
                <input type="hidden" name="periodKind" value={period.kind} />
                <input type="hidden" name="year" value={period.year} />
                <input type="hidden" name="index" value={period.index} />
                <input type="hidden" name="returnTo" value={returnTo} />
                <p className="text-[11px] text-ink-faint">
                  Congela els càrrecs de {period.label} ({formatCents(row.balance.chargesCents)}) per
                  si més endavant es corregeix un preu. No es pot desfer des d’aquí.
                </p>
                <button
                  type="submit"
                  className="rounded-lg px-3 py-1.5 text-xs text-ink-soft ring-1 ring-line transition hover:bg-canvas"
                >
                  Tanca el període
                </button>
              </form>
            )}
          </div>
        </>
      )}
    </div>
  )
}

// --- filters ----------------------------------------------------------------

function FilterBar({
  filters,
  areaColumns,
  canFilterOwn,
}: {
  filters: ReturnType<typeof parseCoordinationSearch>
  areaColumns: string[]
  canFilterOwn: boolean
}) {
  const selectClass =
    'rounded-lg border-0 bg-surface px-2.5 py-1.5 text-xs text-ink ring-1 ring-line focus:ring-2 focus:ring-brand-500'

  return (
    <AutoSubmitForm className="flex flex-wrap items-center gap-3 rounded-2xl bg-surface p-3 ring-1 ring-line">
      <input type="hidden" name="p" value={filters.period.kind} />
      <input type="hidden" name="y" value={filters.period.year} />
      {filters.period.kind === 'month' ? (
        <input type="hidden" name="m" value={filters.period.index} />
      ) : (
        <input type="hidden" name="t" value={filters.period.index} />
      )}

      <label className="flex items-center gap-1.5 text-xs text-ink-soft">
        Àrea
        <select name="area" defaultValue={filters.area ?? ''} className={selectClass}>
          <option value="">Totes</option>
          {areaColumns.map((a) => (
            <option key={a} value={a}>
              {a === AREA_GENERAL ? 'Vol. general' : areaLabel(a)}
            </option>
          ))}
        </select>
      </label>

      <label className="flex items-center gap-1.5 text-xs text-ink-soft">
        Tipus
        <select name="tipus" defaultValue={filters.types.join(',')} className={selectClass}>
          <option value="">Tots</option>
          <option value="mitra">Només mitres</option>
          <option value="habitual">Només habituals</option>
        </select>
      </label>

      <label className="flex items-center gap-1.5 text-xs text-ink-soft">
        <input
          type="checkbox"
          name="tots"
          value="1"
          defaultChecked={filters.showEmpty}
          className="size-3.5 rounded border-line text-brand-500 focus:ring-brand-500"
        />
        Mostra els voluntaris sense activitat
      </label>

      {canFilterOwn && (
        <label
          className="flex items-center gap-1.5 text-xs text-ink-soft"
          title="Voluntaris apuntats a alguna de les teves àrees o amb un compromís en alguna d’elles."
        >
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
    </AutoSubmitForm>
  )
}
