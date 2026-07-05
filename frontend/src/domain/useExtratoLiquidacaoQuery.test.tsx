import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { extratoLiquidacaoApi } from '../api/extratoLiquidacao'
import type { ExtratoLiquidacaoFiltro, PaginaResponse, ExtratoLiquidacaoLinha } from '../api/types'
import { useExtratoLiquidacaoQuery } from './useExtratoLiquidacaoQuery'

vi.mock('../api/extratoLiquidacao', () => ({
  extratoLiquidacaoApi: {
    buscar: vi.fn(),
  },
}))

function paginaCom(id: string): PaginaResponse<ExtratoLiquidacaoLinha> {
  return {
    content: [
      {
        id,
        recebivelId: 'r1',
        cedenteId: 'c1',
        cedenteNome: 'Acme Ltda',
        tipo: 'LIQUIDACAO',
        moedaTitulo: 'BRL',
        moedaPagamento: 'BRL',
        valorFace: 1000,
        valorLiquido: 900,
        criadoEm: '2026-07-01T00:00:00Z',
        estornada: false,
      },
    ],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
  }
}

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}

describe('useExtratoLiquidacaoQuery', () => {
  it('chama a API com o filtro recebido e expõe o resultado', async () => {
    vi.mocked(extratoLiquidacaoApi.buscar).mockResolvedValue(paginaCom('linha-1'))
    const filtro: ExtratoLiquidacaoFiltro = { page: 0, size: 20 }

    const { result } = renderHook(() => useExtratoLiquidacaoQuery(filtro), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(extratoLiquidacaoApi.buscar).toHaveBeenCalledWith(filtro)
    expect(result.current.data?.content[0].id).toBe('linha-1')
  })

  it('mantém os dados da página anterior visíveis (keepPreviousData) enquanto a nova página carrega', async () => {
    let resolverPagina2: (valor: PaginaResponse<ExtratoLiquidacaoLinha>) => void = () => {}
    vi.mocked(extratoLiquidacaoApi.buscar).mockImplementationOnce(() => Promise.resolve(paginaCom('pagina-1')));

    const { result, rerender } = renderHook((filtro: ExtratoLiquidacaoFiltro) => useExtratoLiquidacaoQuery(filtro), {
      wrapper,
      initialProps: { page: 0, size: 20 } as ExtratoLiquidacaoFiltro,
    })

    await waitFor(() => expect(result.current.data?.content[0].id).toBe('pagina-1'))

    vi.mocked(extratoLiquidacaoApi.buscar).mockImplementationOnce(
      () => new Promise((resolve) => { resolverPagina2 = resolve }),
    )
    rerender({ page: 1, size: 20 })

    // ainda mostra a página anterior enquanto a nova busca está em voo.
    expect(result.current.data?.content[0].id).toBe('pagina-1')
    expect(result.current.isPlaceholderData).toBe(true)

    resolverPagina2(paginaCom('pagina-2'))
    await waitFor(() => expect(result.current.data?.content[0].id).toBe('pagina-2'))
    expect(result.current.isPlaceholderData).toBe(false)
  })
})
