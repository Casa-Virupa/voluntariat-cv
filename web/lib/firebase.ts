import { cert, getApps, initializeApp, applicationDefault, type App } from 'firebase-admin/app'
import { getFirestore, type Firestore } from 'firebase-admin/firestore'
import { readFileSync } from 'node:fs'

/**
 * Read-only access to the mobile app's Firestore, except for the payments write-back
 * and the links editor (configuration/links).
 *
 * Credentials, in order of preference:
 *   1. FIREBASE_SERVICE_ACCOUNT_B64 — base64 key JSON, easiest to put in a systemd unit
 *   2. GOOGLE_APPLICATION_CREDENTIALS — path to a key file kept outside the repo
 *   3. gcloud application-default credentials, for local development
 *
 * The admin SDK bypasses Firestore security rules, which is exactly why this must only
 * ever run server-side and why the allowlist is enforced before any of it is reachable.
 */
function init(): App {
  const existing = getApps()
  if (existing.length) return existing[0]

  const projectId = process.env.FIREBASE_PROJECT_ID
  if (!projectId) {
    throw new Error('FIREBASE_PROJECT_ID is not set — refusing to guess which project to read.')
  }

  const b64 = process.env.FIREBASE_SERVICE_ACCOUNT_B64
  if (b64) {
    return initializeApp({
      credential: cert(JSON.parse(Buffer.from(b64, 'base64').toString('utf8'))),
      projectId,
    })
  }

  const keyFile = process.env.GOOGLE_APPLICATION_CREDENTIALS
  if (keyFile) {
    return initializeApp({
      credential: cert(JSON.parse(readFileSync(keyFile, 'utf8'))),
      projectId,
    })
  }

  return initializeApp({ credential: applicationDefault(), projectId })
}

const globalForFb = globalThis as unknown as { __fs?: Firestore }

export function firestore(): Firestore {
  if (!globalForFb.__fs) {
    globalForFb.__fs = getFirestore(init())
  }
  return globalForFb.__fs
}

export function credentialSource(): string {
  if (process.env.FIREBASE_SERVICE_ACCOUNT_B64) return 'service account (env)'
  if (process.env.GOOGLE_APPLICATION_CREDENTIALS) return 'service account (file)'
  return 'gcloud application-default'
}
