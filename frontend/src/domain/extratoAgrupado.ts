import type { ExtratoLiquidacaoLinha } from '../api/types'

export interface TransacaoAgrupada {
  /** Linha mostrada na tabela. */
  exibida: ExtratoLiquidacaoLinha
  /** Liquidação que originou o estorno — presente em todo estorno com referência. */
  original?: ExtratoLiquidacaoLinha
}

/**
 * O extrato já devolve o estado final (liquidações estornadas não vêm); aqui só se materializa a
 * "Operação original" de cada estorno a partir da referência que a linha carrega — os
 * valores/cedente/moedas do estorno espelham os da liquidação desfeita, e id/data vêm da
 * referência. Estornos legados sem referência aparecem como linha simples.
 */
export function agruparEstornosComOriginal(linhas: ExtratoLiquidacaoLinha[]): TransacaoAgrupada[] {
  return linhas.map((linha) => {
    if (linha.tipo !== 'ESTORNO' || !linha.liquidacaoEstornadaId) {
      return { exibida: linha }
    }
    const original: ExtratoLiquidacaoLinha = {
      ...linha,
      id: linha.liquidacaoEstornadaId,
      tipo: 'LIQUIDACAO',
      criadoEm: linha.liquidacaoEstornadaCriadoEm ?? linha.criadoEm,
      liquidacaoEstornadaId: null,
      liquidacaoEstornadaCriadoEm: null,
    }
    return { exibida: linha, original }
  })
}
