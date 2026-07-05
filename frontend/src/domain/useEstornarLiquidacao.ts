import { useMutation, useQueryClient } from '@tanstack/react-query'
import { liquidacoesApi } from '../api/liquidacoes'

/**
 * Estorna uma liquidação e invalida o extrato — a linha ESTORNO nova aparece e a original passa a
 * vir com `estornada=true` (sumindo o botão). O erro (ex.: 409 de corrida entre dois operadores)
 * fica exposto pra página exibir; o backend é quem garante que duplo-estorno nunca acontece.
 */
export function useEstornarLiquidacao() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (liquidacaoId: string) => liquidacoesApi.estornar(liquidacaoId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['extrato-liquidacao'] })
    },
  })
}
