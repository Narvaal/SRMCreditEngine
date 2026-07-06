import { forwardRef, type InputHTMLAttributes } from 'react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  /** Texto fixo à esquerda dentro do campo (ex.: símbolo da moeda "R$"). */
  prefixo?: string
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { label, error, prefixo, id, className = '', ...props },
  ref,
) {
  const inputId = id ?? props.name
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={inputId} className="text-sm text-ink-muted">
        {label}
      </label>
      <div className="relative">
        {prefixo && (
          <span className="pointer-events-none absolute inset-y-0 left-3 flex items-center text-sm text-ink-muted">
            {prefixo}
          </span>
        )}
        <input
          id={inputId}
          ref={ref}
          className={`w-full rounded-md border bg-surface-muted py-2 pr-3 text-sm text-ink placeholder:text-ink-faint focus:ring-2 focus:ring-brand-500 focus:outline-none ${
            prefixo ? 'pl-11' : 'pl-3'
          } ${error ? 'border-danger' : 'border-border-strong'} ${className}`}
          {...props}
        />
      </div>
      {error && <span className="text-xs text-danger">{error}</span>}
    </div>
  )
})
