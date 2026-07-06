import { useMemo, useState } from 'react'
import { ApiError } from '../../api/httpClient'
import type { ExtratoLiquidacaoLinha } from '../../api/types'
import { Card, Pagination, Skeleton, Toast } from '../../components/ui'
import { agruparEstornosComOriginal } from '../../domain/extratoAgrupado'
import { useCedentes, useMoedas } from '../../domain/useCatalogos'
import { useEstornarLiquidacao } from '../../domain/useEstornarLiquidacao'
import { useExtratoFiltrosUrlState } from '../../domain/useExtratoFiltrosUrlState'
import { useExtratoLiquidacaoQuery } from '../../domain/useExtratoLiquidacaoQuery'
import { EstornoModal } from './EstornoModal'
import { FiltrosTransacoes } from './FiltrosTransacoes'
import { TransacoesTable } from './TransacoesTable'

interface ToastEstorno {
  tipo: 'success' | 'error'
  mensagem: string
}

export function GridTransacoesPage() {
  const { filtrosUrl, filtroApi, atualizarFiltros, irParaPagina } = useExtratoFiltrosUrlState()
  const extratoQuery = useExtratoLiquidacaoQuery(filtroApi)
  const cedentesQuery = useCedentes()
  const moedasQuery = useMoedas()
  const estornoMutation = useEstornarLiquidacao()

  // Linha selecionada pro estorno — o modal só confirma; o resultado da mutação vai pro toast.
  const [linhaParaEstorno, setLinhaParaEstorno] = useState<ExtratoLiquidacaoLinha | null>(null)
  const [toast, setToast] = useState<ToastEstorno | null>(null)

  // Estorno + liquidação original viram uma linha só (estado final) — transformação de exibição.
  const transacoes = useMemo(() => agruparEstornosComOriginal(extratoQuery.data?.content ?? []), [extratoQuery.data])

  function confirmarEstorno() {
    if (!linhaParaEstorno) return
    const liquidacaoId = linhaParaEstorno.id
    setLinhaParaEstorno(null)
    setToast(null)
    estornoMutation.mutate(liquidacaoId, {
      onSuccess: () => setToast({ tipo: 'success', mensagem: 'Liquidação estornada com sucesso.' }),
      onError: (erro) =>
        setToast({
          tipo: 'error',
          mensagem: erro instanceof ApiError && erro.message ? erro.message : 'Não foi possível estornar a liquidação.',
        }),
    })
  }

  return (
    <div className="mx-auto max-w-6xl">
      <h1 className="mb-1 text-2xl font-semibold text-ink">Grid de Transações</h1>
      <p className="mb-6 text-sm text-ink-muted">Histórico de liquidações e estornos, com filtros e paginação.</p>

      <Card className="mb-6">
        <FiltrosTransacoes
          cedenteId={filtrosUrl.cedenteId}
          moeda={filtrosUrl.moeda}
          tipo={filtrosUrl.tipo}
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
          <TransacoesTable
            transacoes={transacoes}
            onEstornar={setLinhaParaEstorno}
            estornoEmAndamento={estornoMutation.isPending}
          />
          <Pagination page={filtrosUrl.page} totalPages={extratoQuery.data?.totalPages ?? 0} onPageChange={irParaPagina} />
        </>
      )}

      {linhaParaEstorno && (
        <EstornoModal linha={linhaParaEstorno} onConfirmar={confirmarEstorno} onFechar={() => setLinhaParaEstorno(null)} />
      )}

      {toast && (
        <Toast tipo={toast.tipo} onFechar={() => setToast(null)}>
          {toast.mensagem}
        </Toast>
      )}
    </div>
  )
}
