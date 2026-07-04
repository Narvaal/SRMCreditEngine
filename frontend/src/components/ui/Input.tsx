import { forwardRef, type InputHTMLAttributes } from 'react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { label, error, id, className = '', ...props },
  ref,
) {
  const inputId = id ?? props.name
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={inputId} className="text-sm text-ink-muted">
        {label}
      </label>
      <input
        id={inputId}
        ref={ref}
        className={`rounded-md border bg-surface-muted px-3 py-2 text-sm text-ink placeholder:text-ink-faint focus:ring-2 focus:ring-brand-500 focus:outline-none ${
          error ? 'border-danger' : 'border-border-strong'
        } ${className}`}
        {...props}
      />
      {error && <span className="text-xs text-danger">{error}</span>}
    </div>
  )
})
