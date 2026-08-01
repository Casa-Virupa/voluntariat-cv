/**
 * The app's "Enllaços d'interès" text, which lives in Firestore at `configuration/links`
 * — the mobile profile screen reads it directly, so saving here is live for every
 * volunteer on their next app open. This is the dashboard's second Firestore write after
 * the payments write-back, and like that one it is audited.
 *
 *   configuration/links
 *   ├── text        string — the block shown verbatim in the app (URLs become tappable)
 *   └── updated_at  string — ISO-8601 instant, informative only
 */

import { firestore } from './firebase.ts'
import { audit } from './settings.ts'

const DOC_PATH = 'configuration/links'

export interface LinksConfig {
  text: string
  updatedAt: string | null
}

export async function readLinks(): Promise<LinksConfig> {
  const snap = await firestore().doc(DOC_PATH).get()
  const data = snap.data() ?? {}
  return {
    text: typeof data.text === 'string' ? data.text : '',
    updatedAt: typeof data.updated_at === 'string' ? data.updated_at : null,
  }
}

export async function writeLinks(text: string, actor: string): Promise<void> {
  const before = await readLinks()
  // Second precision, no milliseconds — the exact shape the app already stores.
  const updatedAt = new Date().toISOString().replace(/\.\d{3}Z$/, 'Z')

  await firestore().doc(DOC_PATH).set({ text, updated_at: updatedAt })
  audit(actor, 'links.set', 'firestore_links', DOC_PATH, { text: before.text }, { text })
}
