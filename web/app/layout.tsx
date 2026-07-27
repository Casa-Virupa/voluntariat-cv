import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'Voluntariat · Casa Virupa',
  description: 'Panell de coordinació de voluntariat',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ca" className="h-full antialiased">
      <body className="min-h-full">{children}</body>
    </html>
  )
}
