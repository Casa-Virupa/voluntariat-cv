# VoluntariatCV

Volunteer-shift management for Casa Virupa. This repository holds three codebases:

* **Mobile app** (root) — Kotlin Multiplatform + Compose Multiplatform, targeting Android and iOS. Modules: `androidApp/` (Android entry), `iosApp/` (Xcode project), `features/` (feature screens), `shared/` (core, data, domain, design system…), `build-logic/` (convention plugins). See `CLAUDE.md` for the full architecture and conventions.
* **Admin dashboard** — `web/`, a Next.js app for coordinators. See `web/AGENTS.md` and `web/deploy/README.md`.
* **Admin tooling** — `tools/users/`, a local CLI to create Firebase users. See `tools/users/README.md`.

### Build and run the Android app

Product flavors are `dev` and `prod`, so task names are flavor-scoped:

```shell
./gradlew :androidApp:assembleDevDebug   # build dev debug APK
./gradlew :androidApp:installDevDebug    # install on a device/emulator
```

### Build and run the iOS app

Open [/iosApp](./iosApp) in Xcode, pick a scheme and run. The Xcode build phase rebuilds the shared Kotlin framework automatically.

### Reference types

Here are the exact serialized strings, from `FirebaseVolunteerRepository.kt`, `Volunteer.kt`, and `FirebaseVolunteer.kt`.

## `volunteer_type` — the `type` field (`FirebaseVolunteerType.type`)

The `VolunteerType` sealed class only has **two** category strings written to Firestore (`shared/model/.../calendar/Volunteer.kt`, mapped in `FirebaseVolunteerRepository.kt:206-216`):

|       Domain Type        | String Value |
|--------------------------|--------------|
| `VolunteerType.General`  | `"general"`  |
| `VolunteerType.Specific` | `"specific"` |

When it's `"specific"`, the actual area goes in the `specific_areas` field (null for `"general"`).

> Note: this is per-shift on the reservation (`FirebaseShift.type`). It's distinct from the *user's* volunteer type (`UserVolunteerType` = `Habitual` / `Mitra`), which lives on the user doc.

## `specific_areas` — the `specific_areas` field

From `SpecificArea.toFirebaseValue()` (`FirebaseVolunteerRepository.kt:242-266`):

Specific areas and their serialized string values

The following lists each SpecificArea enum value and the exact string that is written to / read from Firestore.

- Animals: `"animals"`
- Shop: `"shop"`
- Communication: `"communication"`
- VolunteerCoordination: `"volunteer_coordination"`
- Kitchen: `"kitchen"`
- GraphicalDesign: `"graphical_design"`
- VirupaEditions: `"virupa_editions"`
- Exterior: `"exterior"`
- CanBordoiEvents: `"can_bordoi_events"`
- Grove: `"grove"`
- Registrations: `"registrations"`
- Gardening: `"gardening"`
- Labor: `"labor"`
- Maintenance: `"maintenance"`
- Pedagogical: `"pedagogical"`
- CommunityHealth: `"community_health"`
- Grants: `"grants"`
- Temple: `"temple"`
- Transcriptions: `"transcriptions"`
- TechnicalAndAudiovisual: `"technical_and_audiovisual"`
- TechnicalAndTexts: `"technical_and_texts"`
- Unknown: (empty string) `""`

These same strings map back on read via `String.toSpecificArea()`.
