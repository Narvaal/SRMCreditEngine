import { useEffect, type ReactNode } from 'react'

interface ModalProps {
  titulo: string
  /** Fechar por Esc, clique no overlay ou pelos botões do conteúdo. */
  onFechar: () => void
  children: ReactNode
}

export function Modal({ titulo, onFechar, children }: ModalProps) {
  useEffect(() => {
    function aoTeclar(evento: KeyboardEvent) {
      if (evento.key === 'Escape') onFechar()
    }
    document.addEventListener('keydown', aoTeclar)
    return () => document.removeEventListener('keydown', aoTeclar)
  }, [onFechar])

  return (
    <div
      data-testid="modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      onClick={onFechar}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={titulo}
        className="w-full max-w-md rounded-lg border border-border bg-surface p-6 shadow-xl"
        onClick={(evento) => evento.stopPropagation()}
      >
        <h2 className="mb-4 text-lg font-semibold text-ink">{titulo}</h2>
        {children}
      </div>
    </div>
  )
}
