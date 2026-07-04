import type { ReactNode } from 'react'

type Tipo = 'success' | 'error' | 'info'

const estilos: Record<Tipo, string> = {
  success: 'border-brand-600/40 bg-brand-600/10 text-brand-400',
  error: 'border-danger/40 bg-danger-bg text-danger',
  info: 'border-border-strong bg-surface-muted text-ink-muted',
}

export function Alert({ tipo = 'info', children }: { tipo?: Tipo; children: ReactNode }) {
  return <div className={`rounded-md border px-4 py-3 text-sm ${estilos[tipo]}`}>{children}</div>
}
