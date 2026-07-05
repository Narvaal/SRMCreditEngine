import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/httpClient'
import { catalogosApi } from '../../api/catalogos'
import { cedentesApi } from '../../api/cedentes'
import { extratoLiquidacaoApi } from '../../api/extratoLiquidacao'
import { liquidacoesApi } from '../../api/liquidacoes'
import type { LiquidacaoResponse, PaginaResponse, ExtratoLiquidacaoLinha } from '../../api/types'
import { GridTransacoesPage } from './GridTransacoesPage'

vi.mock('../../api/cedentes', () => ({ cedentesApi: { listar: vi.fn(), criar: vi.fn() } }))
vi.mock('../../api/catalogos', () => ({
  catalogosApi: { listarMoedas: vi.fn(), listarTiposRecebivel: vi.fn() },
}))
vi.mock('../../api/extratoLiquidacao', () => ({ extratoLiquidacaoApi: { buscar: vi.fn() } }))
vi.mock('../../api/liquidacoes', () => ({ liquidacoesApi: { estornar: vi.fn() } }))

function paginaCom(...nomes: string[]): PaginaResponse<ExtratoLiquidacaoLinha> {
  const content: ExtratoLiquidacaoLinha[] = nomes.map((nome, i) => ({
    id: `l${i}`,
    recebivelId: `r${i}`,
    cedenteId: `c${i}`,
    cedenteNome: nome,
    tipo: 'LIQUIDACAO',
    moedaTitulo: 'BRL',
    moedaPagamento: 'BRL',
    valorFace: 1000,
    valorLiquido: 900,
    criadoEm: '2026-07-01T00:00:00Z',
    estornada: false,
  }))
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: content.length > 0 ? 2 : 0 }
}

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return (
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/transacoes']}>{children}</MemoryRouter>
    </QueryClientProvider>
  )
}

function mockarCatalogos() {
  vi.mocked(cedentesApi.listar).mockResolvedValue([{ id: 'c1', nome: 'Acme Ltda', documento: '123' }])
  vi.mocked(catalogosApi.listarMoedas).mockResolvedValue([
    { codigo: 'BRL', nome: 'Real Brasileiro', casasDecimais: 2 },
  ])
}

describe('GridTransacoesPage', () => {
  it('renderiza as linhas retornadas pela API e a paginação', async () => {
    mockarCatalogos()
    vi.mocked(extratoLiquidacaoApi.buscar).mockResolvedValue(paginaCom('Acme Ltda'))

    render(<GridTransacoesPage />, { wrapper })

    expect(await screen.findByRole('cell', { name: 'Acme Ltda' })).toBeInTheDocument()
    expect(screen.getByText(/página 1 de 2/i)).toBeInTheDocument()
  })

  it('mostra o estado vazio quando não há transações', async () => {
    mockarCatalogos()
    vi.mocked(extratoLiquidacaoApi.buscar).mockResolvedValue(paginaCom())

    render(<GridTransacoesPage />, { wrapper })

    expect(await screen.findByText(/nenhuma transação encontrada/i)).toBeInTheDocument()
  })

  it('selecionar um cedente no filtro refaz a busca com o cedenteId', async () => {
    mockarCatalogos()
    vi.mocked(extratoLiquidacaoApi.buscar).mockResolvedValue(paginaCom('Acme Ltda'))

    render(<GridTransacoesPage />, { wrapper })
    await screen.findByRole('cell', { name: 'Acme Ltda' })

    const user = userEvent.setup()
    await user.selectOptions(screen.getByLabelText('Cedente'), 'c1')

    await waitFor(() =>
      expect(extratoLiquidacaoApi.buscar).toHaveBeenCalledWith(expect.objectContaining({ cedenteId: 'c1', page: 0 })),
    )
  })

  it('clicar em "Próxima" avança a página', async () => {
    mockarCatalogos()
    vi.mocked(extratoLiquidacaoApi.buscar).mockResolvedValue(paginaCom('Acme Ltda'))

    render(<GridTransacoesPage />, { wrapper })
    await screen.findByRole('cell', { name: 'Acme Ltda' })

    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: /próxima/i }))

    await waitFor(() => expect(extratoLiquidacaoApi.buscar).toHaveBeenCalledWith(expect.objectContaining({ page: 1 })))
  })

  it('estornar com confirmação chama a API e refaz a busca do extrato', async () => {
    mockarCatalogos()
    vi.mocked(extratoLiquidacaoApi.buscar).mockResolvedValue(paginaCom('Acme Ltda'))
    vi.mocked(liquidacoesApi.estornar).mockResolvedValue({ id: 'estorno-1' } as LiquidacaoResponse)

    render(<GridTransacoesPage />, { wrapper })
    await screen.findByRole('cell', { name: 'Acme Ltda' })
    const buscasAntes = vi.mocked(extratoLiquidacaoApi.buscar).mock.calls.length

    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: 'Estornar' }))
    await user.click(screen.getByRole('button', { name: 'Confirmar estorno' }))

    await waitFor(() => expect(liquidacoesApi.estornar).toHaveBeenCalledWith('l0'))
    // invalidação do extrato dispara uma nova busca
    await waitFor(() =>
      expect(vi.mocked(extratoLiquidacaoApi.buscar).mock.calls.length).toBeGreaterThan(buscasAntes),
    )
  })

  it('erro no estorno (ex.: 409 de corrida) mostra a mensagem da API num alerta', async () => {
    mockarCatalogos()
    vi.mocked(extratoLiquidacaoApi.buscar).mockResolvedValue(paginaCom('Acme Ltda'))
    vi.mocked(liquidacoesApi.estornar).mockRejectedValue(
      new ApiError({
        timestamp: '2026-07-04T12:00:00Z',
        status: 409,
        codigo: 'ESTORNO_INVALIDO',
        mensagem: 'Não é possível estornar a liquidação l0: já foi estornada anteriormente',
        path: '/api/liquidacoes/l0/estorno',
        camposInvalidos: [],
      }),
    )

    render(<GridTransacoesPage />, { wrapper })
    await screen.findByRole('cell', { name: 'Acme Ltda' })

    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: 'Estornar' }))
    await user.click(screen.getByRole('button', { name: 'Confirmar estorno' }))

    expect(await screen.findByText(/já foi estornada anteriormente/i)).toBeInTheDocument()
  })
})
