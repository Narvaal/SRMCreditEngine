import { act, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { useExtratoFiltrosUrlState } from './useExtratoFiltrosUrlState'

function wrapperComUrl(initialEntry: string) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <MemoryRouter initialEntries={[initialEntry]}>{children}</MemoryRouter>
  }
}

describe('useExtratoFiltrosUrlState', () => {
  it('usa os defaults (page=0, size=20) quando a URL não tem parâmetros', () => {
    const { result } = renderHook(() => useExtratoFiltrosUrlState(), {
      wrapper: wrapperComUrl('/transacoes'),
    })

    expect(result.current.filtrosUrl).toEqual({
      cedenteId: '',
      moeda: '',
      tipo: '',
      dataInicio: '',
      dataFim: '',
      page: 0,
      size: 20,
    })
  })

  it('lê os filtros existentes na URL', () => {
    const { result } = renderHook(() => useExtratoFiltrosUrlState(), {
      wrapper: wrapperComUrl('/transacoes?cedenteId=c1&moeda=USD&page=2&size=50'),
    })

    expect(result.current.filtrosUrl).toMatchObject({
      cedenteId: 'c1',
      moeda: 'USD',
      page: 2,
      size: 50,
    })
  })

  it('atualizarFiltros seta o parâmetro e volta pra primeira página', () => {
    const { result } = renderHook(() => useExtratoFiltrosUrlState(), {
      wrapper: wrapperComUrl('/transacoes?page=3'),
    })

    act(() => {
      result.current.atualizarFiltros({ moeda: 'USD' })
    })

    expect(result.current.filtrosUrl.moeda).toBe('USD')
    expect(result.current.filtrosUrl.page).toBe(0)
  })

  it('atualizarFiltros remove o parâmetro da URL quando o valor é vazio, em vez de setar string vazia', () => {
    const { result } = renderHook(() => useExtratoFiltrosUrlState(), {
      wrapper: wrapperComUrl('/transacoes?moeda=USD'),
    })

    act(() => {
      result.current.atualizarFiltros({ moeda: '' })
    })

    expect(result.current.filtrosUrl.moeda).toBe('')
    expect(result.current.filtroApi.moeda).toBeUndefined()
  })

  it('irParaPagina só altera a página, preservando os demais filtros', () => {
    const { result } = renderHook(() => useExtratoFiltrosUrlState(), {
      wrapper: wrapperComUrl('/transacoes?cedenteId=c1&moeda=USD'),
    })

    act(() => {
      result.current.irParaPagina(4)
    })

    expect(result.current.filtrosUrl).toMatchObject({ cedenteId: 'c1', moeda: 'USD', page: 4 })
  })

  it('filtroApi converte dataFim (inclusiva, escolhida pelo operador) para o exclusivo que o backend espera', () => {
    const { result } = renderHook(() => useExtratoFiltrosUrlState(), {
      wrapper: wrapperComUrl('/transacoes?dataInicio=2026-07-01&dataFim=2026-07-10'),
    })

    expect(result.current.filtroApi.dataInicio).toBe('2026-07-01T00:00:00.000Z')
    // "até 10/07" deve incluir o dia inteiro de 10/07 → exclusivo é o início do dia 11/07.
    expect(result.current.filtroApi.dataFim).toBe('2026-07-11T00:00:00.000Z')
  })

  it('filtroApi não envia dataInicio/dataFim quando não informados na URL', () => {
    const { result } = renderHook(() => useExtratoFiltrosUrlState(), {
      wrapper: wrapperComUrl('/transacoes'),
    })

    expect(result.current.filtroApi.dataInicio).toBeUndefined()
    expect(result.current.filtroApi.dataFim).toBeUndefined()
  })
})
