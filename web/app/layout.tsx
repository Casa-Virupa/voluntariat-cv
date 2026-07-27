import type { Metadata } from 'next'
import localFont from 'next/font/local'
import './globals.css'

/**
 * The two Casa Virupa faces, self-hosted from the very files the mobile app ships in
 * shared/designsystem/src/commonMain/composeResources/font. Only the weights the app's
 * Typography.kt actually uses are loaded — regular, medium and bold, no italics.
 */
const kalice = localFont({
  src: [
    { path: './fonts/Kalice-Regular.otf', weight: '400', style: 'normal' },
    { path: './fonts/Kalice-Medium.otf', weight: '500', style: 'normal' },
    { path: './fonts/Kalice-Bold.otf', weight: '700', style: 'normal' },
  ],
  variable: '--font-kalice',
  display: 'swap',
})

const dmSans = localFont({
  src: [
    { path: './fonts/DMSans-Regular.ttf', weight: '400', style: 'normal' },
    { path: './fonts/DMSans-Medium.ttf', weight: '500', style: 'normal' },
    { path: './fonts/DMSans-Bold.ttf', weight: '700', style: 'normal' },
  ],
  variable: '--font-dm-sans',
  display: 'swap',
})

export const metadata: Metadata = {
  title: 'Voluntariat · Casa Virupa',
  description: 'Panell de coordinació de voluntariat',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ca" className={`h-full antialiased ${dmSans.variable} ${kalice.variable}`}>
      <body className="min-h-full">{children}</body>
    </html>
  )
}
