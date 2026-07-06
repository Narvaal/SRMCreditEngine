import { useEffect, type ReactNode } from 'react'

interface ToastProps {
  tipo: 'success' | 'error'
  onFechar: () => void
  /** Auto-dismiss; 0 desliga (útil em teste). */
  duracaoMs?: number
  children: ReactNode
}

// Mesma paleta do Alert, mas com fundo sólido — o toast flutua sobre a tabela.
const tons: Record<ToastProps['tipo'], string> = {
  success: 'border-brand-600/60 bg-surface text-brand-400',
  error: 'border-danger/60 bg-surface text-danger',
}

/** Notificação flutuante de resultado — some sozinha, sem bloquear a página. */
export function Toast({ tipo, onFechar, duracaoMs = 5000, children }: ToastProps) {
  useEffect(() => {
    if (duracaoMs <= 0) return
    const timer = setTimeout(onFechar, duracaoMs)
    return () => clearTimeout(timer)
  }, [duracaoMs, onFechar])

  return (
    <div
      role="status"
      aria-live="polite"
      className={`fixed right-6 bottom-6 z-50 flex max-w-sm items-start gap-3 rounded-md border px-4 py-3 text-sm shadow-xl ${tons[tipo]}`}
    >
      <span className="flex-1">{children}</span>
      <button
        type="button"
        aria-label="Fechar notificação"
        className="text-current opacity-70 hover:opacity-100"
        onClick={onFechar}
      >
        ✕
      </button>
    </div>
  )
}
