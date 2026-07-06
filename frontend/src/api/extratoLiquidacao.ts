import { httpClient } from './httpClient'
import type { ExtratoLiquidacaoFiltro, ExtratoLiquidacaoLinha, PaginaResponse } from './types'

export const extratoLiquidacaoApi = {
  buscar: (filtro: ExtratoLiquidacaoFiltro) => {
    const params = new URLSearchParams()
    if (filtro.cedenteId) params.set('cedenteId', filtro.cedenteId)
    if (filtro.moeda) params.set('moeda', filtro.moeda)
    if (filtro.tipo) params.set('tipo', filtro.tipo)
    if (filtro.dataInicio) params.set('dataInicio', filtro.dataInicio)
    if (filtro.dataFim) params.set('dataFim', filtro.dataFim)
    params.set('page', String(filtro.page))
    params.set('size', String(filtro.size))

    return httpClient.get<PaginaResponse<ExtratoLiquidacaoLinha>>(`/relatorios/extrato-liquidacao?${params}`)
  },
}
