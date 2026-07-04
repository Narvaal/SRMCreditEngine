import type { ButtonHTMLAttributes, ReactNode } from 'react'

type Variante = 'primary' | 'secondary' | 'ghost'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: Variante
  children: ReactNode
}

const variantes: Record<Variante, string> = {
  primary: 'bg-brand-600 text-canvas hover:bg-brand-700 focus-visible:outline-brand-400',
  secondary: 'bg-surface-muted text-ink border border-border-strong hover:border-brand-500',
  ghost: 'text-ink-muted hover:text-ink hover:bg-surface-muted',
}

export function Button({ variante = 'primary', className = '', children, ...props }: ButtonProps) {
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-md px-4 py-2 text-sm font-medium transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-50 ${variantes[variante]} ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}
