import { useQuery } from '@tanstack/react-query'
import { catalogosApi } from '../api/catalogos'
import { cedentesApi } from '../api/cedentes'

export function useMoedas() {
  return useQuery({ queryKey: ['moedas'], queryFn: catalogosApi.listarMoedas })
}

export function useTiposRecebivel() {
  return useQuery({ queryKey: ['tipos-recebivel'], queryFn: catalogosApi.listarTiposRecebivel })
}

export function useCedentes() {
  return useQuery({ queryKey: ['cedentes'], queryFn: cedentesApi.listar })
}
