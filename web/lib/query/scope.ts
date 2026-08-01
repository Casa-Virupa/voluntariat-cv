/**
 * Who an area_responsible means by "els meus voluntaris".
 *
 * There are two ways to belong to an area, and a volunteer needs only one of them:
 *
 *   1. THEY ARE LISTED IN IT — `fs_user_area`, mirrored from the app's `specific_areas`.
 *   2. A COMMITMENT AIMED AT THE AREA APPLIES TO THEM — a `commitment_rule` for that area,
 *      whether it is global, per volunteer type, or personal.
 *
 * The second is what makes the filter useful before anybody has booked anything: a target
 * of 8h in Cuina is the Cuina responsible's business even if the volunteer never ticked
 * Cuina on their phone. `__total__` rules are excluded — a whole-organisation target says
 * nothing about which area it belongs to, so counting it would make every volunteer
 * everybody's.
 *
 * Rules are evaluated AT THE PERIOD START, exactly as the coordination table resolves them,
 * so the filter and the commitment columns can never disagree about who has a commitment.
 *
 * This is a view, never a permission: the area scope of what a responsible may read stays
 * `allowedAreas` in the query layer (see lib/authz.ts).
 */

import { raw } from '../db/index.ts'
import { AREA_TOTAL } from '../contract.ts'
import { ruleMatchesVolunteer, ruleValidAt, type CommitmentRule } from '../commitments.ts'

/** Every rule valid at `date`, precedence unresolved — the caller decides which one wins. */
export function commitmentRulesValidAt(date: string): CommitmentRule[] {
  const rows = raw()
    .prepare(
      `SELECT id, scope_kind AS scopeKind, scope_value AS scopeValue, area,
              period_kind AS periodKind, target_minutes AS targetMinutes,
              valid_from AS validFrom, valid_to AS validTo
         FROM commitment_rule`,
    )
    .all() as CommitmentRule[]
  // Filtered in JS rather than SQL so the same predicate serves the config page's preview.
  return rows.filter((r) => ruleValidAt(r, date))
}

/**
 * The uids behind "els meus voluntaris" for the given areas, at the given date.
 *
 * Returned as an explicit list rather than as a SQL subquery because the commitment half
 * of the definition lives in `ruleMatchesVolunteer`, and duplicating that precedence logic
 * in SQL is exactly how the filter and the columns would drift apart. The list is bounded
 * by the number of mirrored volunteers, which is dozens.
 *
 * An empty result is meaningful — "nobody" — and callers must render it as such rather
 * than dropping the filter.
 */
export function myVolunteerUids(areas: string[], atDate: string): string[] {
  if (areas.length === 0) return []

  const placeholders = areas.map(() => '?').join(', ')
  const listed = raw()
    .prepare(`SELECT DISTINCT uid FROM fs_user_area WHERE area IN (${placeholders})`)
    .all(...areas) as Array<{ uid: string }>

  const uids = new Set(listed.map((r) => r.uid))

  const rules = commitmentRulesValidAt(atDate).filter(
    (r) => r.area !== AREA_TOTAL && areas.includes(r.area),
  )
  if (rules.length > 0) {
    const volunteers = raw()
      .prepare(`SELECT uid, volunteer_type AS volunteerType FROM fs_user`)
      .all() as Array<{ uid: string; volunteerType: string | null }>

    for (const v of volunteers) {
      if (rules.some((rule) => ruleMatchesVolunteer(rule, v))) uids.add(v.uid)
    }
  }

  return [...uids]
}
