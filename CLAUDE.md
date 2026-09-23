# Voluntariat app (Casa Virupa)

Kotlin Multiplatform (Android + iOS) volunteer app. Firestore via GitLive (`dev.gitlive:firebase-*`),
DI via Koin, UI in Compose Multiplatform. The admin web dashboard (separate repo:
`../voluntariat-dashboard`) is the single writer of all `*_rules` collections; the app only reads.

## Module layout

- `shared/model` — pure domain models + logic (no Firebase). Tests live here (`commonTest`).
- `shared/domain` — repository interfaces.
- `shared/data` — Firebase implementations (`Firebase*Repository`) + `@Serializable` DTOs in
  `repositories/requests/`. Registered in `shared/data/.../di/DataModule.kt`.
- `features/*` — Compose screens + ViewModels, each with its own Koin module in `di/`.

## Dashboard-published rule collections (read-only for the app)

Both follow the same pattern: fetch the whole collection as a snapshots Flow, map per-doc with
`runCatching { doc.data<DTO>() }` so malformed docs are skipped, `.catch` on the Flow.

- `price_rules` → `FirebasePriceRepository` → `PriceRules.priceAt(date)`. Errors emit `PriceRules.Empty`.
- `commitment_rules` → `FirebaseCommitmentRepository` → `CommitmentRules.targetFor(...)` (total)
  and `.areaTargetsFor(...)` (per-area, shown in the profile breakdown rows).
  Errors emit **null** (not empty): null means "could not read → fall back to the hardcoded
  defaults in `User.getMonthHours()` (60 h/quarter Mitra, 8 h/month Habitual)", while a loaded
  collection with no matching rule means "no commitment → show no target, not 0 %".
  Spec: `../voluntariat-dashboard/COMMITMENTS-APP.md`. Key points:
  - Doc IDs `dash-<n>`; `<n>` is the last tiebreak in resolution.
  - Resolution: filter by area + scope match + validity at the **period start**
    (`valid_from <= start`, `valid_to` exclusive/absent); winner = highest scope specificity
    (user > volunteer_type > global), then greatest `valid_from`, then greatest `<n>`.
  - Areas are typed (`CommitmentArea`: `Total`/`General`/`Specific(SpecificArea)`); the data
    layer maps `__total__`/`__general__`/area codes via `toSpecificArea()`.
  - `target_minutes` is minutes; scale when the rule's native `period_kind` differs from the viewed
    period (quarter→month: /3 rounded; month→quarter: ×3). A scaled target must never be shown as
    "failed" — the native period isn't over.
  - Resolution logic + tests: `shared/model/.../commitment/CommitmentRule.kt` and
    `CommitmentRulesTest.kt` (mirrors the dashboard's `lib/commitments.ts`).
  - Consumed by `ProfileViewModel.commitmentTarget`; `DegreeOfCompliance` in `ProfileScreen`
    handles the null-target (no commitment) rendering.

## Dashboard-published config docs (read-only for the app)

- `configuration/links` → `FirebaseInterestLinksRepository` → `InterestLinks` (profile "Enllaços d'interès").
- `configuration/areas` → `FirebaseAreaConfigRepository` → `AreaConfig.onlineAreas`: the specific
  areas whose shifts may be done **online**. Missing doc / error → `AreaConfig.Empty` (nothing is
  online). Drives the "online" switch in `ReservationFormViewModel` (`showOnlineToggle`) and the
  forced-online flow on «NO VOLUNTARIAT» days (`RemoteDialog`, `forcedOnline`: only `Specific`
  shifts in online areas, no meals/nights). General volunteering is never online.
  Editor: dashboard `/configuracio?seccio=arees`; shape in `lib/areas-doc.ts`.

## Fields the app writes / reads beyond the basics

- `volunteers/{id}.shifts[i].online: Boolean` (default false) ← `Shift.online`. Shown as an
  «Online» `CVTag` in the reservation summary and `DayDetailScreen`; dashboard mirrors it into
  `fs_booking_shift.online` (badge in the calendar day panel).
- `users/{uid}.food_handler_certificate: Boolean` (default false) ← `User.hasFoodHandlerCertificate`.
  **Currently hidden**: the `Certificates(...)` call in `ProfileScreen` is commented out until the
  Firestore rules allow the write; uncomment those lines to release. The **volunteer sets it
  themselves** from the profile `Certificates` switch via
  `AuthRepository.setFoodHandlerCertificate` — together with `onboarding_completed` it is the
  only user field the app writes, so the console Firestore rules must allow the owner to update
  exactly those keys. Dashboard shows it as an «Aliments» badge (coordinació, vista d'àpats) and
  Excel column; never label it «Manipulador».
- Reservation-form notice: a **habitual non-member** sees «avisa-ho a l'hostatgeria» under the
  additional options (`showSleepNotice`) because the sleep chip is member-only.
- **Meals/bed-only bookings** (issue #108): a booking may have **no shifts** when it has meals or
  a bed. It is priced with the `<item>_no_volunteering` fields of `price_rules` (issue #93,
  `Prices.forBooking`), stays outside the mitra quota, isn't counted as a volunteer (calendar /
  day header) and shows under «Només àpats o pernoctació» in `DayDetailScreen`.
- **Mitra quota** (`MonthlyCharge.kt`, issue #90): `mitra_free_meals` is one pool for lunch,
  dinner and same-day breakfast (fallback `mitra_free_lunches + mitra_free_dinners` on old docs);
  `mitra_free_sleeps` nights, and a free night frees its next-day breakfast. Zero-priced items
  take no slot; order = date, booking id, breakfast → lunch → dinner. Mirrored bit-for-bit by
  the dashboard's `v_charge_discounted` (`lib/allowance.test.ts`).

Firestore security rules are console-managed (not in either repo): rule collections are
`allow read: if request.auth != null; allow write: if false;`.

## Volunteer-type string mapping

Firestore stores `"mitra"`/`"habitual"` (also used by `commitment_rules.scope_value`);
mapping lives in `FirebaseAuthRepository.kt` (`String?.toVolunteerType()`).

IMPORTANT: `User.volunteerType` is carried **independently of role**, mirroring the dashboard
(`lib/commitments.ts` matches on the raw `volunteer_type` field with no role gate) — an
admin/coordinator can still be a mitra. `isMitra` and `getMonthHours()` derive from it, not
from `role`. Don't reintroduce the old pattern of reading the type through
`(role as? UserRole.Volunteer)?.type`.

## Building & testing

- Android SDK (platform 36, cmdline-tools) lives at `~/Library/Android/sdk` (see
  `local.properties`, gitignored).
  - Android build: `./gradlew :androidApp:assembleDevDebug`. Only the `dev` flavor has a
    `google-services.json`; `prod` variants fail on that missing (secret) file — not a code error.
  - Tests: `./gradlew :shared:model:iosSimulatorArm64Test`
  - iOS compile check: `./gradlew :features:profile:compileKotlinIosSimulatorArm64` (etc. per module)

## Workflow

- Never commit without Oscar's explicit consent; no Claude co-author lines in commits.
