/**
 * Coordinator-only configuration. Six sections behind one query parameter, because the
 * page is long and a coordinator only ever comes here for one of them.
 *
 * This page has to exist before /coordinacio is useful: the commitment columns read the
 * rules created here, and it is the only way to correct a price or restore an access.
 */

import Link from 'next/link'

import { requireCoordinator } from '@/lib/authz'
import {
  AREA_CODES,
  AREA_GENERAL,
  AREA_TOTAL,
  areaLabel,
  CHARGEABLE_ITEMS,
  ITEM_LABEL,
  VOLUNTEER_TYPE_LABEL,
  VOLUNTEER_TYPES,
} from '@/lib/contract'
import { formatCents, formatMinutes, monthPeriod, todayInMadrid } from '@/lib/dates'
import {
  admins,
  auditPage,
  commitmentRules,
  dataQuality,
  priceRules,
  recentSyncRuns,
  volunteerOptions,
  type AuditPage,
} from '@/lib/query/config'
import {
  atRiskRatio,
  AUDIT_ACTION_LABEL,
  AUDIT_ENTITY_LABEL,
  getSetting,
  writebackEnabled,
} from '@/lib/settings'
import { readLinks, type LinksConfig } from '@/lib/links'
import { planWriteback, recentWritebacks } from '@/lib/writeback'
import type { RawSearch } from '@/lib/query/filters'

import { AutoSubmitForm } from '../_components/AutoSubmitForm'
import { SyncButton } from '../_components/SyncButton'
import { Badge, Card, CardHeader, EmptyState, Notice } from '../_components/ui'
import {
  addCommitmentAction,
  addPriceAction,
  endCommitmentAction,
  endPriceAction,
  runWritebackAction,
  saveLinksAction,
  saveAdminAction,
  saveSettingsAction,
  setWritebackAction,
  toggleAdminAction,
} from './actions'

export const dynamic = 'force-dynamic'

const SECTIONS = [
  { key: 'preus', label: 'Preus' },
  { key: 'compromisos', label: 'Compromisos' },
  { key: 'accessos', label: 'Accessos' },
  { key: 'enllacos', label: 'Enllaços' },
  { key: 'sync', label: 'Sincronització' },
  { key: 'dades', label: 'Qualitat de dades' },
  { key: 'firebase', label: 'Escriptura a Firebase' },
] as const

const inputClass =
  'rounded-lg border-0 bg-canvas px-2.5 py-1.5 text-xs text-ink-strong ring-1 ring-line focus:ring-2 focus:ring-brand-500'
const buttonClass =
  'rounded-lg bg-brand-700 px-3 py-1.5 text-xs font-medium uppercase tracking-[0.08em] text-white transition hover:bg-brand-600'
const ghostButtonClass =
  'rounded-lg px-2 py-1 text-xs text-ink-soft ring-1 ring-line transition hover:bg-canvas hover:text-ink-strong'
const thClass =
  'border-b border-line px-3 py-2 text-left text-[11px] font-medium uppercase tracking-[0.08em] text-ink-faint'
const tdClass = 'border-b border-line px-3 py-2 text-ink'

export default async function ConfiguracioPage({
  searchParams,
}: {
  searchParams: Promise<RawSearch>
}) {
  await requireCoordinator()
  const search = await searchParams
  const section = pickSection(one(search.seccio))
  const ok = one(search.ok)
  const error = one(search.error)
  const today = todayInMadrid()

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-xl font-medium text-brand-900">Configuració</h1>
        <p className="mt-0.5 text-xs text-ink-soft">
          Preus, compromisos i accessos són propietat del panell: no existeixen a l’app i
          sobreviuen a qualsevol resincronització.
        </p>
      </div>

      <nav className="flex flex-wrap gap-1 rounded-2xl bg-surface p-1.5 ring-1 ring-line">
        {SECTIONS.map((s) => (
          <Link
            key={s.key}
            href={`/configuracio?seccio=${s.key}`}
            className={`rounded-lg px-3 py-1.5 text-xs font-medium transition ${
              s.key === section
                ? 'bg-brand-700 text-white'
                : 'text-ink-soft hover:bg-canvas hover:text-brand-700'
            }`}
          >
            {s.label}
          </Link>
        ))}
      </nav>

      {ok && <Notice>{ok}</Notice>}
      {error && <Notice tone="bad">{error}</Notice>}

      {section === 'preus' && <PricesSection today={today} />}
      {section === 'compromisos' && <CommitmentsSection today={today} />}
      {section === 'accessos' && <AdminsSection />}
      {section === 'enllacos' && <LinksSection />}
      {section === 'sync' && <SyncSection auditPageNumber={parsePageNumber(one(search.canvis))} />}
      {section === 'dades' && <DataQualitySection />}
      {section === 'firebase' && (
        <FirebaseSection today={today} month={parseMonth(one(search.mes), today)} />
      )}
    </div>
  )
}

function one(v: string | string[] | undefined): string | null {
  if (Array.isArray(v)) return v[v.length - 1] ?? null
  return v ?? null
}

function pickSection(value: string | null): (typeof SECTIONS)[number]['key'] {
  const found = SECTIONS.find((s) => s.key === value)
  return found?.key ?? 'preus'
}

/** Out-of-range or hand-edited page numbers fall back to the first page; auditPage() clamps the top. */
function parsePageNumber(value: string | null): number {
  const n = Number(value)
  return Number.isInteger(n) && n >= 1 ? n : 1
}

/**
 * Write-back is always about ONE month — the app's payment documents are keyed on
 * (user, year, month), and rule 2 says only that month's charges may ever be written.
 */
function parseMonth(value: string | null, today: string): { year: number; month: number } {
  const m = value && /^\d{4}-\d{2}$/.test(value) ? value : today.slice(0, 7)
  const year = Number(m.slice(0, 4))
  const month = Number(m.slice(5, 7))
  if (year < 2000 || year > 2100 || month < 1 || month > 12) {
    return { year: Number(today.slice(0, 4)), month: Number(today.slice(5, 7)) }
  }
  return { year, month }
}

// --- prices ------------------------------------------------------------------

async function PricesSection({ today }: { today: string }) {
  const rules = priceRules()

  return (
    <div className="space-y-4">
      <Notice>
        Un preu no s’edita mai: se’n crea un de nou amb una data d’inici i el vigent es tanca
        aquell mateix dia. Així cap càrrec ja calculat canvia retroactivament — les reserves de
        maig conserven el preu de maig.
      </Notice>

      <Card>
        <CardHeader
          title="Preus vigents i històrics"
          subtitle="El més específic guanya: (concepte + tipus + soci) per damunt de (concepte + tipus), per damunt de (concepte)."
        />
        <div className="overflow-x-auto">
          <table className="w-full text-xs">
            <thead>
              <tr>
                <th className={thClass}>Concepte</th>
                <th className={thClass}>Tipus</th>
                <th className={thClass}>Soci</th>
                <th className={`${thClass} text-right`}>Preu</th>
                <th className={thClass}>Vigent des de</th>
                <th className={thClass}>Fins a</th>
                <th className={thClass}>Nota</th>
                <th className={thClass} />
              </tr>
            </thead>
            <tbody>
              {rules.map((r) => {
                const current = r.validFrom <= today && (r.validTo === null || today < r.validTo)
                return (
                  <tr key={r.id} className={current ? '' : 'text-ink-faint'}>
                    <td className={`${tdClass} font-medium`}>
                      {ITEM_LABEL[r.item as keyof typeof ITEM_LABEL] ?? r.item}
                    </td>
                    <td className={tdClass}>
                      {r.volunteerType
                        ? VOLUNTEER_TYPE_LABEL[r.volunteerType as 'mitra' | 'habitual']
                        : 'Qualsevol'}
                    </td>
                    <td className={tdClass}>
                      {r.isMember === null ? 'Qualsevol' : r.isMember ? 'Sí' : 'No'}
                    </td>
                    <td className={`${tdClass} text-right font-medium tabular-nums`}>
                      {formatCents(r.unitPriceCents)}
                    </td>
                    <td className={`${tdClass} tabular-nums`}>{r.validFrom}</td>
                    <td className={`${tdClass} tabular-nums`}>{r.validTo ?? '—'}</td>
                    <td className={`${tdClass} max-w-[16rem] truncate`} title={r.note ?? ''}>
                      {r.note ?? ''}
                    </td>
                    <td className={tdClass}>
                      {r.validTo === null && (
                        <form action={endPriceAction} className="flex items-center gap-1">
                          <input type="hidden" name="id" value={r.id} />
                          <input
                            type="date"
                            name="validTo"
                            defaultValue={today}
                            required
                            className={inputClass}
                          />
                          <button type="submit" className={ghostButtonClass}>
                            Tanca
                          </button>
                        </form>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </Card>

      <Card>
        <CardHeader title="Afegeix un preu" />
        <form action={addPriceAction} className="flex flex-wrap items-end gap-3 p-5">
          <Field label="Concepte">
            <select name="item" className={inputClass} required>
              {CHARGEABLE_ITEMS.map((i) => (
                <option key={i} value={i}>
                  {ITEM_LABEL[i]}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Tipus de voluntari">
            <select name="volunteerType" className={inputClass} defaultValue="">
              <option value="">Qualsevol</option>
              {VOLUNTEER_TYPES.map((t) => (
                <option key={t} value={t}>
                  {VOLUNTEER_TYPE_LABEL[t]}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Soci">
            <select name="isMember" className={inputClass} defaultValue="">
              <option value="">Qualsevol</option>
              <option value="yes">Sí</option>
              <option value="no">No</option>
            </select>
          </Field>
          <Field label="Preu (€)">
            <input name="price" placeholder="8,00" required className={`${inputClass} w-24`} />
          </Field>
          <Field label="Vigent des de">
            <input type="date" name="validFrom" defaultValue={today} required className={inputClass} />
          </Field>
          <Field label="Nota">
            <input name="note" className={`${inputClass} w-52`} />
          </Field>
          <button type="submit" className={buttonClass}>
            Afegeix
          </button>
        </form>
      </Card>
    </div>
  )
}

// --- commitments -------------------------------------------------------------

async function CommitmentsSection({ today }: { today: string }) {
  const rules = commitmentRules()
  const volunteers = volunteerOptions()

  return (
    <div className="space-y-4">
      <Notice>
        L’app només coneix un total global (60 h/trimestre per a mitres, 8 h/mes per a
        habituals). Els compromisos per àrea només existeixen aquí. El més específic guanya:
        persona &gt; tipus &gt; global, avaluat a l’inici del període.
      </Notice>

      <Card>
        <CardHeader title="Compromisos" subtitle="«Total» vol dir totes les àrees sumades." />
        <div className="overflow-x-auto">
          <table className="w-full text-xs">
            <thead>
              <tr>
                <th className={thClass}>Àmbit</th>
                <th className={thClass}>Qui</th>
                <th className={thClass}>Àrea</th>
                <th className={`${thClass} text-right`}>Objectiu</th>
                <th className={thClass}>Període</th>
                <th className={thClass}>Des de</th>
                <th className={thClass}>Fins a</th>
                <th className={thClass} />
              </tr>
            </thead>
            <tbody>
              {rules.map((r) => {
                const current = r.validFrom <= today && (r.validTo === null || today < r.validTo)
                return (
                  <tr key={r.id} className={current ? '' : 'text-ink-faint'}>
                    <td className={tdClass}>
                      {r.scopeKind === 'global'
                        ? 'Global'
                        : r.scopeKind === 'volunteer_type'
                          ? 'Per tipus'
                          : 'Personal'}
                    </td>
                    <td className={tdClass}>
                      {r.scopeKind === 'volunteer_type'
                        ? VOLUNTEER_TYPE_LABEL[r.scopeValue as 'mitra' | 'habitual']
                        : (r.scopeName ?? r.scopeValue ?? 'Tothom')}
                    </td>
                    <td className={`${tdClass} font-medium`}>{areaLabel(r.area)}</td>
                    <td className={`${tdClass} text-right tabular-nums`}>
                      {formatMinutes(r.targetMinutes)}
                    </td>
                    <td className={tdClass}>{r.periodKind === 'quarter' ? 'Trimestre' : 'Mes'}</td>
                    <td className={`${tdClass} tabular-nums`}>{r.validFrom}</td>
                    <td className={`${tdClass} tabular-nums`}>{r.validTo ?? '—'}</td>
                    <td className={tdClass}>
                      {r.validTo === null && (
                        <form action={endCommitmentAction} className="flex items-center gap-1">
                          <input type="hidden" name="id" value={r.id} />
                          <input
                            type="date"
                            name="validTo"
                            defaultValue={today}
                            required
                            className={inputClass}
                          />
                          <button type="submit" className={ghostButtonClass}>
                            Tanca
                          </button>
                        </form>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </Card>

      <Card>
        <CardHeader
          title="Afegeix un compromís"
          subtitle="Si el període del compromís no coincideix amb el que es consulta, el panell el prorateja i el marca amb ~."
        />
        <form action={addCommitmentAction} className="flex flex-wrap items-end gap-3 p-5">
          <Field label="Àmbit">
            <select name="scopeKind" className={inputClass} defaultValue="volunteer_type">
              <option value="global">Global</option>
              <option value="volunteer_type">Per tipus</option>
              <option value="user">Persona</option>
            </select>
          </Field>
          <Field label="Tipus (si «per tipus»)">
            <select name="volunteerType" className={inputClass} defaultValue="habitual">
              {VOLUNTEER_TYPES.map((t) => (
                <option key={t} value={t}>
                  {VOLUNTEER_TYPE_LABEL[t]}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Persona (si «persona»)">
            <select name="uid" className={inputClass} defaultValue="">
              <option value="">—</option>
              {volunteers.map((v) => (
                <option key={v.uid} value={v.uid}>
                  {v.name}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Àrea">
            <select name="area" className={inputClass} defaultValue={AREA_TOTAL}>
              <option value={AREA_TOTAL}>Total (totes les àrees)</option>
              <option value={AREA_GENERAL}>Voluntariat general</option>
              {AREA_CODES.map((a) => (
                <option key={a} value={a}>
                  {areaLabel(a)}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Hores">
            <input name="hours" placeholder="8" required className={`${inputClass} w-20`} />
          </Field>
          <Field label="Per">
            <select name="periodKind" className={inputClass} defaultValue="month">
              <option value="month">Mes</option>
              <option value="quarter">Trimestre</option>
            </select>
          </Field>
          <Field label="Vigent des de">
            <input type="date" name="validFrom" defaultValue={today} required className={inputClass} />
          </Field>
          <Field label="Nota">
            <input name="note" className={`${inputClass} w-44`} />
          </Field>
          <button type="submit" className={buttonClass}>
            Afegeix
          </button>
        </form>
      </Card>
    </div>
  )
}

// --- the allowlist -----------------------------------------------------------

async function AdminsSection() {
  const rows = admins()

  return (
    <div className="space-y-4">
      <Notice>
        Aquesta llista <strong>és</strong> el control d’accés: es torna a llegir a cada petició,
        de manera que retirar un accés té efecte immediat. Un responsable d’àrea només veu les
        seves àrees, i això s’aplica a la consulta SQL, no amagant columnes.
      </Notice>

      <Card>
        <CardHeader title="Qui pot entrar" />
        <div className="overflow-x-auto">
          <table className="w-full text-xs">
            <thead>
              <tr>
                <th className={thClass}>Correu</th>
                <th className={thClass}>Nom</th>
                <th className={thClass}>Rol</th>
                <th className={thClass}>Àrees</th>
                <th className={thClass}>Estat</th>
                <th className={thClass} />
              </tr>
            </thead>
            <tbody>
              {rows.map((a) => (
                <tr key={a.email} className={a.disabledAt ? 'text-ink-faint' : ''}>
                  <td className={`${tdClass} font-medium`}>{a.email}</td>
                  <td className={tdClass}>{a.displayName ?? '—'}</td>
                  <td className={tdClass}>
                    {a.role === 'coordinator' ? 'Coordinació' : 'Responsable d’àrea'}
                  </td>
                  <td className={tdClass}>
                    {a.role === 'coordinator' ? (
                      <span className="text-ink-faint">totes</span>
                    ) : (
                      a.areas.map(areaLabel).join(', ') || '—'
                    )}
                  </td>
                  <td className={tdClass}>
                    {a.disabledAt ? (
                      <Badge className="bg-canvas text-ink-soft ring-line">Retirat</Badge>
                    ) : (
                      <Badge className="bg-ok/10 text-ok ring-ok/20">Actiu</Badge>
                    )}
                  </td>
                  <td className={tdClass}>
                    <form action={toggleAdminAction}>
                      <input type="hidden" name="email" value={a.email} />
                      <input type="hidden" name="disable" value={a.disabledAt ? '0' : '1'} />
                      <button type="submit" className={ghostButtonClass}>
                        {a.disabledAt ? 'Restableix' : 'Retira'}
                      </button>
                    </form>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <Card>
        <CardHeader
          title="Afegeix o modifica un accés"
          subtitle="Amb el mateix correu es modifica l’existent. Les àrees només s’apliquen als responsables d’àrea."
        />
        <form action={saveAdminAction} className="space-y-4 p-5">
          <div className="flex flex-wrap items-end gap-3">
            <Field label="Correu de Google">
              <input
                name="email"
                type="email"
                required
                placeholder="nom@example.cat"
                className={`${inputClass} w-64`}
              />
            </Field>
            <Field label="Nom">
              <input name="displayName" className={`${inputClass} w-44`} />
            </Field>
            <Field label="Rol">
              <select name="role" className={inputClass} defaultValue="area_responsible">
                <option value="area_responsible">Responsable d’àrea</option>
                <option value="coordinator">Coordinació</option>
              </select>
            </Field>
            <button type="submit" className={buttonClass}>
              Guarda
            </button>
          </div>

          <fieldset>
            <legend className="text-[11px] font-medium uppercase tracking-[0.08em] text-ink-faint">
              Àrees
            </legend>
            <div className="mt-2 grid grid-cols-2 gap-x-4 gap-y-1 sm:grid-cols-3 lg:grid-cols-4">
              {AREA_CODES.map((a) => (
                <label key={a} className="flex items-center gap-1.5 text-xs text-ink-soft">
                  <input
                    type="checkbox"
                    name="areas"
                    value={a}
                    className="size-3.5 rounded border-line text-brand-500 focus:ring-brand-500"
                  />
                  {areaLabel(a)}
                </label>
              ))}
            </div>
          </fieldset>
        </form>
      </Card>
    </div>
  )
}

// --- the app's links text ------------------------------------------------------

/**
 * Edits Firestore directly (configuration/links), not the SQLite mirror: this text is
 * app content, and saving it is live for every volunteer. Hence the read may fail when
 * Firebase credentials are absent (local dev on seed data) — that becomes a notice, not
 * an error page, because the rest of /configuracio works fine without Firestore.
 */
async function LinksSection() {
  let links: LinksConfig | null = null
  let readError: string | null = null
  try {
    links = await readLinks()
  } catch (error) {
    readError = error instanceof Error ? error.message : String(error)
  }

  return (
    <div className="space-y-4">
      <Notice>
        Aquest text és el que l’app mostra a «Enllaços d’interès», al perfil. S’escriu
        directament a Firebase (<code>configuration/links</code>) i els voluntaris el veuen en
        obrir l’app — no cal cap sincronització. Les URL es tornen enllaços clicables a l’app.
      </Notice>

      {readError ? (
        <Notice tone="bad">
          No s’ha pogut llegir <code>configuration/links</code> de Firebase: {readError}
        </Notice>
      ) : (
        <Card>
          <CardHeader
            title="Enllaços d’interès"
            subtitle={
              links?.updatedAt
                ? `Darrera actualització: ${formatIsoInstant(links.updatedAt)}`
                : 'Encara no s’ha guardat mai cap text.'
            }
          />
          <form action={saveLinksAction} className="space-y-3 p-5">
            <textarea
              name="text"
              rows={10}
              defaultValue={links?.text ?? ''}
              placeholder={'Calendari d’activitats:\nhttps://…'}
              className={`${inputClass} w-full font-mono leading-relaxed`}
            />
            <button type="submit" className={buttonClass}>
              Guarda i publica a l’app
            </button>
          </form>
        </Card>
      )}
    </div>
  )
}

/** The doc's updated_at is an ISO string, unlike the epoch seconds everywhere else. */
function formatIsoInstant(iso: string): string {
  const ms = Date.parse(iso)
  return Number.isFinite(ms) ? formatInstant(Math.floor(ms / 1000)) : iso
}

// --- sync --------------------------------------------------------------------

async function SyncSection({ auditPageNumber }: { auditPageNumber: number }) {
  const runs = recentSyncRuns()
  const audit = auditPage(auditPageNumber)
  const ratio = atRiskRatio()
  const quarters = getSetting<number>('sync_history_quarters', 1)

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader
          title="Sincronització"
          subtitle="El cron del servidor la llança a les 00:10 i a les 12:10. Una resincronització completa rellegeix tot l’històric — és l’única manera que arribi l’esborrat d’una reserva antiga."
          actions={
            <>
              <SyncButton />
              <SyncButton full label="Resincronització completa" />
            </>
          }
        />
        <div className="overflow-x-auto">
          <table className="w-full text-xs">
            <thead>
              <tr>
                <th className={thClass}>Quan</th>
                <th className={thClass}>Mode</th>
                <th className={thClass}>Origen</th>
                <th className={thClass}>Estat</th>
                <th className={`${thClass} text-right`}>Llegides</th>
                <th className={`${thClass} text-right`}>Noves</th>
                <th className={`${thClass} text-right`}>Esborrades</th>
                <th className={thClass}>Finestra</th>
                <th className={thClass}>Error</th>
              </tr>
            </thead>
            <tbody>
              {runs.length === 0 && (
                <tr>
                  <td className={tdClass} colSpan={9}>
                    Encara no s’ha executat cap sincronització.
                  </td>
                </tr>
              )}
              {runs.map((r) => (
                <tr key={r.id}>
                  <td className={`${tdClass} tabular-nums`}>{formatInstant(r.startedAt)}</td>
                  <td className={tdClass}>{r.mode === 'full' ? 'Completa' : 'Finestra'}</td>
                  <td className={tdClass}>{r.triggeredBy ?? r.trigger}</td>
                  <td className={tdClass}>
                    <Badge
                      className={
                        r.status === 'ok'
                          ? 'bg-ok/10 text-ok ring-ok/20'
                          : r.status === 'running'
                            ? 'bg-brand-100 text-brand-700 ring-brand-200'
                            : 'bg-bad/10 text-bad ring-bad/20'
                      }
                    >
                      {r.status}
                    </Badge>
                  </td>
                  <td className={`${tdClass} text-right tabular-nums`}>{r.readBookings}</td>
                  <td className={`${tdClass} text-right tabular-nums`}>{r.insertedBookings}</td>
                  <td className={`${tdClass} text-right tabular-nums`}>{r.deletedBookings}</td>
                  <td className={`${tdClass} tabular-nums`}>{r.windowFrom ?? 'tot'}</td>
                  <td className={`${tdClass} max-w-[22rem] text-bad`}>{r.errorMessage ?? ''}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <Card>
        <CardHeader title="Preferències" />
        <form action={saveSettingsAction} className="flex flex-wrap items-end gap-4 p-5">
          <Field
            label="Llindar «a prop» (0–1)"
            hint="Per sota del compromís però per damunt d’aquesta proporció es mostra en ambre."
          >
            <input name="atRiskRatio" defaultValue={String(ratio)} className={`${inputClass} w-20`} />
          </Field>
          <Field label="Trimestres d’historial" hint="Quant enrere llegeix una sincronització normal.">
            <input
              name="historyQuarters"
              type="number"
              min={1}
              max={12}
              defaultValue={quarters}
              className={`${inputClass} w-20`}
            />
          </Field>
          <button type="submit" className={buttonClass}>
            Guarda
          </button>
        </form>
      </Card>

      <Card id="canvis">
        <CardHeader
          title="Últims canvis"
          subtitle="Qui ha canviat què, i quan. Preus, compromisos, accessos, apunts i tancaments: tot el que escriu el panell queda aquí."
          actions={
            <a href="/api/export/canvis" className={ghostButtonClass}>
              Exporta tot l’historial
            </a>
          }
        />
        {audit.total === 0 ? (
          <EmptyState title="Cap canvi registrat">
            Encara no s’ha tocat cap preu, compromís, accés ni apunt des del panell.
          </EmptyState>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-xs">
                <thead>
                  <tr>
                    <th className={thClass}>Quan</th>
                    <th className={thClass}>Acció</th>
                    <th className={thClass}>Entitat</th>
                    <th className={thClass}>Id</th>
                    <th className={thClass}>Qui</th>
                  </tr>
                </thead>
                <tbody>
                  {audit.rows.map((a) => (
                    <tr key={a.id}>
                      <td className={`${tdClass} tabular-nums text-ink-soft`}>
                        {formatInstant(a.at)}
                      </td>
                      <td className={`${tdClass} font-medium`}>
                        {AUDIT_ACTION_LABEL[a.action] ?? a.action}
                      </td>
                      <td className={tdClass}>{AUDIT_ENTITY_LABEL[a.entity] ?? a.entity}</td>
                      <td className={`${tdClass} text-ink-faint`}>{a.entityId ?? '—'}</td>
                      <td className={tdClass}>{a.actor}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <AuditPager page={audit} />
          </>
        )}
      </Card>
    </div>
  )
}

/**
 * The change log is the one table here that only ever grows, so it is paged instead of
 * truncated: page 1 is the newest AUDIT_PAGE_SIZE entries and every older one is still
 * reachable. The export ignores the paging and takes the lot.
 */
function AuditPager({ page }: { page: AuditPage }) {
  const first = (page.page - 1) * page.pageSize + 1
  const last = first + page.rows.length - 1
  const href = (n: number) => `/configuracio?seccio=sync&canvis=${n}#canvis`
  const disabledClass = 'rounded-lg px-2 py-1 text-xs text-ink-ghost ring-1 ring-line/60'

  return (
    <div className="flex flex-wrap items-center gap-3 border-t border-line px-5 py-3">
      <span className="text-xs tabular-nums text-ink-soft">
        {first}–{last} de {page.total}
      </span>
      <div className="ml-auto flex items-center gap-2">
        <span className="text-xs tabular-nums text-ink-faint">
          Pàgina {page.page} de {page.pageCount}
        </span>
        {page.page > 1 ? (
          <Link href={href(page.page - 1)} className={ghostButtonClass}>
            Més recents
          </Link>
        ) : (
          <span className={disabledClass}>Més recents</span>
        )}
        {page.page < page.pageCount ? (
          <Link href={href(page.page + 1)} className={ghostButtonClass}>
            Més antics
          </Link>
        ) : (
          <span className={disabledClass}>Més antics</span>
        )}
      </div>
    </div>
  )
}

// --- data quality ------------------------------------------------------------

async function DataQualitySection() {
  const q = dataQuality()

  return (
    <div className="space-y-4">
      <Notice tone={q.zeroMinuteShifts > q.liveBookings * 0.1 ? 'bad' : 'info'}>
        Cada avís d’aquesta llista és una reserva que el panell no ha pogut interpretar del tot,
        i per tant un número que en algun lloc és més baix que la realitat. Si «Torns de 0
        minuts» és alt, el més probable és que el format de <code>time_range</code> a Firestore
        no sigui el que espera el lector d’hores.
      </Notice>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <QualityTile label="Reserves vives" value={q.liveBookings} />
        <QualityTile
          label="Torns de 0 minuts"
          value={q.zeroMinuteShifts}
          bad={q.zeroMinuteShifts > 0}
        />
        <QualityTile
          label="Càrrecs sense preu"
          value={q.unpricedItems}
          bad={q.unpricedItems > 0}
        />
        <QualityTile
          label="Reserves òrfenes"
          value={q.orphanBookings}
          bad={q.orphanBookings > 0}
        />
      </div>

      <Card>
        <CardHeader title="Avisos per tipus" />
        {q.anomalies.length === 0 ? (
          <EmptyState title="Cap avís">
            Totes les reserves sincronitzades s’han interpretat completament.
          </EmptyState>
        ) : (
          <table className="w-full text-xs">
            <thead>
              <tr>
                <th className={thClass}>Avís</th>
                <th className={`${thClass} text-right`}>Reserves</th>
                <th className={`${thClass} text-right`}>% del total</th>
              </tr>
            </thead>
            <tbody>
              {q.anomalies.map((a) => (
                <tr key={a.bit}>
                  <td className={`${tdClass} font-medium`}>{a.label}</td>
                  <td className={`${tdClass} text-right tabular-nums`}>{a.count}</td>
                  <td className={`${tdClass} text-right tabular-nums text-ink-faint`}>
                    {q.liveBookings === 0 ? '—' : `${Math.round((a.count / q.liveBookings) * 100)} %`}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>

      <Card>
        <CardHeader title="Altres comprovacions" />
        <ul className="divide-y divide-line text-xs">
          <QualityRow
            label="Torns amb àrea desconeguda"
            value={q.unknownAreaShifts}
            hint="SpecificArea.Unknown es guarda com a cadena buida; també apareix aquí una àrea nova afegida a l’app i no al panell."
          />
          <QualityRow
            label="Files de càrrec sense preu"
            value={q.unpricedAmountRows}
            hint="Un àpat o pernocta sense cap regla de preu aplicable. Compta 0 €, que gairebé mai és el que es vol."
          />
          <QualityRow
            label="Reserves amb 0 hores en total"
            value={q.zeroMinuteBookings}
            hint="Si això és massiu, el lector d’hores és incorrecte."
          />
          <QualityRow
            label="Voluntaris sense tipus"
            value={q.usersWithoutType}
            hint="Sense tipus no s’hereta cap compromís per defecte: sortiran com «sense compromís»."
          />
        </ul>
      </Card>
    </div>
  )
}

function QualityTile({ label, value, bad = false }: { label: string; value: number; bad?: boolean }) {
  return (
    <div className="rounded-xl bg-surface px-4 py-3 ring-1 ring-line">
      <p className="text-[11px] font-medium uppercase tracking-[0.08em] text-ink-faint">{label}</p>
      <p
        className={`mt-1 text-xl font-semibold tabular-nums ${bad ? 'text-warn' : 'text-brand-900'}`}
      >
        {value}
      </p>
    </div>
  )
}

function QualityRow({ label, value, hint }: { label: string; value: number; hint: string }) {
  return (
    <li className="px-5 py-3">
      <div className="flex items-baseline justify-between gap-4">
        <span className="font-medium text-ink">{label}</span>
        <span className={`tabular-nums ${value > 0 ? 'text-warn' : 'text-ink-faint'}`}>{value}</span>
      </div>
      <p className="mt-0.5 text-ink-faint">{hint}</p>
    </li>
  )
}

// --- write-back --------------------------------------------------------------

async function FirebaseSection({
  today,
  month: selected,
}: {
  today: string
  month: { year: number; month: number }
}) {
  const enabled = writebackEnabled()
  const { year, month } = selected
  const period = monthPeriod(year, month)
  const plan = planWriteback(period)
  const history = recentWritebacks()

  const drifting = plan.filter((p) => p.action === 'update')
  const missing = plan.filter((p) => p.action === 'skip_no_doc')
  const duplicates = plan.filter((p) => p.docCount > 1)

  return (
    <div className="space-y-4">
      <Notice tone={enabled ? 'warn' : 'info'}>
        <p>
          El panell llegeix Firestore i prou, amb una única excepció: escriure{' '}
          <code>amount</code> i <code>paid</code> als documents de <code>payments</code> perquè
          el mòbil acabi mostrant les mateixes xifres. Ara mateix està{' '}
          <strong>{enabled ? 'ACTIVAT' : 'desactivat'}</strong>.
        </p>
        <p className="mt-2">
          Activar-ho canviarà imports que els voluntaris ja han vist, perquè{' '}
          <code>payments.amount</code> de l’app ja és incorrecte per a tothom que hagi
          cancel·lat alguna reserva mai: l’app l’incrementa a cada reserva i no el descompta
          mai. Revisa aquest informe abans d’obrir la porta.
        </p>
      </Notice>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <QualityTile label={`Diferències (${period.label})`} value={drifting.length} bad={drifting.length > 0} />
        <QualityTile label="Sense document a l’app" value={missing.length} />
        <QualityTile label="Documents duplicats" value={duplicates.length} bad={duplicates.length > 0} />
        <QualityTile label="Ja coincideixen" value={plan.filter((p) => p.action === 'skip_nochange').length} />
      </div>

      <Card>
        <CardHeader
          title={`Conciliació de ${period.label}`}
          subtitle="«Derivat» són només els càrrecs d’aquest mes, sense saldo anterior — l’app fa amount += delta i un saldo acumulat es duplicaria."
          actions={
            <AutoSubmitForm className="flex items-center gap-2">
              <input type="hidden" name="seccio" value="firebase" />
              <input
                type="month"
                name="mes"
                defaultValue={`${year}-${String(month).padStart(2, '0')}`}
                max={today.slice(0, 7)}
                className={inputClass}
              />
            </AutoSubmitForm>
          }
        />
        <div className="overflow-x-auto">
          <table className="w-full text-xs">
            <thead>
              <tr>
                <th className={thClass}>Voluntari</th>
                <th className={`${thClass} text-right`}>Derivat</th>
                <th className={`${thClass} text-right`}>A l’app</th>
                <th className={`${thClass} text-right`}>Diferència</th>
                <th className={thClass}>Pagat (panell)</th>
                <th className={thClass}>Pagat (app)</th>
                <th className={thClass}>Acció</th>
              </tr>
            </thead>
            <tbody>
              {plan.length === 0 && (
                <tr>
                  <td className={tdClass} colSpan={7}>
                    Cap càrrec ni cap document de pagament aquest mes.
                  </td>
                </tr>
              )}
              {plan.map((p) => (
                <tr key={`${p.uid}-${p.month}`}>
                  <td className={`${tdClass} font-medium`}>
                    {p.name}
                    {p.docCount > 1 && (
                      <Badge className="ml-2 bg-warn/10 text-warn-ink ring-warn/30">
                        {p.docCount} documents
                      </Badge>
                    )}
                  </td>
                  <td className={`${tdClass} text-right tabular-nums`}>{formatCents(p.derivedCents)}</td>
                  <td className={`${tdClass} text-right tabular-nums`}>
                    {p.appAmountCents === null ? '—' : formatCents(p.appAmountCents)}
                  </td>
                  <td
                    className={`${tdClass} text-right tabular-nums ${
                      p.driftCents === 0 ? 'text-ink-faint' : 'text-bad'
                    }`}
                  >
                    {p.driftCents === 0 ? '—' : formatCents(p.driftCents)}
                  </td>
                  <td className={tdClass}>{p.derivedPaid ? 'Sí' : 'No'}</td>
                  <td className={`${tdClass} ${p.paidDiffers ? 'text-bad' : ''}`}>
                    {p.appPaid === null ? '—' : p.appPaid ? 'Sí' : 'No'}
                  </td>
                  <td className={tdClass}>{ACTION_LABEL[p.action]}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <Card>
        <CardHeader title="Interruptor" />
        <div className="flex flex-wrap items-end gap-4 p-5">
          <form action={setWritebackAction}>
            <input type="hidden" name="enabled" value={enabled ? '0' : '1'} />
            <button
              type="submit"
              className={
                enabled
                  ? 'rounded-lg bg-bad px-3 py-1.5 text-xs font-medium text-white transition hover:opacity-90'
                  : buttonClass
              }
            >
              {enabled ? 'Desactiva l’escriptura' : 'Activa l’escriptura'}
            </button>
          </form>

          <form action={runWritebackAction} className="flex flex-wrap items-end gap-3">
            <input type="hidden" name="year" value={year} />
            <input type="hidden" name="month" value={month} />
            <label className="flex items-center gap-1.5 text-xs text-ink-soft">
              <input
                type="checkbox"
                name="allowCreate"
                value="1"
                className="size-3.5 rounded border-line text-brand-500 focus:ring-brand-500"
              />
              Crea els documents que falten
            </label>
            <button
              type="submit"
              disabled={!enabled}
              className={`${ghostButtonClass} disabled:opacity-40`}
            >
              Escriu {period.label} a Firebase
            </button>
          </form>
        </div>
      </Card>

      <Card>
        <CardHeader title="Historial d’escriptures" />
        <div className="overflow-x-auto">
          <table className="w-full text-xs">
            <thead>
              <tr>
                <th className={thClass}>Quan</th>
                <th className={thClass}>Voluntari</th>
                <th className={thClass}>Mes</th>
                <th className={`${thClass} text-right`}>Esperat</th>
                <th className={`${thClass} text-right`}>Escrit</th>
                <th className={thClass}>Estat</th>
              </tr>
            </thead>
            <tbody>
              {history.length === 0 && (
                <tr>
                  <td className={tdClass} colSpan={6}>
                    Encara no s’ha escrit mai res a Firebase.
                  </td>
                </tr>
              )}
              {history.map((w) => (
                <tr key={w.id}>
                  <td className={`${tdClass} tabular-nums`}>{formatInstant(w.createdAt)}</td>
                  <td className={tdClass}>{w.name}</td>
                  <td className={`${tdClass} tabular-nums`}>
                    {w.year}-{String(w.month).padStart(2, '0')}
                  </td>
                  <td className={`${tdClass} text-right tabular-nums`}>
                    {w.expectedAmountCents === null ? '—' : formatCents(w.expectedAmountCents)}
                  </td>
                  <td className={`${tdClass} text-right tabular-nums`}>
                    {formatCents(w.newAmountCents)}
                  </td>
                  <td className={tdClass}>{w.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  )
}

const ACTION_LABEL: Record<string, string> = {
  update: 'Actualitzaria',
  create: 'Crearia',
  skip_nochange: 'Cap canvi',
  skip_no_doc: 'Sense document',
}

// --- small helpers -----------------------------------------------------------

function Field({
  label,
  hint,
  children,
}: {
  label: string
  hint?: string
  children: React.ReactNode
}) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-[11px] font-medium uppercase tracking-[0.08em] text-ink-faint">{label}</span>
      {children}
      {hint && <span className="max-w-[16rem] text-[11px] text-ink-faint">{hint}</span>}
    </label>
  )
}

function formatInstant(epochSeconds: number | null): string {
  if (epochSeconds === null) return '—'
  // The year is not decoration here: the change log now goes back as far as the install does.
  return new Intl.DateTimeFormat('ca-ES', {
    timeZone: 'Europe/Madrid',
    day: '2-digit',
    month: '2-digit',
    year: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(epochSeconds * 1000))
}
