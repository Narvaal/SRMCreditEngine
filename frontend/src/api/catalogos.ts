import { httpClient } from './httpClient'
import type { Moeda, TipoRecebivel } from './types'

export const catalogosApi = {
  listarMoedas: () => httpClient.get<Moeda[]>('/moedas'),
  listarTiposRecebivel: () => httpClient.get<TipoRecebivel[]>('/tipos-recebivel'),
}
