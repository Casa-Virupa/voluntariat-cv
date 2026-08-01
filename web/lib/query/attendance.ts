/**
 * Head-counts per day for the kitchen and the guest house: how many DISTINCT people have
 * a live booking with lunch, dinner or an overnight stay on each service date.
 *
 * COUNT(DISTINCT …) and not COUNT(*): (user_id, service_date) is not unique — the app's
 * duplicate guard is client-side only — and a person who booked twice still eats once.
 */

import { raw } from '../db/index.ts'
import { eachDay } from '../dates.ts'

export interface AttendanceDay {
  date: string
  lunch: number
  dinner: number
  sleep: number
}

/**
 * One row per day in the INCLUSIVE [from, to] range, zero-filled: the spreadsheet
 * consuming this needs "nobody signed up" to be a 0 on that date, not a missing row.
 * Both bounds must already be valid 'YYYY-MM-DD' strings — the route validates.
 */
export function attendanceByDay(from: string, to: string): AttendanceDay[] {
  const rows = raw()
    .prepare(
      `SELECT service_date AS date,
              COUNT(DISTINCT CASE WHEN has_lunch = 1 THEN user_id END) AS lunch,
              COUNT(DISTINCT CASE WHEN has_dinner = 1 THEN user_id END) AS dinner,
              COUNT(DISTINCT CASE WHEN sleep = 1 THEN user_id END) AS sleep
         FROM fs_booking
        WHERE deleted_at IS NULL AND service_date >= ? AND service_date <= ?
        GROUP BY service_date
        ORDER BY service_date`,
    )
    .all(from, to) as AttendanceDay[]

  const byDate = new Map(rows.map((r) => [r.date, r]))
  return eachDay(from, to).map(
    (date) => byDate.get(date) ?? { date, lunch: 0, dinner: 0, sleep: 0 },
  )
}
