import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { Cedente, Moeda } from '../../api/types'
import { FiltrosTransacoes } from './FiltrosTransacoes'

const cedentes: Cedente[] = [{ id: 'c1', nome: 'Acme Ltda', documento: '123' }]
const moedas: Moeda[] = [
  { codigo: 'BRL', nome: 'Real Brasileiro', casasDecimais: 2 },
  { codigo: 'USD', nome: 'Dólar Americano', casasDecimais: 2 },
]

describe('FiltrosTransacoes', () => {
  it('selecionar um cedente chama onChange só com cedenteId', async () => {
    const onChange = vi.fn()
    render(
      <FiltrosTransacoes
        cedenteId=""
        moeda=""
        dataInicio=""
        dataFim=""
        cedentes={cedentes}
        moedas={moedas}
        onChange={onChange}
      />,
    )

    await userEvent.selectOptions(screen.getByLabelText('Cedente'), 'c1')

    expect(onChange).toHaveBeenCalledWith({ cedenteId: 'c1' })
  })

  it('selecionar uma moeda chama onChange só com moeda', async () => {
    const onChange = vi.fn()
    render(
      <FiltrosTransacoes
        cedenteId=""
        moeda=""
        dataInicio=""
        dataFim=""
        cedentes={cedentes}
        moedas={moedas}
        onChange={onChange}
      />,
    )

    await userEvent.selectOptions(screen.getByLabelText('Moeda'), 'USD')

    expect(onChange).toHaveBeenCalledWith({ moeda: 'USD' })
  })

  it('alterar a data "De" chama onChange só com dataInicio', () => {
    const onChange = vi.fn()
    render(
      <FiltrosTransacoes
        cedenteId=""
        moeda=""
        dataInicio=""
        dataFim=""
        cedentes={cedentes}
        moedas={moedas}
        onChange={onChange}
      />,
    )

    const campoData = screen.getByLabelText('De')
    fireEvent.change(campoData, { target: { value: '2026-07-01' } })

    expect(onChange).toHaveBeenCalledWith({ dataInicio: '2026-07-01' })
  })

  it('alterar a data "Até" chama onChange só com dataFim', () => {
    const onChange = vi.fn()
    render(
      <FiltrosTransacoes
        cedenteId=""
        moeda=""
        dataInicio=""
        dataFim=""
        cedentes={cedentes}
        moedas={moedas}
        onChange={onChange}
      />,
    )

    const campoData = screen.getByLabelText('Até')
    fireEvent.change(campoData, { target: { value: '2026-07-10' } })

    expect(onChange).toHaveBeenCalledWith({ dataFim: '2026-07-10' })
  })

  it('renderiza "Todos"/"Todas" como opção default e as opções recebidas via props', () => {
    render(
      <FiltrosTransacoes
        cedenteId=""
        moeda=""
        dataInicio=""
        dataFim=""
        cedentes={cedentes}
        moedas={moedas}
        onChange={vi.fn()}
      />,
    )

    expect(screen.getByRole('option', { name: 'Todos' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Todas' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Acme Ltda' })).toBeInTheDocument()
  })
})
