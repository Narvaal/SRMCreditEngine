import { forwardRef, type ReactNode, type SelectHTMLAttributes } from 'react'

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string
  error?: string
  children: ReactNode
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { label, error, id, className = '', children, ...props },
  ref,
) {
  const selectId = id ?? props.name
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={selectId} className="text-sm text-ink-muted">
        {label}
      </label>
      <select
        id={selectId}
        ref={ref}
        className={`rounded-md border bg-surface-muted px-3 py-2 text-sm text-ink focus:ring-2 focus:ring-brand-500 focus:outline-none ${
          error ? 'border-danger' : 'border-border-strong'
        } ${className}`}
        {...props}
      >
        {children}
      </select>
      {error && <span className="text-xs text-danger">{error}</span>}
    </div>
  )
})
