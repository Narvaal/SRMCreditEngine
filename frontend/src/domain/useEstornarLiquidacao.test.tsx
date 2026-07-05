import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/httpClient'
import { liquidacoesApi } from '../api/liquidacoes'
import type { LiquidacaoResponse } from '../api/types'
import { useEstornarLiquidacao } from './useEstornarLiquidacao'

vi.mock('../api/liquidacoes', () => ({
  liquidacoesApi: {
    estornar: vi.fn(),
  },
}))

let queryClient: QueryClient

function wrapper({ children }: { children: ReactNode }) {
  queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}

describe('useEstornarLiquidacao', () => {
  it('sucesso chama a API com o id e invalida o extrato', async () => {
    vi.mocked(liquidacoesApi.estornar).mockResolvedValue({ id: 'estorno-1' } as LiquidacaoResponse)
    const { result } = renderHook(() => useEstornarLiquidacao(), { wrapper })
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')

    result.current.mutate('liquidacao-1')

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(liquidacoesApi.estornar).toHaveBeenCalledWith('liquidacao-1')
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['extrato-liquidacao'] })
  })

  it('falha expõe o ApiError sem invalidar o extrato', async () => {
    vi.mocked(liquidacoesApi.estornar).mockRejectedValue(
      new ApiError({
        timestamp: '2026-07-04T12:00:00Z',
        status: 409,
        codigo: 'ESTORNO_INVALIDO',
        mensagem: 'já foi estornada anteriormente',
        path: '/api/liquidacoes/liquidacao-1/estorno',
        camposInvalidos: [],
      }),
    )
    const { result } = renderHook(() => useEstornarLiquidacao(), { wrapper })
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')

    result.current.mutate('liquidacao-1')

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(result.current.error).toBeInstanceOf(ApiError)
    expect((result.current.error as ApiError).codigo).toBe('ESTORNO_INVALIDO')
    expect(invalidateSpy).not.toHaveBeenCalled()
  })
})
