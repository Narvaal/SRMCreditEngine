import type { ErroResponse } from './types'

/** Erro tipado normalizado a partir do ErroResponse padrão do backend (ver GlobalExceptionHandler). */
export class ApiError extends Error {
  readonly status: number
  readonly codigo: string
  readonly camposInvalidos: ErroResponse['camposInvalidos']

  constructor(erro: ErroResponse) {
    super(erro.mensagem)
    this.name = 'ApiError'
    this.status = erro.status
    this.codigo = erro.codigo
    this.camposInvalidos = erro.camposInvalidos
  }
}

const BASE_URL = '/api'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  })

  if (!response.ok) {
    const erro = (await response.json()) as ErroResponse
    throw new ApiError(erro)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

export const httpClient = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) }),
}
