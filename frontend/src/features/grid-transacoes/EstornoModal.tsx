import type { ExtratoLiquidacaoLinha } from '../../api/types'
import { Button, Modal } from '../../components/ui'
import { TransacaoDetalhes } from './TransacaoDetalhes'

interface EstornoModalProps {
  linha: ExtratoLiquidacaoLinha
  /** Confirmar fecha o modal na hora — o resultado (sucesso/erro) vai pro toast da página. */
  onConfirmar: () => void
  onFechar: () => void
}

/** Confirmação de estorno com os dados completos da transação. */
export function EstornoModal({ linha, onConfirmar, onFechar }: EstornoModalProps) {
  return (
    <Modal titulo="Estornar liquidação" onFechar={onFechar}>
      <TransacaoDetalhes linha={linha} />

      <p className="mt-3 font-mono text-xs text-ink-faint">id {linha.id}</p>

      <div className="mt-6 flex justify-end gap-2">
        <Button variante="ghost" onClick={onFechar}>
          Cancelar
        </Button>
        <Button onClick={onConfirmar}>Confirmar estorno</Button>
      </div>
    </Modal>
  )
}
