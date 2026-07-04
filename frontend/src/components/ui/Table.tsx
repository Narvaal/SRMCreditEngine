import type { ReactNode, TdHTMLAttributes, ThHTMLAttributes } from 'react'

export function Table({ children }: { children: ReactNode }) {
  return (
    <div className="overflow-x-auto rounded-lg border border-border">
      <table className="w-full border-collapse text-sm">{children}</table>
    </div>
  )
}

export function TableHead({ children }: { children: ReactNode }) {
  return (
    <thead className="bg-surface-muted text-left text-xs tracking-wide text-ink-muted uppercase">{children}</thead>
  )
}

export function TableBody({ children }: { children: ReactNode }) {
  return <tbody className="divide-border divide-y">{children}</tbody>
}

export function TableRow({ children }: { children: ReactNode }) {
  return <tr className="hover:bg-surface-muted/60">{children}</tr>
}

export function Th({ children, ...props }: ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th className="px-4 py-3 font-medium" {...props}>
      {children}
    </th>
  )
}

export function Td({ children, className = '', ...props }: TdHTMLAttributes<HTMLTableCellElement>) {
  return (
    <td className={`px-4 py-3 text-ink ${className}`} {...props}>
      {children}
    </td>
  )
}
