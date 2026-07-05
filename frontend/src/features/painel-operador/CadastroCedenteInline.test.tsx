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

const CPF_VALIDO_DIGITADO = '52998224725'
const CPF_VALIDO_MASCARADO = '529.982.247-25'

async function expandirFormulario(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole('button', { name: /cadastrar novo cedente/i }))
}

describe('CadastroCedenteInline', () => {
  it('começa colapsado e expande ao clicar', async () => {
    render(<CadastroCedenteInline onCriado={vi.fn()} />, { wrapper })
    const user = userEvent.setup()

    expect(screen.queryByLabelText('Nome')).not.toBeInTheDocument()

    await expandirFormulario(user)

    expect(screen.getByLabelText('Nome')).toBeInTheDocument()
    expect(screen.getByLabelText('Documento CPF/CNPJ')).toBeInTheDocument()
  })

  it('aplica a máscara de CPF e de CNPJ conforme a digitação', async () => {
    render(<CadastroCedenteInline onCriado={vi.fn()} />, { wrapper })
    const user = userEvent.setup()

    await expandirFormulario(user)
    const campoDocumento = screen.getByLabelText('Documento CPF/CNPJ')

    await user.type(campoDocumento, CPF_VALIDO_DIGITADO)
    expect(campoDocumento).toHaveValue(CPF_VALIDO_MASCARADO)

    await user.clear(campoDocumento)
    await user.type(campoDocumento, '11444777000161')
    expect(campoDocumento).toHaveValue('11.444.777/0001-61')
  })

  it('valida os campos antes de chamar a API', async () => {
    render(<CadastroCedenteInline onCriado={vi.fn()} />, { wrapper })
    const user = userEvent.setup()

    await expandirFormulario(user)
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText(/entre 3 e 20 caracteres/i)).toBeInTheDocument()
    expect(screen.getByText(/informe um cpf ou cnpj válido/i)).toBeInTheDocument()
    expect(cedentesApi.criar).not.toHaveBeenCalled()
  })

  it('rejeita CPF com dígito verificador errado', async () => {
    render(<CadastroCedenteInline onCriado={vi.fn()} />, { wrapper })
    const user = userEvent.setup()

    await expandirFormulario(user)
    await user.type(screen.getByLabelText('Nome'), 'Nova Ltda')
    await user.type(screen.getByLabelText('Documento CPF/CNPJ'), '52998224726')
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText('CPF inválido.')).toBeInTheDocument()
    expect(cedentesApi.criar).not.toHaveBeenCalled()
  })

  it('rejeita razão social com caracteres especiais', async () => {
    render(<CadastroCedenteInline onCriado={vi.fn()} />, { wrapper })
    const user = userEvent.setup()

    await expandirFormulario(user)
    await user.type(screen.getByLabelText('Nome'), 'Nova & Cia')
    await user.type(screen.getByLabelText('Documento CPF/CNPJ'), CPF_VALIDO_DIGITADO)
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText(/não pode conter caracteres especiais/i)).toBeInTheDocument()
    expect(cedentesApi.criar).not.toHaveBeenCalled()
  })

  it('sucesso envia o documento sem máscara, colapsa e devolve o id pro chamador', async () => {
    vi.mocked(cedentesApi.criar).mockResolvedValue({
      id: 'novo-1',
      nome: 'Nova Ltda',
      documento: CPF_VALIDO_DIGITADO,
    })
    const onCriado = vi.fn()
    render(<CadastroCedenteInline onCriado={onCriado} />, { wrapper })
    const user = userEvent.setup()

    await expandirFormulario(user)
    await user.type(screen.getByLabelText('Nome'), 'Nova Ltda')
    await user.type(screen.getByLabelText('Documento CPF/CNPJ'), CPF_VALIDO_DIGITADO)
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    await waitFor(() => expect(onCriado).toHaveBeenCalledWith('novo-1'))
    expect(cedentesApi.criar).toHaveBeenCalledWith({ nome: 'Nova Ltda', documento: CPF_VALIDO_DIGITADO })
    expect(screen.queryByLabelText('Nome')).not.toBeInTheDocument()
  })

  it('documento duplicado (409) mostra a mensagem da API e mantém o formulário aberto', async () => {
    vi.mocked(cedentesApi.criar).mockRejectedValue(
      new ApiError({
        timestamp: '2026-07-04T12:00:00Z',
        status: 409,
        codigo: 'CEDENTE_DUPLICADO',
        mensagem: `Já existe um cedente cadastrado com o documento: ${CPF_VALIDO_DIGITADO}`,
        path: '/api/cedentes',
        camposInvalidos: [],
      }),
    )
    const onCriado = vi.fn()
    render(<CadastroCedenteInline onCriado={onCriado} />, { wrapper })
    const user = userEvent.setup()

    await expandirFormulario(user)
    await user.type(screen.getByLabelText('Nome'), 'Nova Ltda')
    await user.type(screen.getByLabelText('Documento CPF/CNPJ'), CPF_VALIDO_DIGITADO)
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText(/já existe um cedente cadastrado/i)).toBeInTheDocument()
    expect(onCriado).not.toHaveBeenCalled()
    expect(screen.getByLabelText('Nome')).toBeInTheDocument()
  })

  it('erros de bean-validation do backend (camposInvalidos) aparecem no campo correspondente', async () => {
    vi.mocked(cedentesApi.criar).mockRejectedValue(
      new ApiError({
        timestamp: '2026-07-04T12:00:00Z',
        status: 400,
        codigo: 'REQUEST_INVALIDO',
        mensagem: 'Payload inválido',
        path: '/api/cedentes',
        camposInvalidos: [{ campo: 'documento', mensagem: 'Documento deve ser um CPF ou CNPJ válido' }],
      }),
    )
    render(<CadastroCedenteInline onCriado={vi.fn()} />, { wrapper })
    const user = userEvent.setup()

    await expandirFormulario(user)
    await user.type(screen.getByLabelText('Nome'), 'Nova Ltda')
    await user.type(screen.getByLabelText('Documento CPF/CNPJ'), CPF_VALIDO_DIGITADO)
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText('Documento deve ser um CPF ou CNPJ válido')).toBeInTheDocument()
    expect(screen.queryByText('Payload inválido')).not.toBeInTheDocument()
  })
})
