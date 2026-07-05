import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/httpClient'
import type { ExtratoLiquidacaoLinha } from '../../api/types'
import { EstornoModal } from './EstornoModal'

const linha: ExtratoLiquidacaoLinha = {
  id: 'liq-1',
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

function renderModal(props: Partial<Parameters<typeof EstornoModal>[0]> = {}) {
  const padrao = {
    linha,
    estornando: false,
    sucesso: false,
    erro: null,
    onConfirmar: vi.fn(),
    onFechar: vi.fn(),
  }
  const finais = { ...padrao, ...props }
  render(<EstornoModal {...finais} />)
  return finais
}

describe('EstornoModal', () => {
  it('mostra os dados completos da transação', () => {
    renderModal()

    expect(screen.getByRole('dialog', { name: 'Estornar liquidação' })).toBeInTheDocument()
    expect(screen.getByText('Empresa Teste Ltda')).toBeInTheDocument()
    expect(screen.getByText('LIQUIDACAO')).toBeInTheDocument()
    expect(screen.getByText('BRL → BRL')).toBeInTheDocument()
    expect(screen.getByText(/1\.000,00/)).toBeInTheDocument()
    expect(screen.getByText(/900,00/)).toBeInTheDocument()
    expect(screen.getByText('10.00%')).toBeInTheDocument()
    expect(screen.getByText(/liq-1/)).toBeInTheDocument()
  })

  it('não mostra deságio em operação cross-currency', () => {
    renderModal({ linha: { ...linha, moedaPagamento: 'USD' } })

    expect(screen.queryByText('Deságio')).not.toBeInTheDocument()
  })

  it('confirmar chama onConfirmar; cancelar chama onFechar sem confirmar', async () => {
    const { onConfirmar, onFechar } = renderModal()
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: 'Cancelar' }))
    expect(onFechar).toHaveBeenCalledTimes(1)
    expect(onConfirmar).not.toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: 'Confirmar estorno' }))
    expect(onConfirmar).toHaveBeenCalledTimes(1)
  })

  it('desabilita os botões enquanto o estorno está em voo', () => {
    renderModal({ estornando: true })

    expect(screen.getByRole('button', { name: 'Estornando...' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled()
  })

  it('sucesso mostra a mensagem e troca os botões por Fechar', async () => {
    const { onFechar } = renderModal({ sucesso: true })

    expect(screen.getByText('Liquidação estornada com sucesso.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Confirmar estorno' })).not.toBeInTheDocument()

    await userEvent.setup().click(screen.getByRole('button', { name: 'Fechar' }))
    expect(onFechar).toHaveBeenCalled()
  })

  it('erro da API aparece dentro do modal, mantendo confirmar disponível pra nova tentativa', () => {
    renderModal({
      erro: new ApiError({
        timestamp: '2026-07-05T12:00:00Z',
        status: 409,
        codigo: 'ESTORNO_INVALIDO',
        mensagem: 'Não é possível estornar a liquidação liq-1: já foi estornada anteriormente',
        path: '/api/liquidacoes/liq-1/estorno',
        camposInvalidos: [],
      }),
    })

    expect(screen.getByText(/já foi estornada anteriormente/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Confirmar estorno' })).toBeInTheDocument()
  })

  it('Esc e clique no overlay fecham o modal', async () => {
    const { onFechar } = renderModal()
    const user = userEvent.setup()

    await user.keyboard('{Escape}')
    expect(onFechar).toHaveBeenCalledTimes(1)

    await user.click(screen.getByTestId('modal-overlay'))
    expect(onFechar).toHaveBeenCalledTimes(2)
  })
})
