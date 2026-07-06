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
  valorLiquido: 900,
  criadoEm: '2026-07-03T12:00:00Z',
  estornada: false,
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

  it('não calcula taxa quando moedaTitulo difere de moedaPagamento (comparação sem sentido entre moedas)', () => {
    render(
      <TransacoesTable
        transacoes={paraTabela([
          { ...linhaBase, id: '3', moedaTitulo: 'BRL', moedaPagamento: 'USD', valorFace: 10000, valorLiquido: 1742.12 },
        ])}
        onEstornar={vi.fn()}
      />,
    )

    expect(screen.getAllByText('—').length).toBeGreaterThan(0)
    expect(screen.queryByText('82.58%')).not.toBeInTheDocument()
  })

  it('calcula a taxa normalmente quando a moeda é a mesma', () => {
    render(
      <TransacoesTable
        transacoes={paraTabela([{ ...linhaBase, id: '4', valorFace: 1000, valorLiquido: 900 }])}
        onEstornar={vi.fn()}
      />,
    )

    expect(screen.getByText('10.00%')).toBeInTheDocument()
  })

  it('mostra o botão Estornar só em LIQUIDACAO não-estornada', () => {
    render(
      <TransacoesTable
        transacoes={paraTabela([
          { ...linhaBase, id: 'liq' },
          { ...linhaBase, id: 'liq-estornada', recebivelId: 'r-solo', estornada: true },
        ])}
        onEstornar={vi.fn()}
      />,
    )

    expect(screen.getAllByRole('button', { name: 'Estornar' })).toHaveLength(1)
    expect(screen.getByText('Estornada')).toBeInTheDocument()
  })

  it('clicar em Estornar entrega a linha completa pro chamador (que abre o modal)', async () => {
    const onEstornar = vi.fn()
    render(<TransacoesTable transacoes={paraTabela([linhaBase])} onEstornar={onEstornar} />)
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Estornar' }))

    expect(onEstornar).toHaveBeenCalledWith(linhaBase)
  })

  it('estorno pareado vira uma linha só, expansível, com os dados da liquidação original', async () => {
    const liquidacao = { ...linhaBase, id: 'liq', estornada: true, valorFace: 222.22, valorLiquido: 182.92 }
    const estorno = {
      ...linhaBase,
      id: 'est',
      tipo: 'ESTORNO' as const,
      valorFace: 222.22,
      valorLiquido: 182.92,
      criadoEm: '2026-07-07T09:14:00Z',
      liquidacaoEstornadaId: 'liq',
      liquidacaoEstornadaCriadoEm: linhaBase.criadoEm,
    }
    render(<TransacoesTable transacoes={paraTabela([estorno, liquidacao])} onEstornar={vi.fn()} />)

    // uma linha só, com o tipo do estorno e os valores da liquidação
    expect(screen.getAllByText('Estorno')).toHaveLength(1)
    expect(screen.queryByText('Estornada')).not.toBeInTheDocument()

    // expande: aparece a operação original
    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: 'Exibir operação original' }))

    expect(screen.getByText('Operação original')).toBeInTheDocument()
    expect(screen.getByText('Liquidação')).toBeInTheDocument()

    // recolhe no segundo clique
    await user.click(screen.getByRole('button', { name: 'Recolher operação original' }))
    expect(screen.queryByText('Operação original')).not.toBeInTheDocument()
  })

  it('liquidações comuns e estornos legados sem referência não têm seta de expandir', () => {
    render(
      <TransacoesTable
        transacoes={paraTabela([linhaBase, { ...linhaBase, id: 'est-solo', recebivelId: 'r-solo', tipo: 'ESTORNO' }])}
        onEstornar={vi.fn()}
      />,
    )

    expect(screen.queryByRole('button', { name: /operação original/i })).not.toBeInTheDocument()
  })

  it('estorno com referência mas sem a liquidação na página continua expansível', async () => {
    const estorno = {
      ...linhaBase,
      id: 'est-cross',
      tipo: 'ESTORNO' as const,
      liquidacaoEstornadaId: 'liq-outra-pagina',
      liquidacaoEstornadaCriadoEm: '2026-07-01T08:00:00Z',
    }
    render(<TransacoesTable transacoes={paraTabela([estorno])} onEstornar={vi.fn()} />)

    await userEvent.setup().click(screen.getByRole('button', { name: 'Exibir operação original' }))
    expect(screen.getByText('Operação original')).toBeInTheDocument()
  })

  it('apenas uma linha fica expandida por vez', async () => {
    const par1 = [
      { ...linhaBase, id: 'liq1', recebivelId: 'ra', estornada: true },
      { ...linhaBase, id: 'est1', recebivelId: 'ra', tipo: 'ESTORNO' as const, liquidacaoEstornadaId: 'liq1' },
    ]
    const par2 = [
      { ...linhaBase, id: 'liq2', recebivelId: 'rb', estornada: true },
      { ...linhaBase, id: 'est2', recebivelId: 'rb', tipo: 'ESTORNO' as const, liquidacaoEstornadaId: 'liq2' },
    ]
    render(<TransacoesTable transacoes={paraTabela([...par1, ...par2])} onEstornar={vi.fn()} />)
    const user = userEvent.setup()

    const setas = screen.getAllByRole('button', { name: 'Exibir operação original' })
    expect(setas).toHaveLength(2)

    await user.click(setas[0])
    expect(screen.getAllByText('Operação original')).toHaveLength(1)

    // expandir a segunda recolhe a primeira
    await user.click(screen.getByRole('button', { name: 'Exibir operação original' }))
    expect(screen.getAllByText('Operação original')).toHaveLength(1)
    expect(screen.getAllByRole('button', { name: 'Exibir operação original' })).toHaveLength(1)
  })
})
