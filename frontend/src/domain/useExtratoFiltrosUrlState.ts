import { useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import type { ExtratoLiquidacaoFiltro } from '../api/types'
import { fimDoDiaSeguinteISO, inicioDoDiaISO } from '../lib/formatters'

const SIZE_PADRAO = 20

export interface FiltrosUrlState {
  cedenteId: string
  moeda: string
  tipo: string // '' | 'LIQUIDACAO' | 'ESTORNO'
  dataInicio: string // yyyy-MM-dd cru, direto do <input type="date">
  dataFim: string
  page: number
  size: number
}

type FiltrosEditaveis = Omit<FiltrosUrlState, 'page' | 'size'>

/**
 * Filtros e paginação vivem na URL (não em useState) — compartilhável, sobrevive a
 * refresh/voltar, e vira a queryKey de useExtratoLiquidacaoQuery sem duplicar estado em dois
 * lugares.
 */
export function useExtratoFiltrosUrlState() {
  const [searchParams, setSearchParams] = useSearchParams()

  const filtrosUrl: FiltrosUrlState = useMemo(
    () => ({
      cedenteId: searchParams.get('cedenteId') ?? '',
      moeda: searchParams.get('moeda') ?? '',
      tipo: searchParams.get('tipo') ?? '',
      dataInicio: searchParams.get('dataInicio') ?? '',
      dataFim: searchParams.get('dataFim') ?? '',
      page: Number(searchParams.get('page') ?? '0'),
      size: Number(searchParams.get('size') ?? String(SIZE_PADRAO)),
    }),
    [searchParams],
  )

  function atualizarFiltros(parcial: Partial<FiltrosEditaveis>) {
    setSearchParams((atual) => {
      const proximos = new URLSearchParams(atual)
      for (const [chave, valor] of Object.entries(parcial)) {
        if (valor) {
          proximos.set(chave, valor)
        } else {
          proximos.delete(chave)
        }
      }
      proximos.set('page', '0') // qualquer mudança de filtro volta pra primeira página
      return proximos
    })
  }

  function irParaPagina(pagina: number) {
    setSearchParams((atual) => {
      const proximos = new URLSearchParams(atual)
      proximos.set('page', String(pagina))
      return proximos
    })
  }

  // O backend filtra criado_em < dataFim (exclusivo) — conversão isolada aqui, nunca na apresentação.
  const filtroApi: ExtratoLiquidacaoFiltro = useMemo(
    () => ({
      cedenteId: filtrosUrl.cedenteId || undefined,
      moeda: filtrosUrl.moeda || undefined,
      tipo: filtrosUrl.tipo || undefined,
      dataInicio: filtrosUrl.dataInicio ? inicioDoDiaISO(filtrosUrl.dataInicio) : undefined,
      dataFim: filtrosUrl.dataFim ? fimDoDiaSeguinteISO(filtrosUrl.dataFim) : undefined,
      page: filtrosUrl.page,
      size: filtrosUrl.size,
    }),
    [filtrosUrl],
  )

  return { filtrosUrl, filtroApi, atualizarFiltros, irParaPagina }
}
