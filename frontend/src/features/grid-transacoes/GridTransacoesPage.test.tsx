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
    valorPresente: 900,
    valorLiquido: 900,
    criadoEm: '2026-07-01T00:00:00Z',
    liquidacaoEstornadaId: null,
    liquidacaoEstornadaCriadoEm: null,
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

  it('estornar abre o modal, confirmar fecha o modal na hora e o sucesso aparece num toast', async () => {
    mockarCatalogos()
    vi.mocked(extratoLiquidacaoApi.buscar).mockResolvedValue(paginaCom('Acme Ltda'))
    vi.mocked(liquidacoesApi.estornar).mockResolvedValue({ id: 'estorno-1' } as LiquidacaoResponse)

    render(<GridTransacoesPage />, { wrapper })
    await screen.findByRole('cell', { name: 'Acme Ltda' })
    const buscasAntes = vi.mocked(extratoLiquidacaoApi.buscar).mock.calls.length
    const chamadasAntes = vi.mocked(liquidacoesApi.estornar).mock.calls.length

    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: 'Estornar' }))

    // modal aberto com os dados da transação antes de qualquer chamada
    expect(screen.getByRole('dialog', { name: 'Estornar liquidação' })).toBeInTheDocument()
    expect(vi.mocked(liquidacoesApi.estornar).mock.calls.length).toBe(chamadasAntes)

    await user.click(screen.getByRole('button', { name: 'Confirmar' }))

    // o modal fecha imediatamente, sem esperar a resposta
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    await waitFor(() => expect(liquidacoesApi.estornar).toHaveBeenCalledWith('l0'))
    expect(await screen.findByRole('status')).toHaveTextContent('Liquidação estornada com sucesso.')
    // invalidação do extrato dispara uma nova busca
    await waitFor(() =>
      expect(vi.mocked(extratoLiquidacaoApi.buscar).mock.calls.length).toBeGreaterThan(buscasAntes),
    )
  })

  it('trava os botões Estornar enquanto um estorno está em voo', async () => {
    mockarCatalogos()
    vi.mocked(extratoLiquidacaoApi.buscar).mockResolvedValue(paginaCom('Acme Ltda', 'Beta SA'))
    let liberarEstorno!: (valor: LiquidacaoResponse) => void
    vi.mocked(liquidacoesApi.estornar).mockImplementation(
      () => new Promise((resolve) => (liberarEstorno = resolve)),
    )

    render(<GridTransacoesPage />, { wrapper })
    await screen.findByRole('cell', { name: 'Acme Ltda' })

    const user = userEvent.setup()
    await user.click(screen.getAllByRole('button', { name: 'Estornar' })[0])
    await user.click(screen.getByRole('button', { name: 'Confirmar' }))

    // com a mutação pendente, nenhum outro estorno pode começar
    for (const botao of screen.getAllByRole('button', { name: 'Estornar' })) {
      expect(botao).toBeDisabled()
    }

    liberarEstorno({ id: 'estorno-1' } as LiquidacaoResponse)
    await waitFor(() => {
      for (const botao of screen.getAllByRole('button', { name: 'Estornar' })) {
        expect(botao).toBeEnabled()
      }
    })
  })

  it('cancelar fecha o modal sem chamar a API', async () => {
    mockarCatalogos()
    vi.mocked(extratoLiquidacaoApi.buscar).mockResolvedValue(paginaCom('Acme Ltda'))

    render(<GridTransacoesPage />, { wrapper })
    await screen.findByRole('cell', { name: 'Acme Ltda' })
    // mocks são de módulo — compara contagem pra não depender de outros testes do arquivo
    const chamadasAntes = vi.mocked(liquidacoesApi.estornar).mock.calls.length

    const user = userEvent.setup()
    await user.click(screen.getByRole('button', { name: 'Estornar' }))
    await user.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(vi.mocked(liquidacoesApi.estornar).mock.calls.length).toBe(chamadasAntes)
  })

  it('erro no estorno (ex.: 409 de corrida) mostra a mensagem da API num toast', async () => {
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
    await user.click(screen.getByRole('button', { name: 'Confirmar' }))

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(await screen.findByRole('status')).toHaveTextContent(/já foi estornada anteriormente/i)
  })
})
