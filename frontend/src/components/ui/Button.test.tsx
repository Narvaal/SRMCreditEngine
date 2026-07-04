import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { Button } from './Button'

describe('Button', () => {
  it('renderiza o texto e dispara onClick', async () => {
    const onClick = vi.fn()
    const user = userEvent.setup()
    render(<Button onClick={onClick}>Liquidar</Button>)

    await user.click(screen.getByRole('button', { name: 'Liquidar' }))

    expect(onClick).toHaveBeenCalledOnce()
  })

  it('fica desabilitado quando disabled=true', () => {
    render(<Button disabled>Liquidar</Button>)

    expect(screen.getByRole('button', { name: 'Liquidar' })).toBeDisabled()
  })
})
