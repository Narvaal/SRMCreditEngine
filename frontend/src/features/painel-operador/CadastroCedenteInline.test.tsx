import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'
import { cedentesApi } from '../../api/cedentes'
import { ApiError } from '../../api/httpClient'
import { CadastroCedenteInline } from './CadastroCedenteInline'

vi.mock('../../api/cedentes', () => ({
  cedentesApi: {
    listar: vi.fn(),
    criar: vi.fn(),
  },
}))

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
}

describe('CadastroCedenteInline', () => {
  it('começa colapsado e expande ao clicar', async () => {
    render(<CadastroCedenteInline onCriado={vi.fn()} />, { wrapper })
    const user = userEvent.setup()

    expect(screen.queryByLabelText('Nome')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /cadastrar novo cedente/i }))

    expect(screen.getByLabelText('Nome')).toBeInTheDocument()
    expect(screen.getByLabelText('Documento')).toBeInTheDocument()
  })

  it('valida campos obrigatórios antes de chamar a API', async () => {
    render(<CadastroCedenteInline onCriado={vi.fn()} />, { wrapper })
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /cadastrar novo cedente/i }))
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText(/informe nome e documento/i)).toBeInTheDocument()
    expect(cedentesApi.criar).not.toHaveBeenCalled()
  })

  it('sucesso colapsa, limpa os campos e devolve o id pro chamador', async () => {
    vi.mocked(cedentesApi.criar).mockResolvedValue({ id: 'novo-1', nome: 'Nova Ltda', documento: '999' })
    const onCriado = vi.fn()
    render(<CadastroCedenteInline onCriado={onCriado} />, { wrapper })
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /cadastrar novo cedente/i }))
    await user.type(screen.getByLabelText('Nome'), 'Nova Ltda')
    await user.type(screen.getByLabelText('Documento'), '999')
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    await waitFor(() => expect(onCriado).toHaveBeenCalledWith('novo-1'))
    expect(screen.queryByLabelText('Nome')).not.toBeInTheDocument()
  })

  it('documento duplicado (409) mostra a mensagem da API e mantém o formulário aberto', async () => {
    vi.mocked(cedentesApi.criar).mockRejectedValue(
      new ApiError({
        timestamp: '2026-07-04T12:00:00Z',
        status: 409,
        codigo: 'CEDENTE_DUPLICADO',
        mensagem: 'Já existe um cedente cadastrado com o documento: 999',
        path: '/api/cedentes',
        camposInvalidos: [],
      }),
    )
    const onCriado = vi.fn()
    render(<CadastroCedenteInline onCriado={onCriado} />, { wrapper })
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /cadastrar novo cedente/i }))
    await user.type(screen.getByLabelText('Nome'), 'Nova Ltda')
    await user.type(screen.getByLabelText('Documento'), '999')
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText(/já existe um cedente cadastrado/i)).toBeInTheDocument()
    expect(onCriado).not.toHaveBeenCalled()
    expect(screen.getByLabelText('Nome')).toBeInTheDocument()
  })
})
