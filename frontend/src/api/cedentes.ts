import { httpClient } from './httpClient'
import type { Cedente } from './types'

export const cedentesApi = {
  listar: () => httpClient.get<Cedente[]>('/cedentes'),
  criar: (request: { nome: string; documento: string }) => httpClient.post<Cedente>('/cedentes', request),
}
