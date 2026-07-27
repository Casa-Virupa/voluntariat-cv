/**
 * Parsing never throws and never silently drops data. Anything the mirror could not make
 * sense of is recorded as a flag on the row, so it shows up in the data-quality panel
 * instead of quietly becoming a zero.
 */
export const ANOMALY = {
  /** A shift's time_range could not be parsed into two times of day. */
  UNPARSEABLE_TIME: 1 << 0,
  /** abs(end - start) was 0 or implausibly long (> 12h). Counted as 0 minutes. */
  IMPLAUSIBLE_RANGE: 1 << 1,
  /** `shift` was neither "morning" nor "afternoon". */
  UNKNOWN_SLOT: 1 << 2,
  /** type.type was not "general"/"specific", or a specific shift had no usable area. */
  UNKNOWN_AREA: 1 << 3,
  /** meal_types contained something other than "lunch"/"dinner". */
  UNKNOWN_MEAL: 1 << 4,
  /** The booking references a user_id with no document in `users`. */
  ORPHAN_USER: 1 << 5,
  /** The booking had no shifts at all. */
  NO_SHIFTS: 1 << 6,
  /**
   * The stored instant was not local midnight in Europe/Madrid — the volunteer booked
   * from another timezone. Not an error; the +12h rule handles it. Worth seeing.
   */
  FOREIGN_TIMEZONE: 1 << 7,
  /** Another booking exists for the same user on the same day (the app allows this). */
  DUPLICATE_DAY: 1 << 8,
} as const

export const ANOMALY_LABELS: Record<number, string> = {
  [ANOMALY.UNPARSEABLE_TIME]: 'Horari illegible',
  [ANOMALY.IMPLAUSIBLE_RANGE]: 'Durada impossible',
  [ANOMALY.UNKNOWN_SLOT]: 'Torn desconegut',
  [ANOMALY.UNKNOWN_AREA]: 'Àrea desconeguda',
  [ANOMALY.UNKNOWN_MEAL]: 'Àpat desconegut',
  [ANOMALY.ORPHAN_USER]: 'Voluntari inexistent',
  [ANOMALY.NO_SHIFTS]: 'Sense torns',
  [ANOMALY.FOREIGN_TIMEZONE]: 'Reserva feta en un altre fus horari',
  [ANOMALY.DUPLICATE_DAY]: 'Reserva duplicada el mateix dia',
}

/**
 * The flags that make a number on screen wrong, missing or unattributable. The other two —
 * a booking made from abroad, and a second booking on the same day — are perfectly normal
 * things a volunteer does, and the mirror handles both correctly.
 *
 * Only these earn a warning marker in the calendar grid. Marking the informational ones
 * there put a ⚠ on a fifth of all cells, which trains people to ignore the symbol.
 */
export const ANOMALY_AFFECTS_NUMBERS =
  ANOMALY.UNPARSEABLE_TIME |
  ANOMALY.IMPLAUSIBLE_RANGE |
  ANOMALY.UNKNOWN_SLOT |
  ANOMALY.UNKNOWN_AREA |
  ANOMALY.UNKNOWN_MEAL |
  ANOMALY.ORPHAN_USER |
  ANOMALY.NO_SHIFTS

export function affectsNumbers(flags: number): boolean {
  return (flags & ANOMALY_AFFECTS_NUMBERS) !== 0
}

export function describeAnomalies(flags: number): string[] {
  return Object.entries(ANOMALY_LABELS)
    .filter(([bit]) => (flags & Number(bit)) !== 0)
    .map(([, label]) => label)
}
