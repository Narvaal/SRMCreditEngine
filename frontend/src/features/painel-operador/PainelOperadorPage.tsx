import { ApiError } from '../../api/httpClient'
import { Alert, Card, Skeleton } from '../../components/ui'
import { useCedentes, useMoedas, useTiposRecebivel } from '../../domain/useCatalogos'
import { usePainelOperadorForm } from '../../domain/usePainelOperadorForm'
import { RecebivelForm } from './RecebivelForm'
import { SimulacaoResultCard } from './SimulacaoResultCard'

export function PainelOperadorPage() {
  const cedentesQuery = useCedentes()
  const tiposQuery = useTiposRecebivel()
  const moedasQuery = useMoedas()

  const { form, simulacao, simulacaoPronta, onSubmit, isSubmitting, resultadoEnvio } = usePainelOperadorForm()

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
            />
          </Card>

          <SimulacaoResultCard
            dados={simulacao.data}
            carregando={simulacao.isFetching}
            erro={simulacao.error instanceof ApiError ? simulacao.error : null}
            pronto={simulacaoPronta}
          />
        </div>
      )}
    </div>
  )
}
