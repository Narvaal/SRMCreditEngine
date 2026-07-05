import { zodResolver } from '@hookform/resolvers/zod'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useForm } from 'react-hook-form'
import { describe, expect, it, vi } from 'vitest'
import type { Cedente, Moeda, TipoRecebivel } from '../../api/types'
import { recebivelFormSchema, type RecebivelFormInput, type RecebivelFormOutput } from '../../domain/recebivelFormSchema'
import { RecebivelForm } from './RecebivelForm'

const cedentes: Cedente[] = [{ id: 'c1', nome: 'Acme Ltda', documento: '123' }]
const tiposRecebivel: TipoRecebivel[] = [
  { codigo: 'DUPLICATA_MERCANTIL', nome: 'Duplicata Mercantil' },
  { codigo: 'CHEQUE_PRE_DATADO', nome: 'Cheque Pré-datado' },
]
const moedas: Moeda[] = [
  { codigo: 'BRL', nome: 'Real Brasileiro', casasDecimais: 2 },
  { codigo: 'USD', nome: 'Dólar Americano', casasDecimais: 2 },
]

function Harness({ isSubmitting = false, onValid = vi.fn() }: { isSubmitting?: boolean; onValid?: () => void }) {
  const form = useForm<RecebivelFormInput, unknown, RecebivelFormOutput>({
    resolver: zodResolver(recebivelFormSchema),
    defaultValues: {
      cedenteId: '',
      tipoRecebivelCodigo: 'DUPLICATA_MERCANTIL',
      valorFace: '' as unknown as number,
      dataVencimento: '',
      moedaTitulo: 'BRL',
      moedaPagamento: 'BRL',
    },
  })

  return (
    <RecebivelForm
      form={form}
      onSubmit={form.handleSubmit(onValid)}
      isSubmitting={isSubmitting}
      cedentes={cedentes}
      tiposRecebivel={tiposRecebivel}
      moedas={moedas}
    />
  )
}

describe('RecebivelForm', () => {
  it('lista as opções recebidas via props (cedentes, tipos, moedas)', () => {
    render(<Harness />)

    expect(screen.getByRole('option', { name: 'Acme Ltda' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: 'Duplicata Mercantil' })).toBeInTheDocument()
    expect(screen.getAllByRole('option', { name: 'BRL' }).length).toBeGreaterThan(0)
  })

  it('tipo de recebível não tem placeholder — abre já com Duplicata Mercantil selecionada', () => {
    render(<Harness />)

    const select = screen.getByLabelText('Tipo de recebível')
    expect(select).toHaveValue('DUPLICATA_MERCANTIL')
    expect(select.querySelectorAll('option')).toHaveLength(2)
  })

  it('submeter com campos obrigatórios vazios mostra erros de validação e não chama onValid', async () => {
    const onValid = vi.fn()
    render(<Harness onValid={onValid} />)

    await userEvent.click(screen.getByRole('button', { name: /liquidar recebível/i }))

    expect(await screen.findByText('Selecione um cedente.')).toBeInTheDocument()
    expect(screen.getByText('Informe um valor.')).toBeInTheDocument()
    expect(screen.getByText('Informe o vencimento.')).toBeInTheDocument()
    expect(onValid).not.toHaveBeenCalled()
  })

  it('valor de face mostra o símbolo da moeda do título e acompanha a troca de moeda', async () => {
    render(<Harness />)

    expect(screen.getByText('R$')).toBeInTheDocument()

    await userEvent.selectOptions(screen.getByLabelText('Moeda do título'), 'USD')

    expect(screen.getByText('US$')).toBeInTheDocument()
    expect(screen.queryByText('R$')).not.toBeInTheDocument()
  })

  it('isSubmitting desabilita o botão e troca o texto', () => {
    render(<Harness isSubmitting />)

    const botao = screen.getByRole('button', { name: /enviando/i })
    expect(botao).toBeDisabled()
  })
})
