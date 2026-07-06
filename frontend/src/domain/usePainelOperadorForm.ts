import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { recebiveisApi } from '../api/recebiveis'
import type { LiquidacaoItemResultado } from '../api/types'
import {
  camposPrecificacaoSchema,
  recebivelFormSchema,
  type RecebivelFormInput,
  type RecebivelFormOutput,
} from './recebivelFormSchema'
import { useDebouncedValue } from './useDebouncedValue'
import { useSimulacaoRecebivel } from './useSimulacaoRecebivel'

const DEBOUNCE_MS = 450
const CAMPOS_PRECIFICACAO = ['tipoRecebivelCodigo', 'valorFace', 'dataVencimento', 'moedaTitulo', 'moedaPagamento'] as const

/**
 * Orquestrador central do Painel do Operador: formulário (react-hook-form + Zod) + debounce +
 * simulação em tempo real (sem cedenteId, que não afeta o preço) + submissão do lote real.
 */
export function usePainelOperadorForm() {
  const form = useForm<RecebivelFormInput, unknown, RecebivelFormOutput>({
    resolver: zodResolver(recebivelFormSchema),
    // onTouched: o erro aparece assim que o operador sai do campo, casando com o aviso
    // em tempo real do painel de resultado (que não depende de submit).
    mode: 'onTouched',
    defaultValues: {
      cedenteId: '',
      // Duplicata Mercantil é o recebível mais comum na mesa — default em vez de placeholder vazio.
      tipoRecebivelCodigo: 'DUPLICATA_MERCANTIL',
      valorFace: '' as unknown as number,
      dataVencimento: '',
      moedaTitulo: 'BRL',
      moedaPagamento: 'BRL',
    },
  })

  const camposObservados = form.watch(CAMPOS_PRECIFICACAO)
  const camposComAtraso = useDebouncedValue(camposObservados, DEBOUNCE_MS)

  const { requestSimulacao, camposComFalha } = useMemo(() => {
    const [tipoRecebivelCodigo, valorFace, dataVencimento, moedaTitulo, moedaPagamento] = camposComAtraso
    const valores = { tipoRecebivelCodigo, valorFace, dataVencimento, moedaTitulo, moedaPagamento }
    const resultado = camposPrecificacaoSchema.safeParse(valores)
    if (resultado.success) {
      return { requestSimulacao: resultado.data, camposComFalha: [] as string[] }
    }
    // Campo vazio é formulário incompleto ("preencha..."); campo preenchido que falhou é erro
    // de verdade — o painel de resultado avisa em vez de fingir que só falta preencher.
    const campoPreenchido = (campo: string) => {
      const valor = valores[campo as keyof typeof valores]
      return valor !== undefined && valor !== null && String(valor) !== ''
    }
    const falhas = resultado.error.issues.map((issue) => String(issue.path[0])).filter(campoPreenchido)
    return { requestSimulacao: null, camposComFalha: falhas }
  }, [camposComAtraso])

  // O aviso do painel só acompanha erros que o operador já está vendo no campo (onTouched) —
  // sem isso ele apareceria antes do destaque, apontando pra um erro ainda invisível.
  const { errors } = form.formState
  const simulacaoComErro = camposComFalha.some((campo) => campo in errors)

  const simulacao = useSimulacaoRecebivel(requestSimulacao)

  const queryClient = useQueryClient()
  const [resultadoEnvio, setResultadoEnvio] = useState<LiquidacaoItemResultado | null>(null)

  const envioMutation = useMutation({
    mutationFn: (valores: RecebivelFormOutput) => recebiveisApi.enviarLote([valores]),
    onSuccess: (resposta) => {
      const item = resposta.itens[0]
      setResultadoEnvio(item)
      if (item.sucesso) {
        form.resetField('valorFace')
        form.resetField('dataVencimento')
        queryClient.invalidateQueries({ queryKey: ['extrato-liquidacao'] })
      }
    },
  })

  const onSubmit = form.handleSubmit((valores) => {
    setResultadoEnvio(null)
    envioMutation.mutate(valores)
  })

  return {
    form,
    simulacao,
    simulacaoPronta: requestSimulacao !== null,
    simulacaoComErro,
    onSubmit,
    isSubmitting: envioMutation.isPending,
    resultadoEnvio,
  }
}
