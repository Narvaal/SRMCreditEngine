import type { ExtratoLiquidacaoLinha } from '../api/types'

export interface TransacaoAgrupada {
  /** Linha mostrada na tabela — pro estorno, data/tipo do estorno com os dados da liquidação. */
  exibida: ExtratoLiquidacaoLinha
  /** Liquidação que originou o estorno — presente em todo estorno. */
  original?: ExtratoLiquidacaoLinha
}

/**
 * Funde cada ESTORNO com a LIQUIDACAO que ele desfez, pra tabela mostrar uma linha só por
 * operação (estado final). O vínculo vem do próprio extrato (liquidacaoEstornadaId), então o
 * pareamento é exato e independe da página em que cada linha caiu:
 *
 * - a liquidação estornada some da página quando o estorno dela também está aqui;
 * - todo estorno é expansível — se a original não veio na página, ela é reconstruída a partir
 *   da referência (os valores/cedente/moedas do estorno espelham os da liquidação desfeita).
 */
export function agruparEstornosComOriginal(linhas: ExtratoLiquidacaoLinha[]): TransacaoAgrupada[] {
  const porId = new Map(linhas.map((linha) => [linha.id, linha]))

  const fundidas = new Set<string>()
  for (const linha of linhas) {
    if (linha.tipo === 'ESTORNO' && linha.liquidacaoEstornadaId && porId.has(linha.liquidacaoEstornadaId)) {
      fundidas.add(linha.liquidacaoEstornadaId)
    }
  }

  return linhas
    .filter((linha) => !fundidas.has(linha.id))
    .map((linha) => {
      if (linha.tipo !== 'ESTORNO' || !linha.liquidacaoEstornadaId) {
        return { exibida: linha }
      }
      const original: ExtratoLiquidacaoLinha = porId.get(linha.liquidacaoEstornadaId) ?? {
        ...linha,
        id: linha.liquidacaoEstornadaId,
        tipo: 'LIQUIDACAO',
        criadoEm: linha.liquidacaoEstornadaCriadoEm ?? linha.criadoEm,
        estornada: true,
        liquidacaoEstornadaId: null,
        liquidacaoEstornadaCriadoEm: null,
      }
      return { exibida: linha, original }
    })
}
