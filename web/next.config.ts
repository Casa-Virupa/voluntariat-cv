import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  /**
   * Self-hosted on a VPS behind nginx: `standalone` emits a minimal server bundle plus
   * only the node_modules actually reached, so the deploy artefact stays small.
   */
  output: 'standalone',

  /**
   * Native modules must not be bundled — better-sqlite3 loads a .node binary, and
   * firebase-admin uses dynamic requires the bundler cannot follow.
   */
  serverExternalPackages: ['better-sqlite3', 'firebase-admin', 'exceljs'],

  /**
   * The live database must never travel inside a build artefact — on the VPS it lives on
   * a persistent volume, and shipping a stale copy could overwrite real data on deploy.
   * (`lib/db/index.ts` resolves its path from an env var, which makes the tracer pull in
   * the working directory; this is the documented way to scope that back down.)
   */
  outputFileTracingExcludes: {
    '*': ['./data/**', './.next/cache/**'],
  },
}

export default nextConfig
