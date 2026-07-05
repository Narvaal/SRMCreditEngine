import { useMutation, useQueryClient } from '@tanstack/react-query'
import { cedentesApi } from '../api/cedentes'

/**
 * Cadastra um cedente e invalida o catálogo — o select do formulário passa a listar o novo cedente
 * sem reload. Quem decide o que fazer com o cedente criado (ex.: auto-selecionar no form) é o
 * chamador, via onSuccess da mutação.
 */
export function useCadastroCedente() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (dados: { nome: string; documento: string }) => cedentesApi.criar(dados),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cedentes'] })
    },
  })
}
