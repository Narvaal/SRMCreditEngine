import type { ReactNode } from 'react'

type Tom = 'brand' | 'danger' | 'neutral'

const tons: Record<Tom, string> = {
  brand: 'bg-brand-600/15 text-brand-400',
  danger: 'bg-danger-bg text-danger',
  neutral: 'bg-surface-muted text-ink-muted',
}

export function Badge({ children, tom = 'neutral' }: { children: ReactNode; tom?: Tom }) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${tons[tom]}`}>
      {children}
    </span>
  )
}
