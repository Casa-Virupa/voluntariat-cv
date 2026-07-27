/**
 * Commitment resolution and the done/committed status. Pure functions — no I/O, so the
 * whole rule-precedence and proration story is unit-testable. The caller loads the rules
 * once per page and resolves them per (volunteer × area) in memory; there are a handful
 * of rules and a few dozen volunteers, so a join per cell would buy nothing.
 *
 * Two things here decide whether the coordination table tells the truth:
 *
 *   1. PRECEDENCE. The most specific rule valid AT THE PERIOD START wins
 *      (user > volunteer_type > global). "Valid at the period start" and not "valid
 *      today" is what stops an edit made in July from rewriting May's target.
 *
 *   2. PRORATION. Mitra's commitment is 60h per QUARTER. Viewed one month at a time that
 *      is 20h — but a volunteer who does all 60h in March is not failing in January.
 *      A scaled target therefore never reports "no complert": its deadline has not
 *      arrived yet, so the honest answer is "en curs".
 */

import { AREA_TOTAL } from './contract.ts'
import type { Period, PeriodKind } from './dates.ts'

export type CommitmentScopeKind = 'global' | 'volunteer_type' | 'user'

/** A row of `commitment_rule`, as loaded. */
export interface CommitmentRule {
  id: number
  scopeKind: string
  scopeValue: string | null
  area: string
  periodKind: string
  targetMinutes: number
  validFrom: string
  validTo: string | null
}

export interface ResolvedCommitment {
  ruleId: number
  /** Target expressed in the period the user is looking at. */
  targetMinutes: number
  /** The rule's own target, before scaling. */
  nativeTargetMinutes: number
  nativePeriodKind: PeriodKind
  /** True when nativePeriodKind differs from the viewed period, so the number is derived. */
  scaled: boolean
  scopeKind: CommitmentScopeKind
}

/** Higher wins. A rule aimed at one person beats one aimed at their type. */
const SCOPE_PRIORITY: Record<string, number> = { user: 3, volunteer_type: 2, global: 1 }

export interface VolunteerScope {
  uid: string
  volunteerType: string | null
}

/**
 * Does this rule address this volunteer at all? A `volunteer_type` rule must not leak
 * onto someone whose type is null (a coordinator, say) — they have no commitment, and
 * inventing one would show them as failing.
 */
export function ruleMatchesVolunteer(rule: CommitmentRule, who: VolunteerScope): boolean {
  if (rule.scopeKind === 'global') return true
  if (rule.scopeKind === 'user') return rule.scopeValue === who.uid
  if (rule.scopeKind === 'volunteer_type') {
    return who.volunteerType !== null && rule.scopeValue === who.volunteerType
  }
  return false
}

/** Half-open, evaluated at the period start — see the note at the top of the file. */
export function ruleValidAt(rule: CommitmentRule, date: string): boolean {
  if (rule.validFrom > date) return false
  return rule.validTo === null || date < rule.validTo
}

/**
 * Scales a target between month and quarter. Quarter -> month divides by three (the app's
 * own 60h/quarter becomes 20h/month); month -> quarter multiplies by three.
 */
export function scaleTarget(
  minutes: number,
  from: PeriodKind,
  to: PeriodKind,
): { minutes: number; scaled: boolean } {
  if (from === to) return { minutes, scaled: false }
  if (from === 'quarter' && to === 'month') {
    return { minutes: Math.round(minutes / 3), scaled: true }
  }
  return { minutes: minutes * 3, scaled: true }
}

/**
 * The winning rule for one (volunteer, area, period), or null when nothing applies —
 * which is a real answer, rendered as "sense compromís" rather than as 0 %.
 */
export function resolveCommitment(
  rules: CommitmentRule[],
  who: VolunteerScope,
  area: string,
  period: Period,
): ResolvedCommitment | null {
  let best: CommitmentRule | null = null

  for (const rule of rules) {
    if (rule.area !== area) continue
    if (!ruleMatchesVolunteer(rule, who)) continue
    if (!ruleValidAt(rule, period.from)) continue
    if (best === null || beats(rule, best)) best = rule
  }

  if (!best) return null

  const nativeKind: PeriodKind = best.periodKind === 'quarter' ? 'quarter' : 'month'
  const { minutes, scaled } = scaleTarget(best.targetMinutes, nativeKind, period.kind)

  return {
    ruleId: best.id,
    targetMinutes: minutes,
    nativeTargetMinutes: best.targetMinutes,
    nativePeriodKind: nativeKind,
    scaled,
    scopeKind: best.scopeKind as CommitmentScopeKind,
  }
}

/** Most specific scope, then the latest start, then the highest id as the tiebreak. */
function beats(candidate: CommitmentRule, incumbent: CommitmentRule): boolean {
  const cp = SCOPE_PRIORITY[candidate.scopeKind] ?? 0
  const ip = SCOPE_PRIORITY[incumbent.scopeKind] ?? 0
  if (cp !== ip) return cp > ip
  if (candidate.validFrom !== incumbent.validFrom) return candidate.validFrom > incumbent.validFrom
  return candidate.id > incumbent.id
}

/** Every distinct area a set of rules could put a column on the coordination table. */
export function areasWithRules(rules: CommitmentRule[]): string[] {
  return [...new Set(rules.filter((r) => r.area !== AREA_TOTAL).map((r) => r.area))]
}

// --- status ------------------------------------------------------------------

export type CommitmentStatus =
  /** met the target */
  | 'complete'
  /** short of it, but within the at-risk band */
  | 'at_risk'
  /** short of it, and the period's deadline has passed */
  | 'incomplete'
  /** short of a SCALED target, whose real deadline is later — never a failure */
  | 'in_progress'
  /** no rule applies; the volunteer owes no hours */
  | 'none'

export interface Progress {
  doneMinutes: number
  commitment: ResolvedCommitment | null
  /** done / target, or null when there is no target. Not clamped: 1.4 means 140 %. */
  ratio: number | null
  status: CommitmentStatus
}

export const DEFAULT_AT_RISK_RATIO = 0.8

/**
 * Status for one cell. `atRiskRatio` comes from `app_setting.at_risk_ratio`, so a
 * coordinator can move the amber band without a deploy.
 */
export function progressOf(
  doneMinutes: number,
  commitment: ResolvedCommitment | null,
  atRiskRatio: number = DEFAULT_AT_RISK_RATIO,
): Progress {
  if (!commitment || commitment.targetMinutes <= 0) {
    return { doneMinutes, commitment, ratio: null, status: 'none' }
  }

  const ratio = doneMinutes / commitment.targetMinutes

  let status: CommitmentStatus
  if (ratio >= 1) status = 'complete'
  else if (ratio >= atRiskRatio) status = 'at_risk'
  else status = 'incomplete'

  // A target we derived by dividing a quarter into months cannot be "not complete" —
  // the volunteer still has the rest of the quarter to do it in.
  if (commitment.scaled && status === 'incomplete') status = 'in_progress'

  return { doneMinutes, commitment, ratio, status }
}

export const STATUS_LABEL: Record<CommitmentStatus, string> = {
  complete: 'Complert',
  at_risk: 'A prop',
  incomplete: 'No complert',
  in_progress: 'En curs',
  none: 'Sense compromís',
}

/** Tailwind text colours, kept next to the labels so the two never drift apart. */
export const STATUS_TEXT_CLASS: Record<CommitmentStatus, string> = {
  complete: 'text-ok',
  at_risk: 'text-warn',
  incomplete: 'text-bad',
  in_progress: 'text-slate-500',
  none: 'text-slate-400',
}

export const STATUS_DOT_CLASS: Record<CommitmentStatus, string> = {
  complete: 'bg-ok',
  at_risk: 'bg-warn',
  incomplete: 'bg-bad',
  in_progress: 'bg-slate-300',
  none: 'bg-slate-200',
}
