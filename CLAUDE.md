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
- `commitment_rules` → `FirebaseCommitmentRepository` → `CommitmentRules.targetFor(...)`.
  Errors emit **null** (not empty): null means "could not read → fall back to the hardcoded
  defaults in `User.getMonthHours()` (60 h/quarter Mitra, 8 h/month Habitual)", while a loaded
  collection with no matching rule means "no commitment → show no target, not 0 %".
  Spec: `../voluntariat-dashboard/COMMITMENTS-APP.md`. Key points:
  - Doc IDs `dash-<n>`; `<n>` is the last tiebreak in resolution.
  - Resolution: filter by area (`__total__` for the profile UI) + scope match + validity at the
    **period start** (`valid_from <= start`, `valid_to` exclusive/absent); winner = highest
    scope specificity (user > volunteer_type > global), then greatest `valid_from`, then greatest `<n>`.
  - `target_minutes` is minutes; scale when the rule's native `period_kind` differs from the viewed
    period (quarter→month: /3 rounded; month→quarter: ×3). A scaled target must never be shown as
    "failed" — the native period isn't over.
  - Resolution logic + tests: `shared/model/.../commitment/CommitmentRule.kt` and
    `CommitmentRulesTest.kt` (mirrors the dashboard's `lib/commitments.ts`).
  - Consumed by `ProfileViewModel.commitmentTarget`; `DegreeOfCompliance` in `ProfileScreen`
    handles the null-target (no commitment) rendering.

Firestore security rules are console-managed (not in either repo): rule collections are
`allow read: if request.auth != null; allow write: if false;`.

## Volunteer-type string mapping

Firestore stores `"mitra"`/`"habitual"` (also used by `commitment_rules.scope_value`);
mapping lives in `FirebaseAuthRepository.kt` (`String?.toVolunteerType()`).

## Building & testing

- This machine has no Android SDK configured — Android compile tasks fail with "SDK location not
  found". Use the iOS targets to verify common code:
  - Tests: `./gradlew :shared:model:iosSimulatorArm64Test`
  - Compile check: `./gradlew :features:profile:compileKotlinIosSimulatorArm64` (etc. per module)

## Workflow

- Never commit without Oscar's explicit consent; no Claude co-author lines in commits.
