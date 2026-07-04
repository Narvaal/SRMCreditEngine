import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useDebouncedValue } from './useDebouncedValue'

describe('useDebouncedValue', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('não atualiza antes do atraso configurado', () => {
    const { result, rerender } = renderHook(({ valor }) => useDebouncedValue(valor, 450), {
      initialProps: { valor: 'a' },
    })

    rerender({ valor: 'b' })
    act(() => {
      vi.advanceTimersByTime(300)
    })

    expect(result.current).toBe('a')
  })

  it('atualiza depois do atraso configurado', () => {
    const { result, rerender } = renderHook(({ valor }) => useDebouncedValue(valor, 450), {
      initialProps: { valor: 'a' },
    })

    rerender({ valor: 'b' })
    act(() => {
      vi.advanceTimersByTime(450)
    })

    expect(result.current).toBe('b')
  })

  it('reinicia o timer se o valor mudar de novo antes do atraso completar', () => {
    const { result, rerender } = renderHook(({ valor }) => useDebouncedValue(valor, 450), {
      initialProps: { valor: 'a' },
    })

    rerender({ valor: 'b' })
    act(() => {
      vi.advanceTimersByTime(300)
    })
    rerender({ valor: 'c' })
    act(() => {
      vi.advanceTimersByTime(300)
    })

    // só passaram 300ms desde o último rerender ('c') — ainda não deveria ter atualizado
    expect(result.current).toBe('a')

    act(() => {
      vi.advanceTimersByTime(150)
    })
    expect(result.current).toBe('c')
  })
})
