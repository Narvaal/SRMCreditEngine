import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { extratoLiquidacaoApi } from '../api/extratoLiquidacao'
import type { ExtratoLiquidacaoFiltro } from '../api/types'

export function useExtratoLiquidacaoQuery(filtro: ExtratoLiquidacaoFiltro) {
  return useQuery({
    queryKey: ['extrato-liquidacao', filtro],
    queryFn: () => extratoLiquidacaoApi.buscar(filtro),
    placeholderData: keepPreviousData, // evita "piscar" a tabela ao trocar de página
  })
}
