import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { recebiveisApi } from '../api/recebiveis'
import { useSimulacaoRecebivel } from './useSimulacaoRecebivel'

vi.mock('../api/recebiveis', () => ({
  recebiveisApi: {
    simular: vi.fn(),
  },
}))

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}

describe('useSimulacaoRecebivel', () => {
  it('não chama a API quando o request é nulo (campos ainda inválidos/incompletos)', () => {
    renderHook(() => useSimulacaoRecebivel(null), { wrapper })

    expect(recebiveisApi.simular).not.toHaveBeenCalled()
  })

  it('chama a API quando o request é válido', async () => {
    vi.mocked(recebiveisApi.simular).mockResolvedValue({
      valorFace: 1000,
      moedaTitulo: 'BRL',
      taxaBaseUsada: 0.01,
      spreadUsado: 0.015,
      prazoMesesUsado: 3,
      valorPresente: 928.6,
      moedaPagamento: 'BRL',
      taxaCambioUsada: null,
      valorLiquido: 928.6,
    })

    const request = {
      tipoRecebivelCodigo: 'DUPLICATA_MERCANTIL',
      valorFace: 1000,
      moedaTitulo: 'BRL',
      dataVencimento: '2026-09-15',
      moedaPagamento: 'BRL',
    }

    const { result } = renderHook(() => useSimulacaoRecebivel(request), { wrapper })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(recebiveisApi.simular).toHaveBeenCalledWith(request)
    expect(result.current.data?.valorLiquido).toBe(928.6)
  })
})
