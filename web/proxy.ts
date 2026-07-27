import { NextResponse, type NextRequest } from 'next/server'

/**
 * Next 16 renamed middleware to proxy. This is an OPTIMISTIC check only — it bounces
 * requests that carry no session cookie so unauthenticated visitors land on the login
 * page instead of a flash of empty dashboard.
 *
 * It is NOT access control. A cookie proves nothing about whether the account is still
 * on the allowlist or which areas it may see; that is decided per request by
 * requireAdmin() / visibleAreas() in lib/authz.ts, against the database.
 */
export function proxy(request: NextRequest) {
  const hasSession =
    request.cookies.has('authjs.session-token') ||
    request.cookies.has('__Secure-authjs.session-token')

  if (!hasSession) {
    const login = new URL('/login', request.url)
    login.searchParams.set('from', request.nextUrl.pathname)
    return NextResponse.redirect(login)
  }

  return NextResponse.next()
}

export const config = {
  matcher: [
    /**
     * Everything except the login page, the auth endpoints, the sync endpoint (which
     * authenticates with a shared key from cron, not a cookie), and static assets.
     */
    '/((?!login|api/auth|api/sync|_next/static|_next/image|favicon.ico).*)',
  ],
}
