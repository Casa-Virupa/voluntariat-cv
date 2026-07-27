#!/usr/bin/env node
// Admin-only CLI. Creates a Firebase Auth user and its users/{uid} Firestore
// document in one step, so both sides can never drift apart.
//
//   node add-user.mjs --name "Anna Puig" --email anna@x.cat \
//                     --role volunteer --volunteer-type mitra --areas kitchen,temple
//   node add-user.mjs --csv users.csv
//
// The Firestore shape mirrors FirestoreUser in
// shared/data/.../data/FirestoreModels.kt — keep them in sync.

import { readFileSync } from 'node:fs'
import { randomInt } from 'node:crypto'
import { parseArgs } from 'node:util'
import { initializeApp, cert, applicationDefault } from 'firebase-admin/app'
import { getAuth } from 'firebase-admin/auth'
import { getFirestore } from 'firebase-admin/firestore'

const PROJECTS = {
  dev: 'voluntariat-casa-virupa-dev',
  prod: 'voluntariat-casa-virupa',
}

// Mirrors String.toUserRole / toVolunteerType / toSpecificArea in
// shared/data/.../repositories/FirebaseAuthRepository.kt. "unknown" is
// deliberately not accepted — it is a parse fallback, not a real value.
const ROLES = ['volunteer', 'area_responsible', 'coordination_team']
const VOLUNTEER_TYPES = ['habitual', 'mitra']
const AREAS = [
  'animals', 'shop', 'communication', 'volunteer_coordination', 'kitchen',
  'graphical_design', 'virupa_editions', 'exterior', 'can_bordoi_events',
  'grove', 'registrations', 'gardening', 'labor', 'maintenance',
  'pedagogical', 'community_health', 'grants', 'temple', 'transcriptions',
  'technical_and_audiovisual', 'technical_and_texts',
]

const USAGE = `
Create Firebase Auth users + their Firestore doc.

Single user:
  node add-user.mjs --name <name> --email <email> --role <role> [options]

Batch:
  node add-user.mjs --csv <file>        (columns: name,email,role,volunteer_type,specific_areas,is_member,password)

Options:
  --name <s>            Full name (required)
  --email <s>           Email, also the sign-in identifier (required)
  --role <s>            ${ROLES.join(' | ')}
  --volunteer-type <s>  ${VOLUNTEER_TYPES.join(' | ')}   (required when --role volunteer)
  --areas <a,b>         Comma-separated specific areas (see --list-areas)
  --member              Mark as member (is_member: true)
  --password <s>        Temporary password. Omitted -> one is generated and printed.
  --project <s>         dev | prod | <explicit project id>   (default: dev)
  --update              If the email already exists in Auth, reuse its uid and rewrite the doc
  --dry-run             Validate and print what would happen, write nothing
  --list-areas          Print the valid specific areas and exit
  --help

Credentials (either one):
  gcloud auth application-default login          # preferred, no key file
  export GOOGLE_APPLICATION_CREDENTIALS=<path>   # or --key-file <path>
`

const { values: flags, positionals } = parseArgs({
  allowPositionals: true,
  options: {
    name: { type: 'string' },
    email: { type: 'string' },
    role: { type: 'string' },
    'volunteer-type': { type: 'string' },
    areas: { type: 'string' },
    member: { type: 'boolean', default: false },
    password: { type: 'string' },
    project: { type: 'string', default: 'dev' },
    'key-file': { type: 'string' },
    csv: { type: 'string' },
    update: { type: 'boolean', default: false },
    'dry-run': { type: 'boolean', default: false },
    'list-areas': { type: 'boolean', default: false },
    help: { type: 'boolean', default: false },
  },
})

if (flags.help) exitWith(USAGE, 0)
if (flags['list-areas']) exitWith(AREAS.join('\n'), 0)
if (positionals.length) exitWith(`Unexpected argument: ${positionals[0]}\n${USAGE}`, 1)

const projectId = PROJECTS[flags.project] ?? flags.project

// --- input ------------------------------------------------------------------

/** A row is the raw, untrusted user input; normalise() turns it into a record. */
function normalise(row, index) {
  const where = index === null ? '' : ` (row ${index + 2})`
  const fail = (msg) => {
    throw new Error(`${msg}${where}`)
  }

  const name = (row.name ?? '').trim()
  const email = (row.email ?? '').trim().toLowerCase()
  const role = (row.role ?? '').trim()
  const volunteerType = (row.volunteer_type ?? '').trim() || null
  const areas = splitList(row.specific_areas)
  const isMember = parseBool(row.is_member)
  const password = (row.password ?? '').trim() || generatePassword()

  if (!name) fail('Missing name')
  if (!email) fail('Missing email')
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) fail(`Invalid email "${email}"`)
  if (!ROLES.includes(role)) fail(`Invalid role "${role}" — expected ${ROLES.join(' | ')}`)

  if (role === 'volunteer') {
    if (!volunteerType) fail('Role "volunteer" requires a volunteer_type')
    if (!VOLUNTEER_TYPES.includes(volunteerType)) {
      fail(`Invalid volunteer_type "${volunteerType}" — expected ${VOLUNTEER_TYPES.join(' | ')}`)
    }
  } else if (volunteerType) {
    fail(`volunteer_type is only valid for role "volunteer" (got role "${role}")`)
  }

  const unknownAreas = areas.filter((a) => !AREAS.includes(a))
  if (unknownAreas.length) {
    fail(`Unknown specific_areas: ${unknownAreas.join(', ')} — see --list-areas`)
  }
  if (password.length < 6) fail('Password must be at least 6 characters (Firebase minimum)')

  return { name, email, role, volunteerType, areas, isMember, password }
}

function splitList(value) {
  return (value ?? '')
    .split(/[;,|]/)
    .map((s) => s.trim().toLowerCase())
    .filter(Boolean)
}

function parseBool(value) {
  const v = (value ?? '').trim().toLowerCase()
  if (!v) return false
  if (['true', '1', 'yes', 'y', 'si', 'sí', 'x'].includes(v)) return true
  if (['false', '0', 'no', 'n'].includes(v)) return false
  throw new Error(`Cannot read "${value}" as a boolean`)
}

/** Readable temporary password — the user is forced to change it on first sign-in. */
function generatePassword() {
  const alphabet = 'abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  return Array.from({ length: 12 }, () => alphabet[randomInt(alphabet.length)]).join('')
}

/** Minimal RFC-4180 reader: quoted fields, embedded commas/newlines, "" escapes. */
function parseCsv(text) {
  const rows = [[]]
  let field = ''
  let quoted = false

  for (let i = 0; i < text.length; i++) {
    const char = text[i]
    if (quoted) {
      if (char === '"') {
        if (text[i + 1] === '"') { field += '"'; i++ } else { quoted = false }
      } else {
        field += char
      }
      continue
    }
    if (char === '"') { quoted = true }
    else if (char === ',') { rows.at(-1).push(field); field = '' }
    else if (char === '\n' || char === '\r') {
      if (char === '\r' && text[i + 1] === '\n') i++
      rows.at(-1).push(field); field = ''; rows.push([])
    } else { field += char }
  }
  rows.at(-1).push(field)

  const cells = rows.filter((r) => r.some((c) => c.trim() !== ''))
  if (!cells.length) throw new Error('CSV is empty')

  const header = cells[0].map((h) => h.trim().toLowerCase().replace(/\s+/g, '_'))
  return cells.slice(1).map((row) =>
    Object.fromEntries(header.map((key, i) => [key, row[i] ?? ''])),
  )
}

function readInput() {
  if (flags.csv) {
    const rows = parseCsv(readFileSync(flags.csv, 'utf8'))
    return rows.map((row, i) => normalise(row, i))
  }
  return [normalise({
    name: flags.name,
    email: flags.email,
    role: flags.role,
    volunteer_type: flags['volunteer-type'],
    specific_areas: flags.areas,
    is_member: String(flags.member),
    password: flags.password,
  }, null)]
}

// --- firebase ---------------------------------------------------------------

function connect() {
  const keyFile = flags['key-file'] ?? process.env.GOOGLE_APPLICATION_CREDENTIALS
  let credential
  let how

  if (keyFile) {
    credential = cert(JSON.parse(readFileSync(keyFile, 'utf8')))
    how = `service-account key (${keyFile})`
  } else {
    credential = applicationDefault()
    how = 'gcloud application-default credentials'
  }

  initializeApp({ credential, projectId })
  return { auth: getAuth(), db: getFirestore(), how }
}

async function createUser({ auth, db }, user) {
  let uid
  let reusedExisting = false

  try {
    uid = (await auth.createUser({
      email: user.email,
      password: user.password,
      displayName: user.name,
      emailVerified: false,
    })).uid
  } catch (error) {
    if (error.code !== 'auth/email-already-exists') throw error
    if (!flags.update) {
      throw new Error(`${user.email} already exists in Auth — pass --update to rewrite its Firestore doc`)
    }
    uid = (await auth.getUserByEmail(user.email)).uid
    reusedExisting = true
  }

  // onboarding_completed: false sends the user through CreatePassword on first
  // sign-in, where they replace the temporary password above.
  // Exactly the fields FirestoreUser declares — no extras, so the app's
  // kotlinx-serialization decode of the doc stays predictable.
  const doc = {
    id: uid,
    name: user.name,
    email: user.email,
    role: user.role,
    volunteer_type: user.volunteerType,
    onboarding_completed: false,
    specific_areas: user.areas,
    is_member: user.isMember,
  }

  try {
    await db.collection('users').doc(uid).set(doc, { merge: reusedExisting })
  } catch (error) {
    // Don't leave an Auth account with no profile behind.
    if (!reusedExisting) {
      await auth.deleteUser(uid).catch(() => {})
      throw new Error(`Firestore write failed, rolled back the Auth user: ${error.message}`)
    }
    throw error
  }

  return { uid, reusedExisting }
}

// --- run --------------------------------------------------------------------

let users
try {
  users = readInput()
} catch (error) {
  exitWith(`✗ ${error.message}`, 1)
}

console.log(`Project: ${projectId}`)

if (flags['dry-run']) {
  console.log('Dry run — nothing will be written.\n')
  for (const u of users) {
    console.log(`  ${u.email}  ${u.role}${u.volunteerType ? `/${u.volunteerType}` : ''}` +
      `  member=${u.isMember}  areas=[${u.areas.join(', ')}]  name="${u.name}"`)
  }
  console.log(`\n${users.length} user(s) validated.`)
  process.exit(0)
}

const firebase = connect()
console.log(`Auth:    ${firebase.how}\n`)

const created = []
const failed = []

for (const user of users) {
  try {
    const { uid, reusedExisting } = await createUser(firebase, user)
    created.push({ ...user, uid })
    console.log(`✓ ${reusedExisting ? 'updated' : 'created'} ${user.email}  (uid ${uid})`)
  } catch (error) {
    failed.push({ email: user.email, message: error.message })
    console.error(`✗ ${user.email}: ${error.message}`)
  }
}

if (created.length) {
  console.log('\nTemporary passwords — share over a private channel, they are not stored anywhere:')
  console.log('email,password')
  for (const u of created) console.log(`${u.email},${u.password}`)
}

console.log(`\n${created.length} ok, ${failed.length} failed.`)
process.exit(failed.length ? 1 : 0)

function exitWith(message, code) {
  ;(code === 0 ? console.log : console.error)(message)
  process.exit(code)
}
