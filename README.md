This is a Kotlin Multiplatform project targeting Android, iOS.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.


### Reference types

Here are the exact serialized strings, from `FirebaseVolunteerRepository.kt`, `Volunteer.kt`, and `FirebaseVolunteer.kt`.

## `volunteer_type` — the `type` field (`FirebaseVolunteerType.type`)

The `VolunteerType` sealed class only has **two** category strings written to Firestore (`shared/model/.../calendar/Volunteer.kt`, mapped in `FirebaseVolunteerRepository.kt:206-216`):

|       Domain Type        | String Value |
|--------------------------|--------------|
| `VolunteerType.General`  | `"general"`  |
| `VolunteerType.Specific` | `"specific"` |

When it's `"specific"`, the actual area goes in the `specific_areas` field (null for `"general"`).

> Note: this is per-shift on the reservation (`FirebaseShift.type`). It's distinct from the *user's* volunteer type (`UserVolunteerType` = `Habitual` / `Mitra`), which lives on the user doc — let me know if that's the one you meant.

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



---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…