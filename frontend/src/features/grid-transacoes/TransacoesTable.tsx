import type { ExtratoLiquidacaoLinha } from '../../api/types'
import { Badge, Button, Table, TableBody, TableHead, TableRow, Td, Th } from '../../components/ui'
import { calcularDesagioPercentual, formatarDataHora, formatarMoeda, formatarPercentual } from '../../lib/formatters'

interface TransacoesTableProps {
  linhas: ExtratoLiquidacaoLinha[]
  /** Abre o modal de confirmação com os dados completos da linha — quem estorna é a página. */
  onEstornar: (linha: ExtratoLiquidacaoLinha) => void
}

export function TransacoesTable({ linhas, onEstornar }: TransacoesTableProps) {
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
    return (
      <Button variante="secondary" className="px-2 py-1 text-xs" onClick={() => onEstornar(linha)}>
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
