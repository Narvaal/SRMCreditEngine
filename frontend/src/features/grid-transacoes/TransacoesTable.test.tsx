import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { ExtratoLiquidacaoLinha } from '../../api/types'
import { agruparEstornosComOriginal } from '../../domain/extratoAgrupado'
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
  valorPresente: 900,
  valorLiquido: 900,
  criadoEm: '2026-07-03T12:00:00Z',
  liquidacaoEstornadaId: null,
  liquidacaoEstornadaCriadoEm: null,
}

function paraTabela(linhas: ExtratoLiquidacaoLinha[]) {
  return agruparEstornosComOriginal(linhas)
}

describe('TransacoesTable', () => {
  it('mostra mensagem quando não há linhas', () => {
    render(<TransacoesTable transacoes={[]} onEstornar={vi.fn()} />)

    expect(screen.getByText(/nenhuma transação encontrada/i)).toBeInTheDocument()
  })

  it('renderiza uma linha por transação, com o cedente e o tipo humanizado', () => {
    render(<TransacoesTable transacoes={paraTabela([linhaBase])} onEstornar={vi.fn()} />)

    expect(screen.getByText('Empresa Teste Ltda')).toBeInTheDocument()
    expect(screen.getByText('Liquidação')).toBeInTheDocument()
  })

  it('em cross-currency a taxa usa o valor presente (mesma moeda do título), não o líquido convertido', () => {
    render(
      <TransacoesTable
        transacoes={paraTabela([
          {
            ...linhaBase,
            id: '3',
            moedaTitulo: 'BRL',
            moedaPagamento: 'USD',
            valorFace: 10000,
            valorPresente: 9756.1,
            valorLiquido: 1742.12,
          },
        ])}
        onEstornar={vi.fn()}
      />,
    )

    expect(screen.getByText('2.44%')).toBeInTheDocument()
    // a razão ingênua face/líquido em moedas diferentes (82.58%) nunca deve aparecer
    expect(screen.queryByText('82.58%')).not.toBeInTheDocument()
  })

  it('calcula a taxa normalmente quando a moeda é a mesma', () => {
    render(
      <TransacoesTable
        transacoes={paraTabela([{ ...linhaBase, id: '4', valorFace: 1000, valorPresente: 900, valorLiquido: 900 }])}
        onEstornar={vi.fn()}
      />,
    )

    expect(screen.getByText('10.00%')).toBeInTheDocument()
  })

  it('mostra o botão Estornar só em LIQUIDACAO — estorno não é estornável', () => {
    render(
      <TransacoesTable
        transacoes={paraTabela([
          { ...linhaBase, id: 'liq' },
          { ...linhaBase, id: 'estorno', recebivelId: 'r-solo', tipo: 'ESTORNO' },
        ])}
        onEstornar={vi.fn()}
      />,
    )

    expect(screen.getAllByRole('button', { name: 'Estornar' })).toHaveLength(1)
  })

  it('clicar em Estornar entrega a linha completa pro chamador (que abre o modal)', async () => {
    const onEstornar = vi.fn()
    render(<TransacoesTable transacoes={paraTabela([linhaBase])} onEstornar={onEstornar} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Estornar' }))

    expect(onEstornar).toHaveBeenCalledWith(linhaBase)
  })

  it('estorno com referência tem Ver origem, que expande a operação original e vira Recolher', async () => {
    const estorno = {
      ...linhaBase,
      id: 'est',
      tipo: 'ESTORNO' as const,
      valorFace: 222.22,
      valorLiquido: 182.92,
      criadoEm: '2026-07-07T09:14:00Z',
      liquidacaoEstornadaId: 'liq',
      liquidacaoEstornadaCriadoEm: '2026-07-05T20:52:00Z',
    }
    render(<TransacoesTable transacoes={paraTabela([estorno])} onEstornar={vi.fn()} />)

    expect(screen.getAllByText('Estorno')).toHaveLength(1)

    // expande: aparece a operação original com a data real da liquidação
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: 'Ver origem' }))

    expect(screen.getByText('Operação original')).toBeInTheDocument()
    expect(screen.getByText('Origem')).toBeInTheDocument()
    expect(screen.getByText(/05\/07\/2026/)).toBeInTheDocument()

    // o botão vira Recolher e recolhe no segundo clique
    await user.click(screen.getByRole('button', { name: 'Recolher' }))
    expect(screen.queryByText('Operação original')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ver origem' })).toBeInTheDocument()
  })

  it('linha de origem usa um badge único Origem, sem repetir Liquidação', async () => {
    const estorno = { ...linhaBase, id: 'est', tipo: 'ESTORNO' as const, liquidacaoEstornadaId: 'liq' }
    render(<TransacoesTable transacoes={paraTabela([estorno])} onEstornar={vi.fn()} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Ver origem' }))
    expect(screen.getByText('Origem')).toBeInTheDocument()
    expect(screen.queryByText('Liquidação')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Recolher' }))
    expect(screen.queryByText('Origem')).not.toBeInTheDocument()
  })

  it('liquidações comuns e estornos legados sem referência não têm Ver origem', () => {
    render(
      <TransacoesTable
        transacoes={paraTabela([linhaBase, { ...linhaBase, id: 'est-solo', recebivelId: 'r-solo', tipo: 'ESTORNO' }])}
        onEstornar={vi.fn()}
      />,
    )

    expect(screen.queryByRole('button', { name: 'Ver origem' })).not.toBeInTheDocument()
  })

  it('linhas expandem de forma independente — várias podem ficar abertas ao mesmo tempo', async () => {
    const estornos = [
      { ...linhaBase, id: 'est1', tipo: 'ESTORNO' as const, liquidacaoEstornadaId: 'liq1' },
      { ...linhaBase, id: 'est2', tipo: 'ESTORNO' as const, liquidacaoEstornadaId: 'liq2' },
    ]
    render(<TransacoesTable transacoes={paraTabela(estornos)} onEstornar={vi.fn()} />)
    const user = userEvent.setup()

    const botoes = screen.getAllByRole('button', { name: 'Ver origem' })
    expect(botoes).toHaveLength(2)

    await user.click(botoes[0])
    expect(screen.getAllByText('Operação original')).toHaveLength(1)

    // expandir a segunda mantém a primeira aberta
    await user.click(screen.getByRole('button', { name: 'Ver origem' }))
    expect(screen.getAllByText('Operação original')).toHaveLength(2)
    expect(screen.getAllByRole('button', { name: 'Recolher' })).toHaveLength(2)

    // fechar uma não afeta a outra
    await user.click(screen.getAllByRole('button', { name: 'Recolher' })[0])
    expect(screen.getAllByText('Operação original')).toHaveLength(1)
  })
})
