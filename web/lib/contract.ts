/**
 * The Firestore wire contract, mirrored from the Kotlin app.
 *
 * SOURCE OF TRUTH — keep in sync by hand, nothing enforces this at compile time:
 *   roles / volunteer types / areas  shared/data/.../repositories/FirebaseAuthRepository.kt:104-150
 *   shift slots / kinds / meals      shared/data/.../repositories/FirebaseVolunteerRepository.kt:157-266
 *   document shapes                  shared/data/.../data/FirestoreModels.kt
 *                                    shared/data/.../repositories/requests/FirebaseVolunteer.kt
 *                                    shared/data/.../repositories/requests/FirebasePayments.kt
 *
 * These strings are ALSO duplicated in tools/users/add-user.mjs. Three copies now exist;
 * adding a SpecificArea to the Kotlin enum means editing all three or the new area shows
 * up as `__unknown__` here.
 *
 * Catalan labels are copied verbatim from the app's own
 * features/profile/src/commonMain/composeResources/values-ca/strings.xml so the dashboard
 * and the phone name things identically.
 */

// --- roles -------------------------------------------------------------------

export const USER_ROLES = ['volunteer', 'area_responsible', 'coordination_team'] as const
export type UserRole = (typeof USER_ROLES)[number] | 'unknown'

export const VOLUNTEER_TYPES = ['habitual', 'mitra'] as const
export type VolunteerType = (typeof VOLUNTEER_TYPES)[number]

export const VOLUNTEER_TYPE_LABEL: Record<VolunteerType, string> = {
  habitual: 'Habitual',
  mitra: 'Mitra',
}

// --- areas -------------------------------------------------------------------

/**
 * Synthetic buckets. They can never collide with a real code because the Kotlin enum
 * only ever serialises lower_snake_case names (or "" for Unknown).
 */
export const AREA_GENERAL = '__general__'
export const AREA_UNKNOWN = '__unknown__'
/** Not an area at all: the scope of a commitment that covers every hour, whatever area. */
export const AREA_TOTAL = '__total__'

/** The 21 real area codes, in the app's own Catalan alphabetical order. */
export const AREA_LABELS: Record<string, string> = {
  animals: 'Animals',
  shop: 'Botiga',
  communication: 'Comunicació',
  volunteer_coordination: 'Coordinació de voluntariat',
  kitchen: 'Cuina',
  graphical_design: 'Disseny gràfic',
  virupa_editions: 'Edicions Virupa',
  can_bordoi_events: 'Esdeveniments Can Bordoi',
  exterior: 'Exterior',
  grove: 'Hort',
  registrations: 'Inscripcions',
  gardening: 'Jardineria',
  labor: 'Laboral',
  maintenance: 'Manteniment',
  pedagogical: 'Pedagògic',
  community_health: 'Salut de la comunitat',
  grants: 'Subvencions',
  technical_and_audiovisual: 'Tècnic i audiovisual',
  technical_and_texts: 'Tècnic i textos',
  temple: 'Temple',
  transcriptions: 'Transcripcions',
}

export const AREA_CODES = Object.keys(AREA_LABELS)

export const SYNTHETIC_AREA_LABELS: Record<string, string> = {
  [AREA_GENERAL]: 'Voluntariat general',
  [AREA_UNKNOWN]: 'Àrea desconeguda',
  [AREA_TOTAL]: 'Total (totes les àrees)',
}

export function areaLabel(code: string): string {
  return AREA_LABELS[code] ?? SYNTHETIC_AREA_LABELS[code] ?? code
}

export function isKnownArea(code: string): boolean {
  return code in AREA_LABELS
}

// --- bookings ----------------------------------------------------------------

export const SHIFT_SLOTS = ['morning', 'afternoon'] as const
export type ShiftSlot = (typeof SHIFT_SLOTS)[number] | 'unknown'

export const SHIFT_SLOT_LABEL: Record<ShiftSlot, string> = {
  morning: 'Matí',
  afternoon: 'Tarda',
  unknown: 'Desconegut',
}

export const SHIFT_KINDS = ['general', 'specific'] as const
export type ShiftKind = (typeof SHIFT_KINDS)[number] | 'unknown'

/** Breakfast is never persisted by the app; it is billable-by-schema but always free. */
export const CHARGEABLE_ITEMS = ['lunch', 'dinner', 'breakfast', 'sleep'] as const
export type ChargeableItem = (typeof CHARGEABLE_ITEMS)[number]

export const ITEM_LABEL: Record<ChargeableItem, string> = {
  lunch: 'Dinar',
  dinner: 'Sopar',
  breakfast: 'Esmorzar',
  sleep: 'Pernocta',
}

/** Only these two ever appear in a `meal_types` array. */
export const PERSISTED_MEALS = ['lunch', 'dinner'] as const

// --- collections -------------------------------------------------------------

export const COLLECTION_USERS = 'users'
export const COLLECTION_VOLUNTEERS = 'volunteers'
export const COLLECTION_PAYMENTS = 'payments'

// --- the app's own defaults, reproduced ---------------------------------------

/** Volunteer.kt:25-26 — superseded by the price_rule table, seeded from these. */
export const APP_DEFAULT_PRICES_CENTS: Record<ChargeableItem, number> = {
  lunch: 800,
  dinner: 800,
  breakfast: 0,
  sleep: 1000,
}

/** User.kt:20-35 — the only commitment the app knows. Mitra's 60h is per QUARTER. */
export const APP_DEFAULT_COMMITMENT = {
  mitra: { minutes: 60 * 60, period: 'quarter' as const },
  habitual: { minutes: 8 * 60, period: 'month' as const },
}

// --- the canonical timezone ---------------------------------------------------

/**
 * `volunteers.timestamp` is midnight in the BOOKER'S device zone
 * (DateTimeUtils.kt:34 uses TimeZone.currentSystemDefault()), so it is 22:00Z or 23:00Z
 * of the previous day for a Spanish volunteer. Never read it as a UTC date.
 */
export const CANONICAL_TZ = 'Europe/Madrid'
