import type { ExtratoLiquidacaoLinha } from '../../api/types'
import { Badge } from '../../components/ui'
import {
  calcularDesagioPercentual,
  formatarDataHora,
  formatarMoeda,
  formatarPercentual,
  rotuloTipoTransacao,
} from '../../lib/formatters'

/**
 * Ficha compacta de uma transação — reutilizada pelo modal de estorno e pela linha expandida
 * da tabela ("Operação original").
 */
export function TransacaoDetalhes({ linha }: { linha: ExtratoLiquidacaoLinha }) {
  return (
    <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-1.5 text-sm">
      <dt className="text-ink-muted">Tipo</dt>
      <dd>
        <Badge tom={linha.tipo === 'ESTORNO' ? 'danger' : 'brand'}>{rotuloTipoTransacao(linha.tipo)}</Badge>
      </dd>

      <dt className="text-ink-muted">Data</dt>
      <dd>{formatarDataHora(linha.criadoEm)}</dd>

      <dt className="text-ink-muted">Cedente</dt>
      <dd>{linha.cedenteNome}</dd>

      <dt className="text-ink-muted">Moeda</dt>
      <dd>
        {linha.moedaTitulo} → {linha.moedaPagamento}
      </dd>

      <dt className="text-ink-muted">Valor bruto</dt>
      <dd className="tabular-nums">{formatarMoeda(linha.valorFace, linha.moedaTitulo)}</dd>

      <dt className="text-ink-muted">Valor líquido</dt>
      <dd className="tabular-nums">{formatarMoeda(linha.valorLiquido, linha.moedaPagamento)}</dd>

      {/* Deságio sobre o valor presente (mesma moeda do título) — vale também em cross-currency. */}
      <dt className="text-ink-muted">Taxa</dt>
      <dd className="tabular-nums">
        {formatarPercentual(calcularDesagioPercentual(linha.valorFace, linha.valorPresente))}
      </dd>
    </dl>
  )
}
