import { describe, expect, it } from 'vitest'
import type { ExtratoLiquidacaoLinha } from '../api/types'
import { agruparEstornosComOriginal } from './extratoAgrupado'

function linha(sobrescreve: Partial<ExtratoLiquidacaoLinha>): ExtratoLiquidacaoLinha {
  return {
    id: 'id',
    recebivelId: 'r1',
    cedenteId: 'c1',
    cedenteNome: 'Acme Ltda',
    tipo: 'LIQUIDACAO',
    moedaTitulo: 'BRL',
    moedaPagamento: 'BRL',
    valorFace: 1000,
    valorPresente: 900,
    valorLiquido: 900,
    criadoEm: '2026-07-01T10:00:00Z',
    liquidacaoEstornadaId: null,
    liquidacaoEstornadaCriadoEm: null,
    ...sobrescreve,
  }
}

describe('agruparEstornosComOriginal', () => {
  it('liquidações passam intactas, sem original', () => {
    const comum = linha({ id: 'liq-comum' })

    const resultado = agruparEstornosComOriginal([comum])

    expect(resultado).toEqual([{ exibida: comum }])
  })

  it('estorno com referência materializa a operação original a partir da própria linha', () => {
    const estorno = linha({
      id: 'est',
      tipo: 'ESTORNO',
      cedenteNome: 'Cedente Original',
      valorFace: 222.22,
      valorLiquido: 182.92,
      criadoEm: '2026-07-07T09:14:00Z',
      liquidacaoEstornadaId: 'liq-original',
      liquidacaoEstornadaCriadoEm: '2026-07-05T20:52:00Z',
    })

    const [{ exibida, original }] = agruparEstornosComOriginal([estorno])

    expect(exibida).toBe(estorno)
    // id e data vêm da referência; cedente/moedas/valores espelham os do estorno
    expect(original?.id).toBe('liq-original')
    expect(original?.tipo).toBe('LIQUIDACAO')
    expect(original?.criadoEm).toBe('2026-07-05T20:52:00Z')
    expect(original?.cedenteNome).toBe('Cedente Original')
    expect(original?.valorFace).toBe(222.22)
    expect(original?.liquidacaoEstornadaId).toBeNull()
  })

  it('estorno legado sem referência aparece como linha simples, sem expandir', () => {
    const estorno = linha({ id: 'est', tipo: 'ESTORNO' })

    const resultado = agruparEstornosComOriginal([estorno])

    expect(resultado).toEqual([{ exibida: estorno }])
  })

  it('preserva a ordem das linhas recebidas', () => {
    const linhas = [
      linha({ id: 'a', tipo: 'ESTORNO', liquidacaoEstornadaId: 'x', liquidacaoEstornadaCriadoEm: '2026-07-01T00:00:00Z' }),
      linha({ id: 'b' }),
      linha({ id: 'c' }),
    ]

    const resultado = agruparEstornosComOriginal(linhas)

    expect(resultado.map((t) => t.exibida.id)).toEqual(['a', 'b', 'c'])
  })
})
