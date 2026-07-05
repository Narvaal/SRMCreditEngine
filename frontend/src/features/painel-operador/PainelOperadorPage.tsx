import { useEffect, useState } from 'react'
import { ApiError } from '../../api/httpClient'
import { Alert, Card, Skeleton } from '../../components/ui'
import { useCedentes, useMoedas, useTiposRecebivel } from '../../domain/useCatalogos'
import { usePainelOperadorForm } from '../../domain/usePainelOperadorForm'
import { CadastroCedenteInline } from './CadastroCedenteInline'
import { RecebivelForm } from './RecebivelForm'
import { SimulacaoResultCard } from './SimulacaoResultCard'

export function PainelOperadorPage() {
  const cedentesQuery = useCedentes()
  const tiposQuery = useTiposRecebivel()
  const moedasQuery = useMoedas()

  const { form, simulacao, simulacaoPronta, simulacaoComErro, onSubmit, isSubmitting, resultadoEnvio } =
    usePainelOperadorForm()

  // Auto-seleção do cedente recém-cadastrado: o setValue só funciona quando a <option> já existe
  // no DOM, e ela só aparece depois do refetch do catálogo invalidado — por isso é um efeito
  // condicionado à lista, não um setValue direto no onCriado (que rodaria cedo demais).
  const [cedenteRecemCriadoId, setCedenteRecemCriadoId] = useState<string | null>(null)
  useEffect(() => {
    if (cedenteRecemCriadoId && cedentesQuery.data?.some((c) => c.id === cedenteRecemCriadoId)) {
      form.setValue('cedenteId', cedenteRecemCriadoId, { shouldValidate: true })
      setCedenteRecemCriadoId(null)
    }
  }, [cedenteRecemCriadoId, cedentesQuery.data, form])

  const catalogosCarregando = cedentesQuery.isLoading || tiposQuery.isLoading || moedasQuery.isLoading

  return (
    <div className="mx-auto max-w-4xl">
      <h1 className="mb-1 text-2xl font-semibold text-ink">Painel do Operador</h1>
      <p className="mb-6 text-sm text-ink-muted">
        Informe os dados do recebível para ver o valor líquido calculado em tempo real e liquidar.
      </p>

      {resultadoEnvio && (
        <div className="mb-4">
          {resultadoEnvio.sucesso ? (
            <Alert tipo="success">Recebível liquidado com sucesso.</Alert>
          ) : (
            <Alert tipo="error">{resultadoEnvio.mensagemErro}</Alert>
          )}
        </div>
      )}

      {catalogosCarregando ? (
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <Card className="flex flex-col gap-4">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
          </Card>
          <Skeleton className="h-48 w-full" />
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <Card>
            <RecebivelForm
              form={form}
              onSubmit={onSubmit}
              isSubmitting={isSubmitting}
              cedentes={cedentesQuery.data ?? []}
              tiposRecebivel={tiposQuery.data ?? []}
              moedas={moedasQuery.data ?? []}
              cadastroCedenteSlot={<CadastroCedenteInline onCriado={setCedenteRecemCriadoId} />}
            />
          </Card>

          <SimulacaoResultCard
            dados={simulacao.data}
            carregando={simulacao.isFetching}
            erro={simulacao.error instanceof ApiError ? simulacao.error : null}
            pronto={simulacaoPronta}
            comErro={simulacaoComErro}
          />
        </div>
      )}
    </div>
  )
}
