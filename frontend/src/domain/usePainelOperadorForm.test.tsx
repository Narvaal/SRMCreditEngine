import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { recebiveisApi } from '../api/recebiveis'
import type { LoteLiquidacaoResponse, SimulacaoRecebivelResponse } from '../api/types'
import { usePainelOperadorForm } from './usePainelOperadorForm'

vi.mock('../api/recebiveis', () => ({
  recebiveisApi: {
    simular: vi.fn(),
    enviarLote: vi.fn(),
  },
}))

const SIMULACAO_RESPOSTA: SimulacaoRecebivelResponse = {
  valorFace: 1000,
  moedaTitulo: 'BRL',
  taxaBaseUsada: 0.01,
  spreadUsado: 0.015,
  prazoMesesUsado: 1,
  valorPresente: 975.61,
  moedaPagamento: 'BRL',
  taxaCambioUsada: null,
  valorLiquido: 975.61,
}

function loteResposta(sucesso: boolean): LoteLiquidacaoResponse {
  return {
    totalItens: 1,
    totalSucesso: sucesso ? 1 : 0,
    totalFalha: sucesso ? 0 : 1,
    itens: [
      {
        sucesso,
        recebivelId: sucesso ? 'recebivel-1' : null,
        liquidacao: sucesso
          ? {
              id: 'liquidacao-1',
              recebivelId: 'recebivel-1',
              cedenteId: 'cedente-1',
              tipo: 'LIQUIDACAO',
              liquidacaoEstornadaId: null,
              valorFace: 1000,
              moedaTitulo: 'BRL',
              taxaBaseUsada: 0.01,
              spreadUsado: 0.015,
              prazoMesesUsado: 1,
              valorPresente: 975.61,
              moedaPagamento: 'BRL',
              taxaCambioUsada: null,
              valorLiquido: 975.61,
              criadoEm: '2026-07-01T00:00:00Z',
            }
          : null,
        codigoErro: sucesso ? null : 'SALDO_INSUFICIENTE',
        mensagemErro: sucesso ? null : 'Saldo insuficiente em caixa BRL',
      },
    ],
  }
}

let queryClient: QueryClient

function wrapper({ children }: { children: ReactNode }) {
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}

/**
 * Preenche os campos que afetam o preço, sem cedenteId — para os testes de simulação. Registra
 * valorFace/dataVencimento antes do setValue: sem isso, resetField (usado em onSuccess do envio)
 * é um no-op no react-hook-form — só reseta campos que passaram por register/Controller, como o
 * <input> real de RecebivelForm faz em produção.
 */
function preencherCamposPrecificacao(form: ReturnType<typeof usePainelOperadorForm>['form']) {
  act(() => {
    form.register('valorFace')
    form.register('dataVencimento')
    form.setValue('tipoRecebivelCodigo', 'DUPLICATA_MERCANTIL')
    form.setValue('valorFace', 1000)
    form.setValue('dataVencimento', '2026-09-15')
    form.setValue('moedaTitulo', 'BRL')
    form.setValue('moedaPagamento', 'BRL')
  })
}

describe('usePainelOperadorForm', () => {
  beforeEach(() => {
    queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  it('não simula enquanto os campos de precificação continuam inválidos (valores default)', () => {
    vi.useFakeTimers()
    vi.mocked(recebiveisApi.simular).mockResolvedValue(SIMULACAO_RESPOSTA)
    const { result } = renderHook(() => usePainelOperadorForm(), { wrapper })

    act(() => {
      vi.advanceTimersByTime(450)
    })

    expect(result.current.simulacaoPronta).toBe(false)
    expect(recebiveisApi.simular).not.toHaveBeenCalled()
  })

  it('só dispara a simulação depois do debounce de 450ms com campos válidos', async () => {
    vi.useFakeTimers()
    vi.mocked(recebiveisApi.simular).mockResolvedValue(SIMULACAO_RESPOSTA)
    const { result } = renderHook(() => usePainelOperadorForm(), { wrapper })

    preencherCamposPrecificacao(result.current.form)

    act(() => {
      vi.advanceTimersByTime(300)
    })
    expect(result.current.simulacaoPronta).toBe(false)
    expect(recebiveisApi.simular).not.toHaveBeenCalled()

    // advanceTimersByTimeAsync flush as microtasks entre disparos de timer — necessário pra
    // deixar a promise mockada de `simular` resolver e o estado do react-query atualizar.
    await act(async () => {
      await vi.advanceTimersByTimeAsync(150)
    })
    expect(result.current.simulacaoPronta).toBe(true)
    expect(recebiveisApi.simular).toHaveBeenCalledWith({
      tipoRecebivelCodigo: 'DUPLICATA_MERCANTIL',
      valorFace: 1000,
      dataVencimento: '2026-09-15',
      moedaTitulo: 'BRL',
      moedaPagamento: 'BRL',
    })
  })

  it('mudar cedenteId não dispara uma nova simulação (não afeta o preço)', async () => {
    vi.useFakeTimers()
    vi.mocked(recebiveisApi.simular).mockResolvedValue(SIMULACAO_RESPOSTA)
    const { result } = renderHook(() => usePainelOperadorForm(), { wrapper })

    preencherCamposPrecificacao(result.current.form)
    await act(async () => {
      await vi.advanceTimersByTimeAsync(450)
    })
    expect(result.current.simulacaoPronta).toBe(true)
    expect(recebiveisApi.simular).toHaveBeenCalledTimes(1)

    act(() => {
      result.current.form.setValue('cedenteId', 'cedente-1')
    })
    await act(async () => {
      await vi.advanceTimersByTimeAsync(450)
    })

    expect(recebiveisApi.simular).toHaveBeenCalledTimes(1)
  })

  it('submissão com sucesso reseta valorFace/dataVencimento, preserva cedenteId e invalida o extrato', async () => {
    vi.mocked(recebiveisApi.enviarLote).mockResolvedValue(loteResposta(true))
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => usePainelOperadorForm(), { wrapper })

    act(() => {
      result.current.form.setValue('cedenteId', 'cedente-1')
    })
    preencherCamposPrecificacao(result.current.form)

    await act(async () => {
      await result.current.onSubmit()
    })

    await waitFor(() => expect(result.current.resultadoEnvio?.sucesso).toBe(true))
    expect(result.current.form.getValues('cedenteId')).toBe('cedente-1')
    expect(result.current.form.getValues('valorFace')).toBe('')
    expect(result.current.form.getValues('dataVencimento')).toBe('')
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['extrato-liquidacao'] })
  })

  it('submissão com falha não reseta o formulário nem invalida o extrato', async () => {
    vi.mocked(recebiveisApi.enviarLote).mockResolvedValue(loteResposta(false))
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries')
    const { result } = renderHook(() => usePainelOperadorForm(), { wrapper })

    act(() => {
      result.current.form.setValue('cedenteId', 'cedente-1')
    })
    preencherCamposPrecificacao(result.current.form)

    await act(async () => {
      await result.current.onSubmit()
    })

    await waitFor(() => expect(result.current.resultadoEnvio?.sucesso).toBe(false))
    expect(result.current.resultadoEnvio?.codigoErro).toBe('SALDO_INSUFICIENTE')
    expect(result.current.form.getValues('valorFace')).toBe(1000)
    expect(invalidateSpy).not.toHaveBeenCalled()
  })
})
