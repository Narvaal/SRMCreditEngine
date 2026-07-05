import type { ExtratoLiquidacaoLinha } from '../../api/types'
import { Badge, Button, Modal } from '../../components/ui'
import { calcularDesagioPercentual, formatarDataHora, formatarMoeda, formatarPercentual } from '../../lib/formatters'

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
      <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
        <dt className="text-ink-muted">Data</dt>
        <dd>{formatarDataHora(linha.criadoEm)}</dd>

        <dt className="text-ink-muted">Cedente</dt>
        <dd>{linha.cedenteNome}</dd>

        <dt className="text-ink-muted">Tipo</dt>
        <dd>
          <Badge tom="brand">{linha.tipo}</Badge>
        </dd>

        <dt className="text-ink-muted">Moeda</dt>
        <dd>
          {linha.moedaTitulo} → {linha.moedaPagamento}
        </dd>

        <dt className="text-ink-muted">Valor de face</dt>
        <dd className="tabular-nums">{formatarMoeda(linha.valorFace, linha.moedaTitulo)}</dd>

        <dt className="text-ink-muted">Valor líquido</dt>
        <dd className="tabular-nums">{formatarMoeda(linha.valorLiquido, linha.moedaPagamento)}</dd>

        {/* Deságio só compara valores na mesma moeda — mesma regra da tabela. */}
        {linha.moedaTitulo === linha.moedaPagamento && (
          <>
            <dt className="text-ink-muted">Deságio</dt>
            <dd className="tabular-nums">
              {formatarPercentual(calcularDesagioPercentual(linha.valorFace, linha.valorLiquido))}
            </dd>
          </>
        )}
      </dl>

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
