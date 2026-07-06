import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { Toast } from './Toast'

describe('Toast', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('renderiza a mensagem como status acessível (aria-live)', () => {
    render(
      <Toast tipo="success" onFechar={vi.fn()}>
        Liquidação estornada com sucesso.
      </Toast>,
    )

    expect(screen.getByRole('status')).toHaveTextContent('Liquidação estornada com sucesso.')
  })

  it('some sozinho depois da duração configurada', () => {
    vi.useFakeTimers()
    const onFechar = vi.fn()
    render(
      <Toast tipo="success" onFechar={onFechar} duracaoMs={5000}>
        ok
      </Toast>,
    )

    act(() => {
      vi.advanceTimersByTime(4999)
    })
    expect(onFechar).not.toHaveBeenCalled()

    act(() => {
      vi.advanceTimersByTime(1)
    })
    expect(onFechar).toHaveBeenCalledTimes(1)
  })

  it('botão de fechar dispara onFechar imediatamente', async () => {
    const onFechar = vi.fn()
    render(
      <Toast tipo="error" onFechar={onFechar}>
        falhou
      </Toast>,
    )

    await userEvent.setup().click(screen.getByRole('button', { name: 'Fechar notificação' }))
    expect(onFechar).toHaveBeenCalledTimes(1)
  })
})
