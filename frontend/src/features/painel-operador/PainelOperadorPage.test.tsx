import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { catalogosApi } from '../../api/catalogos'
import { cedentesApi } from '../../api/cedentes'
import { recebiveisApi } from '../../api/recebiveis'
import type { LoteLiquidacaoResponse } from '../../api/types'
import { PainelOperadorPage } from './PainelOperadorPage'

vi.mock('../../api/cedentes', () => ({ cedentesApi: { listar: vi.fn(), criar: vi.fn() } }))
vi.mock('../../api/catalogos', () => ({
  catalogosApi: { listarMoedas: vi.fn(), listarTiposRecebivel: vi.fn() },
}))
vi.mock('../../api/recebiveis', () => ({ recebiveisApi: { simular: vi.fn(), enviarLote: vi.fn() } }))

function mockarCatalogos() {
  vi.mocked(cedentesApi.listar).mockResolvedValue([{ id: 'c1', nome: 'Acme Ltda', documento: '123' }])
  vi.mocked(catalogosApi.listarTiposRecebivel).mockResolvedValue([
    { codigo: 'DUPLICATA_MERCANTIL', nome: 'Duplicata Mercantil' },
  ])
  vi.mocked(catalogosApi.listarMoedas).mockResolvedValue([
    { codigo: 'BRL', nome: 'Real Brasileiro', casasDecimais: 2 },
    { codigo: 'USD', nome: 'Dólar Americano', casasDecimais: 2 },
  ])
  vi.mocked(recebiveisApi.simular).mockResolvedValue({
    valorFace: 1000,
    moedaTitulo: 'BRL',
    taxaBaseUsada: 0.01,
    spreadUsado: 0.015,
    prazoMesesUsado: 1,
    valorPresente: 975.61,
    moedaPagamento: 'BRL',
    taxaCambioUsada: null,
    valorLiquido: 975.61,
  })
}

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}

describe('PainelOperadorPage', () => {
  it('depois de carregar os catálogos, mostra o formulário e o placeholder de simulação', async () => {
    mockarCatalogos()
    render(<PainelOperadorPage />, { wrapper })

    expect(await screen.findByRole('option', { name: 'Acme Ltda' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Duplicata Mercantil' })).toBeInTheDocument()
    expect(screen.getByText(/preencha os dados do recebível/i)).toBeInTheDocument()
  })

  it('preencher o formulário e submeter com sucesso mostra o alerta de sucesso', async () => {
    mockarCatalogos()
    const resposta: LoteLiquidacaoResponse = {
      totalItens: 1,
      totalSucesso: 1,
      totalFalha: 0,
      itens: [{ sucesso: true, recebivelId: 'r1', liquidacao: null, codigoErro: null, mensagemErro: null }],
    }
    vi.mocked(recebiveisApi.enviarLote).mockResolvedValue(resposta)

    render(<PainelOperadorPage />, { wrapper })
    const user = userEvent.setup()

    await screen.findByRole('option', { name: 'Acme Ltda' })
    await user.selectOptions(screen.getByLabelText('Cedente'), 'c1')
    await user.selectOptions(screen.getByLabelText('Tipo de recebível'), 'DUPLICATA_MERCANTIL')
    await user.type(screen.getByLabelText('Valor de face'), '1000')
    fireEvent.change(screen.getByLabelText('Vencimento'), { target: { value: '2099-08-01' } })

    await user.click(screen.getByRole('button', { name: /liquidar recebível/i }))

    expect(await screen.findByText('Recebível liquidado com sucesso.')).toBeInTheDocument()
    await waitFor(() => expect(recebiveisApi.enviarLote).toHaveBeenCalledTimes(1))
  })

  it('cadastro de cedente inline adiciona o novo cedente e já o deixa selecionado', async () => {
    mockarCatalogos()
    vi.mocked(cedentesApi.criar).mockResolvedValue({ id: 'c-novo', nome: 'Nova Empresa SA', documento: '456' })
    // depois do cadastro, o catálogo invalidado passa a listar o novo cedente
    vi.mocked(cedentesApi.listar)
      .mockResolvedValueOnce([{ id: 'c1', nome: 'Acme Ltda', documento: '123' }])
      .mockResolvedValue([
        { id: 'c1', nome: 'Acme Ltda', documento: '123' },
        { id: 'c-novo', nome: 'Nova Empresa SA', documento: '456' },
      ])

    render(<PainelOperadorPage />, { wrapper })
    const user = userEvent.setup()
    await screen.findByRole('option', { name: 'Acme Ltda' })

    await user.click(screen.getByRole('button', { name: /cadastrar novo cedente/i }))
    await user.type(screen.getByLabelText('Nome'), 'Nova Empresa SA')
    await user.type(screen.getByLabelText('Documento'), '456')
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    // catálogo re-buscado com o novo cedente, e o select já vem com ele selecionado
    expect(await screen.findByRole('option', { name: 'Nova Empresa SA' })).toBeInTheDocument()
    await waitFor(() => expect(screen.getByLabelText('Cedente')).toHaveValue('c-novo'))
  })

  it('submissão com falha mostra a mensagem de erro retornada pela API', async () => {
    mockarCatalogos()
    const resposta: LoteLiquidacaoResponse = {
      totalItens: 1,
      totalSucesso: 0,
      totalFalha: 1,
      itens: [
        { sucesso: false, recebivelId: 'r1', liquidacao: null, codigoErro: 'SALDO_INSUFICIENTE', mensagemErro: 'Saldo insuficiente em caixa BRL' },
      ],
    }
    vi.mocked(recebiveisApi.enviarLote).mockResolvedValue(resposta)

    render(<PainelOperadorPage />, { wrapper })
    const user = userEvent.setup()

    await screen.findByRole('option', { name: 'Acme Ltda' })
    await user.selectOptions(screen.getByLabelText('Cedente'), 'c1')
    await user.selectOptions(screen.getByLabelText('Tipo de recebível'), 'DUPLICATA_MERCANTIL')
    await user.type(screen.getByLabelText('Valor de face'), '1000')
    fireEvent.change(screen.getByLabelText('Vencimento'), { target: { value: '2099-08-01' } })
    await user.click(screen.getByRole('button', { name: /liquidar recebível/i }))

    expect(await screen.findByText('Saldo insuficiente em caixa BRL')).toBeInTheDocument()
  })
})
