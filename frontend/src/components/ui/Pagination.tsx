import { Button } from './Button'

interface PaginationProps {
  page: number // 0-based
  totalPages: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null

  return (
    <div className="flex items-center justify-between px-1 py-3">
      <span className="text-xs text-ink-muted">
        Página {page + 1} de {totalPages}
      </span>
      <div className="flex gap-2">
        <Button variante="secondary" disabled={page === 0} onClick={() => onPageChange(page - 1)}>
          Anterior
        </Button>
        <Button variante="secondary" disabled={page + 1 >= totalPages} onClick={() => onPageChange(page + 1)}>
          Próxima
        </Button>
      </div>
    </div>
  )
}
