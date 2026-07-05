import { useState } from 'react'
import type { ExtratoLiquidacaoLinha } from '../../api/types'
import { Badge, Button, Table, TableBody, TableHead, TableRow, Td, Th } from '../../components/ui'
import { calcularDesagioPercentual, formatarDataHora, formatarMoeda, formatarPercentual } from '../../lib/formatters'

interface TransacoesTableProps {
  linhas: ExtratoLiquidacaoLinha[]
  onEstornar: (liquidacaoId: string) => void
  /** id da liquidação com estorno em voo — desabilita o botão da linha enquanto a mutação roda. */
  estornandoId?: string | null
}

export function TransacoesTable({ linhas, onEstornar, estornandoId = null }: TransacoesTableProps) {
  // Confirmação inline em dois cliques — estado puramente de apresentação, por isso vive aqui.
  const [confirmandoId, setConfirmandoId] = useState<string | null>(null)

  if (linhas.length === 0) {
    return <p className="py-12 text-center text-sm text-ink-muted">Nenhuma transação encontrada para os filtros selecionados.</p>
  }

  function renderAcao(linha: ExtratoLiquidacaoLinha) {
    if (linha.tipo !== 'LIQUIDACAO') {
      return <span className="text-ink-faint">—</span>
    }
    if (linha.estornada) {
      return <Badge tom="neutral">Estornada</Badge>
    }
    if (estornandoId === linha.id) {
      return (
        <Button variante="secondary" className="px-2 py-1 text-xs" disabled>
          Estornando...
        </Button>
      )
    }
    if (confirmandoId === linha.id) {
      return (
        <span className="inline-flex gap-2">
          <Button
            variante="secondary"
            className="px-2 py-1 text-xs text-danger"
            onClick={() => {
              setConfirmandoId(null)
              onEstornar(linha.id)
            }}
          >
            Confirmar estorno
          </Button>
          <Button variante="ghost" className="px-2 py-1 text-xs" onClick={() => setConfirmandoId(null)}>
            Cancelar
          </Button>
        </span>
      )
    }
    return (
      <Button variante="secondary" className="px-2 py-1 text-xs" onClick={() => setConfirmandoId(linha.id)}>
        Estornar
      </Button>
    )
  }

  return (
    <Table>
      <TableHead>
        <tr>
          <Th>Data</Th>
          <Th>Cedente</Th>
          <Th>Tipo</Th>
          <Th>Moeda</Th>
          <Th className="text-right">Valor de face</Th>
          <Th className="text-right">Valor líquido</Th>
          <Th className="text-right">Deságio</Th>
          <Th className="text-right">Ações</Th>
        </tr>
      </TableHead>
      <TableBody>
        {linhas.map((linha) => (
          <TableRow key={linha.id}>
            <Td>{formatarDataHora(linha.criadoEm)}</Td>
            <Td>{linha.cedenteNome}</Td>
            <Td>
              <Badge tom={linha.tipo === 'ESTORNO' ? 'danger' : 'brand'}>{linha.tipo}</Badge>
            </Td>
            <Td>
              {linha.moedaTitulo} → {linha.moedaPagamento}
            </Td>
            <Td className="tabular-nums text-right">{formatarMoeda(linha.valorFace, linha.moedaTitulo)}</Td>
            <Td className="tabular-nums text-right">{formatarMoeda(linha.valorLiquido, linha.moedaPagamento)}</Td>
            <Td className="tabular-nums text-right text-ink-muted">
              {/* Deságio só faz sentido comparando valores na mesma moeda — em conversão cross-currency,
                  valorFace e valorLiquido estão em moedas diferentes e a razão entre eles não representa desconto. */}
              {linha.moedaTitulo === linha.moedaPagamento
                ? formatarPercentual(calcularDesagioPercentual(linha.valorFace, linha.valorLiquido))
                : '—'}
            </Td>
            <Td className="text-right">{renderAcao(linha)}</Td>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}
