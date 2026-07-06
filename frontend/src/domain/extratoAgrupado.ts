import type { ExtratoLiquidacaoLinha } from '../api/types'

export interface TransacaoAgrupada {
  /** Linha mostrada na tabela — pro estorno, data/tipo do estorno com os dados da liquidação. */
  exibida: ExtratoLiquidacaoLinha
  /** Liquidação que originou o estorno — presente só quando ela veio na mesma página. */
  original?: ExtratoLiquidacaoLinha
}

/**
 * Funde cada ESTORNO com a LIQUIDACAO que ele desfez, pra tabela mostrar uma linha só por
 * operação (estado final). Transformação 100% de frontend — o backend segue devolvendo as duas.
 *
 * O extrato não traz o vínculo explícito, então o pareamento usa o recebível: um estorno desfaz
 * a liquidação estornada mais recente do mesmo recebível criada antes dele. Quando o par caiu em
 * outra página, cada linha continua aparecendo sozinha — nunca escondemos uma operação sem
 * conseguir mostrar o estado final dela.
 */
export function agruparEstornosComOriginal(linhas: ExtratoLiquidacaoLinha[]): TransacaoAgrupada[] {
  const liquidacoesEstornadasPorRecebivel = new Map<string, ExtratoLiquidacaoLinha[]>()
  for (const linha of linhas) {
    if (linha.tipo === 'LIQUIDACAO' && linha.estornada) {
      const lista = liquidacoesEstornadasPorRecebivel.get(linha.recebivelId) ?? []
      lista.push(linha)
      liquidacoesEstornadasPorRecebivel.set(linha.recebivelId, lista)
    }
  }

  const consumidas = new Set<string>()
  const originalPorEstorno = new Map<string, ExtratoLiquidacaoLinha>()
  for (const estorno of linhas.filter((linha) => linha.tipo === 'ESTORNO')) {
    const original = (liquidacoesEstornadasPorRecebivel.get(estorno.recebivelId) ?? [])
      .filter((liquidacao) => !consumidas.has(liquidacao.id) && liquidacao.criadoEm <= estorno.criadoEm)
      .reduce<ExtratoLiquidacaoLinha | null>(
        (maisRecente, atual) => (!maisRecente || atual.criadoEm > maisRecente.criadoEm ? atual : maisRecente),
        null,
      )
    if (original) {
      consumidas.add(original.id)
      originalPorEstorno.set(estorno.id, original)
    }
  }

  return linhas
    .filter((linha) => !consumidas.has(linha.id))
    .map((linha) => {
      const original = originalPorEstorno.get(linha.id)
      if (!original) return { exibida: linha }
      return {
        // data e tipo do estorno; cedente, moedas e valores da liquidação original
        exibida: {
          ...linha,
          cedenteId: original.cedenteId,
          cedenteNome: original.cedenteNome,
          moedaTitulo: original.moedaTitulo,
          moedaPagamento: original.moedaPagamento,
          valorFace: original.valorFace,
          valorLiquido: original.valorLiquido,
        },
        original,
      }
    })
}
