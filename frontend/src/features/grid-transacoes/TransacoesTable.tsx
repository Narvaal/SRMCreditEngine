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

interface TransacoesTableProps {
  transacoes: TransacaoAgrupada[]
  /** Abre o modal de confirmação com os dados completos da linha — quem estorna é a página. */
  onEstornar: (linha: ExtratoLiquidacaoLinha) => void
  /** true enquanto um estorno está em voo — trava todos os botões pra não haver 2 transações. */
  estornoEmAndamento?: boolean
}

export function TransacoesTable({ transacoes, onEstornar, estornoEmAndamento = false }: TransacoesTableProps) {
  // Expansão independente por linha — estado puramente de apresentação, por isso vive aqui.
  const [expandidas, setExpandidas] = useState<ReadonlySet<string>>(new Set())

  if (transacoes.length === 0) {
    return <p className="py-12 text-center text-sm text-ink-muted">Nenhuma transação encontrada para os filtros selecionados.</p>
  }

  function alternarExpandida(id: string) {
    setExpandidas((atual) => {
      const proximo = new Set(atual)
      if (!proximo.delete(id)) {
        proximo.add(id)
      }
      return proximo
    })
  }

  function renderAcao(linha: ExtratoLiquidacaoLinha, original: ExtratoLiquidacaoLinha | undefined, expandida: boolean) {
    // Toda liquidação exibida ainda não foi estornada (as estornadas não vêm no
    // extrato; a linha do estorno as representa).
    if (linha.tipo === 'LIQUIDACAO') {
      return (
        <Button
          variante="primary"
          className="px-2 py-1 text-xs"
          disabled={estornoEmAndamento}
          onClick={() => onEstornar(linha)}
        >
          Estornar
        </Button>
      )
    }
    // Estorno com referência: consulta da operação original. Legados sem referência não têm ação.
    // Variante invertida em relação ao Estornar pra diferenciar consulta de ação transacional.
    if (original) {
      return (
        <Button
          variante="secondary"
          className="px-2 py-1 text-xs"
          aria-expanded={expandida}
          onClick={() => alternarExpandida(linha.id)}
        >
          {expandida ? 'Recolher' : 'Ver origem'}
        </Button>
      )
    }
    return <span className="text-ink-faint">—</span>
  }

  return (
    <Table>
      <TableHead>
        <tr>
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
          const expandida = expandidas.has(exibida.id)
          return (
            <Fragment key={exibida.id}>
              <tr className="hover:bg-surface-muted/60">
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
                  {/* Deságio calculado sobre o valor presente (mesma moeda do título) — vale
                      também em operação cross-currency, onde o valor líquido está em outra moeda. */}
                  {formatarPercentual(calcularDesagioPercentual(exibida.valorFace, exibida.valorPresente))}
                </Td>
                <Td className="text-right">{renderAcao(exibida, original, expandida)}</Td>
              </tr>
              {expandida && original && (
                <tr className="bg-surface-muted">
                  <Td>{formatarDataHora(original.criadoEm)}</Td>
                  <Td>{original.cedenteNome}</Td>
                  <Td>
                    <Badge tom="brand">{rotuloTipoTransacao(original.tipo)}</Badge>
                  </Td>
                  <Td>
                    {original.moedaTitulo} → {original.moedaPagamento}
                  </Td>
                  <Td className="tabular-nums text-right">{formatarMoeda(original.valorFace, original.moedaTitulo)}</Td>
                  <Td className="tabular-nums text-right">{formatarMoeda(original.valorLiquido, original.moedaPagamento)}</Td>
                  <Td className="tabular-nums text-right text-ink-muted">
                    {formatarPercentual(calcularDesagioPercentual(original.valorFace, original.valorPresente))}
                  </Td>
                  {/* A original já foi estornada — nenhuma ação disponível; a seta liga ao estorno acima. */}
                  <Td className="text-center">
                    <span aria-hidden className="text-lg text-ink-faint">
                      ↲
                    </span>
                    <span className="sr-only">Operação original</span>
                  </Td>
                </tr>
              )}
            </Fragment>
          )
        })}
      </TableBody>
    </Table>
  )
}
