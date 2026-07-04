import { useQuery } from '@tanstack/react-query'
import { recebiveisApi } from '../api/recebiveis'
import type { SimulacaoRecebivelRequest } from '../api/types'

/** `request` nulo desabilita a query — usado quando os campos ainda não passam na validação local. */
export function useSimulacaoRecebivel(request: SimulacaoRecebivelRequest | null) {
  return useQuery({
    queryKey: ['simulacao', request],
    queryFn: () => recebiveisApi.simular(request as SimulacaoRecebivelRequest),
    enabled: request !== null,
    retry: false,
  })
}
