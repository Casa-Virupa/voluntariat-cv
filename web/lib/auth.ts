import NextAuth from 'next-auth'
import Google from 'next-auth/providers/google'

import { isAllowed, normaliseEmail } from './admins.ts'

/**
 * Google sign-in restricted to the `admin_user` allowlist.
 *
 * The JWT deliberately carries nothing but the identity. Role and area scope are looked
 * up from the database on every request (see authz.ts) so that removing someone from the
 * allowlist locks them out immediately, instead of whenever their token happens to
 * expire.
 */
export const { handlers, auth, signIn, signOut } = NextAuth({
  providers: [Google],
  session: { strategy: 'jwt', maxAge: 60 * 60 },
  pages: { signIn: '/login', error: '/login' },
  callbacks: {
    signIn({ profile }) {
      // Google always sets email_verified for Workspace/Gmail accounts; refuse anything
      // else so an unverified address can never impersonate an allowlisted one.
      if (!profile?.email || profile.email_verified === false) return false
      return isAllowed(profile.email)
    },
    jwt({ token }) {
      if (token.email) token.email = normaliseEmail(token.email)
      return token
    },
  },
})
