import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
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
  estornada: false,
}

describe('TransacoesTable', () => {
  it('mostra mensagem quando não há linhas', () => {
    render(<TransacoesTable linhas={[]} onEstornar={vi.fn()} />)

    expect(screen.getByText(/nenhuma transação encontrada/i)).toBeInTheDocument()
  })

  it('renderiza uma linha por transação, com o cedente e o tipo', () => {
    render(<TransacoesTable linhas={[linhaBase]} onEstornar={vi.fn()} />)

    expect(screen.getByText('Empresa Teste Ltda')).toBeInTheDocument()
    expect(screen.getByText('LIQUIDACAO')).toBeInTheDocument()
  })

  it('diferencia ESTORNO de LIQUIDACAO visualmente (badge)', () => {
    render(<TransacoesTable linhas={[{ ...linhaBase, id: '2', tipo: 'ESTORNO' }]} onEstornar={vi.fn()} />)

    expect(screen.getByText('ESTORNO')).toBeInTheDocument()
  })

  it('não calcula deságio % quando moedaTitulo difere de moedaPagamento (comparação sem sentido entre moedas)', () => {
    render(
      <TransacoesTable
        linhas={[{ ...linhaBase, id: '3', moedaTitulo: 'BRL', moedaPagamento: 'USD', valorFace: 10000, valorLiquido: 1742.12 }]}
        onEstornar={vi.fn()}
      />,
    )

    expect(screen.getAllByText('—').length).toBeGreaterThan(0)
    expect(screen.queryByText('82.58%')).not.toBeInTheDocument()
  })

  it('calcula deságio % normalmente quando a moeda é a mesma', () => {
    render(<TransacoesTable linhas={[{ ...linhaBase, id: '4', valorFace: 1000, valorLiquido: 900 }]} onEstornar={vi.fn()} />)

    expect(screen.getByText('10.00%')).toBeInTheDocument()
  })

  it('mostra o botão Estornar só em LIQUIDACAO não-estornada', () => {
    render(
      <TransacoesTable
        linhas={[
          { ...linhaBase, id: 'liq' },
          { ...linhaBase, id: 'liq-estornada', estornada: true },
          { ...linhaBase, id: 'estorno', tipo: 'ESTORNO' },
        ]}
        onEstornar={vi.fn()}
      />,
    )

    expect(screen.getAllByRole('button', { name: 'Estornar' })).toHaveLength(1)
    expect(screen.getByText('Estornada')).toBeInTheDocument()
  })

  it('clicar em Estornar entrega a linha completa pro chamador (que abre o modal)', async () => {
    const onEstornar = vi.fn()
    render(<TransacoesTable linhas={[linhaBase]} onEstornar={onEstornar} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Estornar' }))

    expect(onEstornar).toHaveBeenCalledWith(linhaBase)
  })
})
