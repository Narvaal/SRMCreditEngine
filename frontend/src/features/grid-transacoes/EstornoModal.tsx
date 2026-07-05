import type { ApiError } from '../../api/httpClient'
import type { ExtratoLiquidacaoLinha } from '../../api/types'
import { Alert, Badge, Button, Modal } from '../../components/ui'
import { calcularDesagioPercentual, formatarDataHora, formatarMoeda, formatarPercentual } from '../../lib/formatters'

interface EstornoModalProps {
  linha: ExtratoLiquidacaoLinha
  estornando: boolean
  sucesso: boolean
  erro: ApiError | null
  onConfirmar: () => void
  onFechar: () => void
}

/** Confirmação de estorno com os dados completos da transação; sucesso/erro aparecem aqui dentro. */
export function EstornoModal({ linha, estornando, sucesso, erro, onConfirmar, onFechar }: EstornoModalProps) {
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

      {sucesso && (
        <div className="mt-4">
          <Alert tipo="success">Liquidação estornada com sucesso.</Alert>
        </div>
      )}
      {!sucesso && erro && (
        <div className="mt-4">
          <Alert tipo="error">{erro.message || 'Não foi possível estornar a liquidação.'}</Alert>
        </div>
      )}

      <div className={`mt-6 flex gap-2 ${sucesso ? 'justify-center' : 'justify-end'}`}>
        {sucesso ? (
          <Button variante="ghost" onClick={onFechar}>
            Fechar
          </Button>
        ) : (
          <>
            <Button variante="ghost" onClick={onFechar} disabled={estornando}>
              Cancelar
            </Button>
            <Button onClick={onConfirmar} disabled={estornando}>
              {estornando ? 'Estornando...' : 'Confirmar estorno'}
            </Button>
          </>
        )}
      </div>
    </Modal>
  )
}
