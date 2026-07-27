# VoluntariatCV

Kotlin Multiplatform + Compose Multiplatform app for volunteer-shift management at Casa Virupa. Targets **Android and iOS** from a shared codebase.

## Tech stack

- Kotlin 2.3.10 (forced `languageVersion = KOTLIN_2_2` via convention), Compose Multiplatform 1.10.0
- AGP 8.13.2, JDK 21, Android compileSdk 36 / minSdk 24 / targetSdk 36
- **Navigation**: Jetpack **Navigation3** (`androidx.navigation3`, alpha) — `rememberNavBackStack`, `NavDisplay`, `@Serializable NavKey`
- **DI**: Koin 4.1.1 (`viewModelOf`, `koinViewModel`, `koinInject`)
- **Backend**: Firebase Auth + Firestore via **GitLive KMP wrapper** (`dev.gitlive:firebase-auth`, `firebase-firestore`). Android also pulls `firebase-bom` + `firebase-analytics`; iOS uses native `FirebaseCore`.
- **HTTP**: Ktor 3.4 (client-android / client-darwin). Used to hit a Google Apps Script endpoint for calendar events.
- **Utils**: kotlinx-datetime, kotlinx-serialization-json, Kermit (logging)

## Build & run

Product flavors **`dev`** and **`prod`** apply — Gradle task names are `assemble<Flavor><BuildType>` / `install<Flavor><BuildType>`.

- `./gradlew :androidApp:assembleDevDebug` — build dev debug APK
- `./gradlew :androidApp:installDevDebug` — install on device/emulator
- `./gradlew :androidApp:assembleProdRelease` — prod release build
- iOS: open `iosApp/iosApp.xcodeproj` in Xcode, pick a scheme, Run. Xcode build phase rebuilds the `Shared` Kotlin framework.

Firebase config:
- Android: `androidApp/src/dev/google-services.json` (flavor-scoped; no prod file yet).
- iOS: `iosApp/iosApp/Firebase/Dev/GoogleService-Info.plist`. Environment selection is read from the Info.plist key `EnvironmentConfiguration` (see `iosApp/iosApp/Core/EnvironmentConfig.swift`).

The `BuildEnvironment` (Dev/Prod) comes from `BuildConfig.BUILD_TYPE` on Android (set via the flavors convention plugin) and from Info.plist on iOS, then flows into Koin via `initKoin(buildEnvironment)`.

## Gradle module graph

```
androidApp/                                  # Android app entry (application module)
iosApp/                                      # Xcode project (SwiftUI entry)

features/
  authentication/   sign-in, create-password screens + VMs + authModule
  calendar/         calendar, day-detail, reservation-form + VMs + calendarModule

shared/
  common/           AppViewModel + InitialUserState (auth bootstrap)
  core/             BuildEnvironment, Ktor HttpClient factory, Navigation3
                    state/navigator helpers (Auth + Main), DateTimeUtils,
                    CalendarConstants, coreModule
  data/             FirebaseAuthRepository, RemoteCalendarRepository,
                    Firestore/request/response models, dataModule
                    (bindings for AuthRepository / CalendarRepository)
  dependencies/     initKoin() entry + SharedModule aggregating feature modules
  designsystem/     VoluntariatCVTheme, Colors, Typography, Buttons,
                    TextFields, Pickers (expect/actual on Android/iOS)
  domain/           AuthRepository, CalendarRepository interfaces
  model/            Domain models: User/UserId/UserRole/VolunteerType,
                    Reservation/Meal/VolunteerShift/TechnicalAreaTurn,
                    Event, GoogleCalendarEvent
  ui/               RootApp (auth nav host) + MainApp (main nav host) +
                    iOS MainViewController + initKoinIOS
```

Dependency direction: `androidApp`/`iosApp` → `shared/ui` → `shared/dependencies` → features → `shared/{common, core, designsystem, domain, data, model}`. `shared/data` depends on `shared/domain` + `shared/core` + `shared/model`. **Never** point `shared/domain` or `shared/model` at anything else — they are the clean layer.

Type-safe project accessors are enabled: use `implementation(projects.shared.core)` — not `project(":shared:core")`.

## Convention plugins (`build-logic/convention/`)

All modules apply one of these instead of hand-rolling config:

| Plugin ID | Purpose |
| --- | --- |
| `voluntariatcv.android.application` | Android app base (compileSdk 36, minSdk 24, JVM 21, BuildConfig) |
| `voluntariatcv.android.application.compose` | + Compose |
| `voluntariatcv.android.application.flavors` | + `dev`/`prod` product flavors with `BUILD_TYPE` buildConfig field |
| `voluntariatcv.kotlin.multiplatform.library` | KMP library base — applies `android.kotlin.multiplatform.library` + `kotlin.multiplatform` + `kotlin.serialization`; auto-adds kermit, koin-core, kotlinx-serialization-json to `commonMain`, and kotlin-test + coroutines-test to `commonTest` |
| `voluntariatcv.compose.multiplatform.library` | + Compose UI stack, Navigation3, koin-compose |

Changes to `build-logic` require a Gradle sync; it's an included build (`includeBuild("build-logic")` in `settings.gradle.kts`).

## Entry points & app flow

- **Android** (`androidApp/.../VoluntariatApplication.kt`): starts Koin with `initKoin(buildEnvironment) { androidContext(...) }`. `MainActivity` collects `AppViewModel.initialUserState`, keeps the splash screen until it resolves, then sets `RootApp(userState)` as content.
- **iOS** (`iosApp/iosApp/iOSApp.swift`): `AppDelegate` calls `FirebaseApp.configure()`; `init` calls `InitKoinIosKt.doInitKoinIOS(buildEnvironment:)`. `ContentView` hosts `MainViewControllerKt.MainViewController` which renders the same `RootApp`.
- **AppViewModel** (`shared/common`): on init, calls `AuthRepository.getCurrentUser()` → publishes `InitialUserState.LoggedIn(onboardingCompleted)` or `NotLogged`.
- **RootApp** (`shared/ui`): auth-layer NavDisplay. Start destination picked from `InitialUserState`:
  - `NotLogged` → `SignInNavKey`
  - `LoggedIn(onboardingCompleted = false)` → `CreatePasswordNavKey`
  - `LoggedIn(onboardingCompleted = true)` → `MainAppContentNavKey` (renders `MainApp`)
- **MainApp**: main-content NavDisplay starting at `CalendarNavKey`, with destinations `ReservationFormNavKey`, `DayDetailNavKey(date)`.

Two separate nav stacks (auth vs main) keep auth history isolated. `MainAppContentNavKey` sets `isLastNavKey = true`, so navigating to it clears the auth stack.

## Conventions

- **New feature screen** → create a KMP library under `features/<name>/`, apply `voluntariatcv-kotlin-multiplatform-library` + `voluntariatcv-compose-multiplatform-library`. Add:
  - `di/<Feature>Module.kt` with `viewModelOf(::YourViewModel)` — register it in `shared/dependencies/SharedModule.kt`.
  - `navigation/<Feature>Navigation.kt` with `@Serializable` NavKeys and an `EntryProviderScope<NavKey>.<feature>Entry(navigator)` extension.
  - Register each NavKey in the relevant `SavedStateConfiguration` polymorphic block (`RootApp`/`MainApp`) — **missing registration breaks state restoration**.
- **Keyed ViewModel** (e.g. needs the NavKey) → in the entry: `koinViewModel<VM> { parametersOf(navKey) }`, and the Koin binding takes the key as a constructor param.
- **New domain model** → `shared/model`. **New repo contract** → `shared/domain`. **New repo impl** → `shared/data`, bound in `dataModule` with `singleOf(::Impl) bind Interface::class`.
- **New library** → add in `gradle/libs.versions.toml` (`[versions]` + `[libraries]`), then reference via `libs.xxx` in the appropriate `sourceSet.dependencies { }`.
- **Android-only dep** → goes in `androidMain.dependencies` (or `androidApp/build.gradle.kts`), never in `commonMain`.
- **expect/actual** lives under `shared/core` (DateTimeUtils) and `shared/designsystem` (Pickers, ModifierExtensions). Follow that pattern for any platform-specific API.
- **No raw strings in UI**: i18n strings go in `composeResources/values/strings.xml` + `values-ca` + `values-es`.
- **Logging**: use Kermit (`co.touchlab.kermit.Logger`), not `println`.
- **Results**: suspend repo APIs return `kotlin.Result<T>`. Prefer `runCatching { ... }` in implementations.

## Firebase contract (current)

- Firestore collections:
  - `users/{uid}` — shape: `FirestoreUser(id, name, email, role, onboarding_completed)`. Role strings: `"volunteer"`, `"area_responsible"`, `"coordination_team"`.
  - `reservations` — shape: `FirebaseReservation(userId, date, scheduleRange, technicalAreaTurn, mealType, sleep)`. Documents are added with `.add(...)` (auto-id).
- `AuthRepository.updateNewPassword` reauthenticates with email/password then writes `onboarding_completed = true` to the user doc.
- Google Calendar events are fetched from an Apps Script URL (`CalendarConstants.SCRIPT_URL`) — this is **not** Firebase but lives behind the same `CalendarRepository` interface.

## Gotchas

- Gradle task names are flavor-scoped — `:androidApp:assembleDebug` does not exist; use `:androidApp:assembleDevDebug`.
- `org.gradle.configuration-cache=true` is on — build script changes occasionally invalidate it; if Gradle reports configuration-cache problems, they must be fixed, not suppressed.
- `warningsAsErrors` can be flipped via `~/.gradle/gradle.properties` (project default is `false`).
- `kotlin.languageVersion` is pinned to `KOTLIN_2_2` in the convention — features added in 2.3 won't compile.
- When adding a new NavKey, updating the `EntryProviderScope` extension is **not enough** — you must also add a `subclass(...)` line in the matching `SavedStateConfiguration` polymorphic block.
- iOS: after changing shared-code public APIs, rebuild from Xcode (not just Gradle) so Swift sees the new symbols in `Shared.framework`.
- There is currently no `prod`-flavor Firebase config on Android; adding `prod` builds requires `androidApp/src/prod/google-services.json`.
