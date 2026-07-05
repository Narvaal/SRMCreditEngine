import { httpClient } from './httpClient'
import type { LiquidacaoResponse } from './types'

export const liquidacoesApi = {
  estornar: (id: string) => httpClient.post<LiquidacaoResponse>(`/liquidacoes/${id}/estorno`),
}
