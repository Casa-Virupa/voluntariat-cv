# users CLI (admin tooling)

Creates a Firebase **Auth** account and its **`users/{uid}` Firestore document** in one
step, so the two can't drift apart.

This directory is **not part of the app build** — it isn't a Gradle module, isn't in
`settings.gradle.kts`, and never ends up in an APK/IPA. It runs on your machine only.

## Setup

```bash
npm --prefix tools/users install
```

Then pick one credential path:

**gcloud (preferred — no key file on disk)**

```bash
gcloud auth application-default login
```

**Service-account key** (headless/CI). Firebase console → Project settings → Service
accounts → *Generate new private key*. Keep the file **outside the repo**:

```bash
export GOOGLE_APPLICATION_CREDENTIALS=~/.config/voluntariat/sa-dev.json
```

The CLI prefers the key file when `GOOGLE_APPLICATION_CREDENTIALS` (or `--key-file`) is
set, and falls back to gcloud ADC otherwise. `.gitignore` guards the usual key-file
names, but don't rely on that — store keys outside the repo.

## Usage

One user:

```bash
node tools/users/add-user.mjs \
  --name "Anna Puig" --email anna.puig@example.cat \
  --role volunteer --volunteer-type mitra \
  --areas kitchen,temple --member
```

A batch (see `users.example.csv`):

```bash
node tools/users/add-user.mjs --csv tools/users/my-volunteers.csv
```

Validate first, write nothing:

```bash
node tools/users/add-user.mjs --csv tools/users/my-volunteers.csv --dry-run
```

Useful flags: `--project dev|prod|<project-id>` (default `dev`), `--password` (otherwise
one is generated and printed), `--update` (email already in Auth → reuse its uid and
merge the doc), `--list-areas`, `--help`.

## What gets written

`users/{uid}`, matching `FirestoreUser` in
`shared/data/src/commonMain/.../data/FirestoreModels.kt`:

| Field | Value |
| --- | --- |
| `id` | the Auth uid |
| `name`, `email` | as given |
| `role` | `volunteer` \| `area_responsible` \| `coordination_team` |
| `volunteer_type` | `habitual` \| `mitra`, `null` for non-volunteers |
| `onboarding_completed` | `false` |
| `specific_areas` | array of area strings |
| `is_member` | boolean |

`onboarding_completed: false` is what routes the user to `CreatePasswordNavKey` on first
sign-in, where they swap the temporary password for their own. Hand that temporary
password over privately — it's printed once and stored nowhere.

If the Firestore write fails, the Auth user created in the same run is deleted again, so
you never end up with an account that has no profile.

## Keeping it in sync

The valid strings are duplicated at the top of `add-user.mjs` from
`FirebaseAuthRepository.kt` (`toUserRole`, `toVolunteerType`, `toSpecificArea`). **If you
add a `SpecificArea`, a role or a volunteer type, add it there too** — nothing enforces
this at compile time.
