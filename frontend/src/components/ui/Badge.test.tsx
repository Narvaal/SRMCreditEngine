import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Badge } from './Badge'

describe('Badge', () => {
  it('renderiza o conteúdo', () => {
    render(<Badge tom="danger">ESTORNO</Badge>)

    expect(screen.getByText('ESTORNO')).toBeInTheDocument()
  })
})
