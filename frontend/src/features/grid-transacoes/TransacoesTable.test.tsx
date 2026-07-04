import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { ExtratoLiquidacaoLinha } from '../../api/types'
import { TransacoesTable } from './TransacoesTable'

const linhaBase: ExtratoLiquidacaoLinha = {
  id: '1',
  recebivelId: 'r1',
  cedenteId: 'c1',
  cedenteNome: 'Empresa Teste Ltda',
  tipo: 'LIQUIDACAO',
  moedaTitulo: 'BRL',
  moedaPagamento: 'BRL',
  valorFace: 1000,
  valorLiquido: 900,
  criadoEm: '2026-07-03T12:00:00Z',
}

describe('TransacoesTable', () => {
  it('mostra mensagem quando não há linhas', () => {
    render(<TransacoesTable linhas={[]} />)

    expect(screen.getByText(/nenhuma transação encontrada/i)).toBeInTheDocument()
  })

  it('renderiza uma linha por transação, com o cedente e o tipo', () => {
    render(<TransacoesTable linhas={[linhaBase]} />)

    expect(screen.getByText('Empresa Teste Ltda')).toBeInTheDocument()
    expect(screen.getByText('LIQUIDACAO')).toBeInTheDocument()
  })

  it('diferencia ESTORNO de LIQUIDACAO visualmente (badge)', () => {
    render(<TransacoesTable linhas={[{ ...linhaBase, id: '2', tipo: 'ESTORNO' }]} />)

    expect(screen.getByText('ESTORNO')).toBeInTheDocument()
  })

  it('não calcula deságio % quando moedaTitulo difere de moedaPagamento (comparação sem sentido entre moedas)', () => {
    render(
      <TransacoesTable
        linhas={[{ ...linhaBase, id: '3', moedaTitulo: 'BRL', moedaPagamento: 'USD', valorFace: 10000, valorLiquido: 1742.12 }]}
      />,
    )

    expect(screen.getByText('—')).toBeInTheDocument()
    expect(screen.queryByText('82.58%')).not.toBeInTheDocument()
  })

  it('calcula deságio % normalmente quando a moeda é a mesma', () => {
    render(<TransacoesTable linhas={[{ ...linhaBase, id: '4', valorFace: 1000, valorLiquido: 900 }]} />)

    expect(screen.getByText('10.00%')).toBeInTheDocument()
  })
})
