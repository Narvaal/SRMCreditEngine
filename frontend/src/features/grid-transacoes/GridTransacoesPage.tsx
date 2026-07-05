import { useState } from 'react'
import { ApiError } from '../../api/httpClient'
import type { ExtratoLiquidacaoLinha } from '../../api/types'
import { Card, Pagination, Skeleton } from '../../components/ui'
import { useCedentes, useMoedas } from '../../domain/useCatalogos'
import { useEstornarLiquidacao } from '../../domain/useEstornarLiquidacao'
import { useExtratoFiltrosUrlState } from '../../domain/useExtratoFiltrosUrlState'
import { useExtratoLiquidacaoQuery } from '../../domain/useExtratoLiquidacaoQuery'
import { EstornoModal } from './EstornoModal'
import { FiltrosTransacoes } from './FiltrosTransacoes'
import { TransacoesTable } from './TransacoesTable'

export function GridTransacoesPage() {
  const { filtrosUrl, filtroApi, atualizarFiltros, irParaPagina } = useExtratoFiltrosUrlState()
  const extratoQuery = useExtratoLiquidacaoQuery(filtroApi)
  const cedentesQuery = useCedentes()
  const moedasQuery = useMoedas()
  const estornoMutation = useEstornarLiquidacao()

  // Linha selecionada pro estorno — o modal mostra os dados completos e o resultado da mutação.
  const [linhaParaEstorno, setLinhaParaEstorno] = useState<ExtratoLiquidacaoLinha | null>(null)

  const erroEstorno = estornoMutation.error instanceof ApiError ? estornoMutation.error : null

  function fecharModal() {
    setLinhaParaEstorno(null)
    estornoMutation.reset()
  }

  return (
    <div className="mx-auto max-w-6xl">
      <h1 className="mb-1 text-2xl font-semibold text-ink">Grid de Transações</h1>
      <p className="mb-6 text-sm text-ink-muted">Histórico de liquidações e estornos, com filtros e paginação.</p>

      <Card className="mb-6">
        <FiltrosTransacoes
          cedenteId={filtrosUrl.cedenteId}
          moeda={filtrosUrl.moeda}
          dataInicio={filtrosUrl.dataInicio}
          dataFim={filtrosUrl.dataFim}
          cedentes={cedentesQuery.data ?? []}
          moedas={moedasQuery.data ?? []}
          onChange={atualizarFiltros}
        />
      </Card>

      {extratoQuery.isLoading ? (
        <Skeleton className="h-64 w-full" />
      ) : (
        <>
          <TransacoesTable linhas={extratoQuery.data?.content ?? []} onEstornar={setLinhaParaEstorno} />
          <Pagination page={filtrosUrl.page} totalPages={extratoQuery.data?.totalPages ?? 0} onPageChange={irParaPagina} />
        </>
      )}

      {linhaParaEstorno && (
        <EstornoModal
          linha={linhaParaEstorno}
          estornando={estornoMutation.isPending}
          sucesso={estornoMutation.isSuccess}
          erro={erroEstorno}
          onConfirmar={() => estornoMutation.mutate(linhaParaEstorno.id)}
          onFechar={fecharModal}
        />
      )}
    </div>
  )
}
