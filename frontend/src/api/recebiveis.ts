import { httpClient } from './httpClient'
import type {
  LoteLiquidacaoResponse,
  RecebivelRequest,
  SimulacaoRecebivelRequest,
  SimulacaoRecebivelResponse,
} from './types'

export const recebiveisApi = {
  simular: (request: SimulacaoRecebivelRequest) =>
    httpClient.post<SimulacaoRecebivelResponse>('/recebiveis/simular', request),
  enviarLote: (itens: RecebivelRequest[]) =>
    httpClient.post<LoteLiquidacaoResponse>('/recebiveis/lote', { itens }),
}
