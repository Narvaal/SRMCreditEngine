import { Fragment, useState } from 'react'
import type { ExtratoLiquidacaoLinha } from '../../api/types'
import { Badge, Button, Table, TableBody, TableHead, Td, Th } from '../../components/ui'
import type { TransacaoAgrupada } from '../../domain/extratoAgrupado'
import {
  calcularDesagioPercentual,
  formatarDataHora,
  formatarMoeda,
  formatarPercentual,
  rotuloTipoTransacao,
} from '../../lib/formatters'
import { TransacaoDetalhes } from './TransacaoDetalhes'

interface TransacoesTableProps {
  transacoes: TransacaoAgrupada[]
  /** Abre o modal de confirmação com os dados completos da linha — quem estorna é a página. */
  onEstornar: (linha: ExtratoLiquidacaoLinha) => void
  /** true enquanto um estorno está em voo — trava todos os botões pra não haver 2 transações. */
  estornoEmAndamento?: boolean
}

const TOTAL_COLUNAS = 9

export function TransacoesTable({ transacoes, onEstornar, estornoEmAndamento = false }: TransacoesTableProps) {
  // Uma linha expandida por vez — estado puramente de apresentação, por isso vive aqui.
  const [expandidaId, setExpandidaId] = useState<string | null>(null)

  if (transacoes.length === 0) {
    return <p className="py-12 text-center text-sm text-ink-muted">Nenhuma transação encontrada para os filtros selecionados.</p>
  }

  function renderAcao(linha: ExtratoLiquidacaoLinha) {
    if (linha.tipo !== 'LIQUIDACAO') {
      return <span className="text-ink-faint">—</span>
    }
    if (linha.estornada) {
      return <Badge tom="neutral">Estornada</Badge>
    }
    return (
      <Button
        variante="secondary"
        className="px-2 py-1 text-xs"
        disabled={estornoEmAndamento}
        onClick={() => onEstornar(linha)}
      >
        Estornar
      </Button>
    )
  }

  return (
    <Table>
      <TableHead>
        <tr>
          <Th className="w-8">
            <span className="sr-only">Detalhes</span>
          </Th>
          <Th>Data</Th>
          <Th>Cedente</Th>
          <Th>Tipo</Th>
          <Th>Moeda</Th>
          <Th className="text-right">Valor bruto</Th>
          <Th className="text-right">Valor líquido</Th>
          <Th className="text-right">Taxa</Th>
          <Th className="text-right">Ações</Th>
        </tr>
      </TableHead>
      <TableBody>
        {transacoes.map(({ exibida, original }) => {
          const expandida = expandidaId === exibida.id
          return (
            <Fragment key={exibida.id}>
              <tr className="hover:bg-surface-muted/60">
                <Td className="pr-0">
                  {original && (
                    <button
                      type="button"
                      aria-expanded={expandida}
                      aria-label={expandida ? 'Recolher operação original' : 'Exibir operação original'}
                      className="text-xs text-ink-muted transition-colors hover:text-ink"
                      onClick={() => setExpandidaId(expandida ? null : exibida.id)}
                    >
                      {expandida ? '▼' : '▶'}
                    </button>
                  )}
                </Td>
                <Td>{formatarDataHora(exibida.criadoEm)}</Td>
                <Td>{exibida.cedenteNome}</Td>
                <Td>
                  <Badge tom={exibida.tipo === 'ESTORNO' ? 'danger' : 'brand'}>{rotuloTipoTransacao(exibida.tipo)}</Badge>
                </Td>
                <Td>
                  {exibida.moedaTitulo} → {exibida.moedaPagamento}
                </Td>
                <Td className="tabular-nums text-right">{formatarMoeda(exibida.valorFace, exibida.moedaTitulo)}</Td>
                <Td className="tabular-nums text-right">{formatarMoeda(exibida.valorLiquido, exibida.moedaPagamento)}</Td>
                <Td className="tabular-nums text-right text-ink-muted">
                  {/* Taxa só faz sentido comparando valores na mesma moeda — em conversão cross-currency,
                      valorFace e valorLiquido estão em moedas diferentes e a razão entre eles não representa desconto. */}
                  {exibida.moedaTitulo === exibida.moedaPagamento
                    ? formatarPercentual(calcularDesagioPercentual(exibida.valorFace, exibida.valorLiquido))
                    : '—'}
                </Td>
                <Td className="text-right">{renderAcao(exibida)}</Td>
              </tr>
              {expandida && original && (
                <tr className="bg-surface-muted/40">
                  <td colSpan={TOTAL_COLUNAS} className="px-4 py-3">
                    <div className="ml-8 flex flex-col gap-2">
                      <span className="text-xs tracking-wide text-ink-muted uppercase">Operação original</span>
                      <TransacaoDetalhes linha={original} />
                    </div>
                  </td>
                </tr>
              )}
            </Fragment>
          )
        })}
      </TableBody>
    </Table>
  )
}
