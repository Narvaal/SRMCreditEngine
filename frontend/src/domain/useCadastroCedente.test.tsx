import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { cedentesApi } from '../api/cedentes'
import { useCadastroCedente } from './useCadastroCedente'

vi.mock('../api/cedentes', () => ({
  cedentesApi: {
    listar: vi.fn(),
    criar: vi.fn(),
  },
}))

let queryClient: QueryClient

function wrapper({ children }: { children: ReactNode }) {
  queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}

describe('useCadastroCedente', () => {
  it('sucesso cria o cedente e invalida o catálogo de cedentes', async () => {
    vi.mocked(cedentesApi.criar).mockResolvedValue({ id: 'novo-1', nome: 'Nova Ltda', documento: '999' })
    const { result } = renderHook(() => useCadastroCedente(), { wrapper })
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')

    result.current.mutate({ nome: 'Nova Ltda', documento: '999' })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(cedentesApi.criar).toHaveBeenCalledWith({ nome: 'Nova Ltda', documento: '999' })
    expect(result.current.data?.id).toBe('novo-1')
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['cedentes'] })
  })

  it('falha não invalida o catálogo', async () => {
    vi.mocked(cedentesApi.criar).mockRejectedValue(new Error('CEDENTE_DUPLICADO'))
    const { result } = renderHook(() => useCadastroCedente(), { wrapper })
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')

    result.current.mutate({ nome: 'Nova Ltda', documento: '999' })

    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(invalidateSpy).not.toHaveBeenCalled()
  })
})
