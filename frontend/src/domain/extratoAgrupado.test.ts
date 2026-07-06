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
    valorLiquido: 900,
    criadoEm: '2026-07-01T10:00:00Z',
    estornada: false,
    liquidacaoEstornadaId: null,
    liquidacaoEstornadaCriadoEm: null,
    ...sobrescreve,
  }
}

describe('agruparEstornosComOriginal', () => {
  it('funde estorno + liquidação referenciada numa linha só, na posição do estorno', () => {
    const liquidacao = linha({ id: 'liq', estornada: true })
    const estorno = linha({
      id: 'est',
      tipo: 'ESTORNO',
      criadoEm: '2026-07-02T09:00:00Z',
      liquidacaoEstornadaId: 'liq',
      liquidacaoEstornadaCriadoEm: '2026-07-01T10:00:00Z',
    })

    const resultado = agruparEstornosComOriginal([estorno, liquidacao])

    expect(resultado).toHaveLength(1)
    expect(resultado[0].exibida).toBe(estorno)
    expect(resultado[0].original).toBe(liquidacao)
  })

  it('liquidações comuns passam intactas, sem original', () => {
    const comum = linha({ id: 'liq-comum' })

    const resultado = agruparEstornosComOriginal([comum])

    expect(resultado).toEqual([{ exibida: comum }])
  })

  it('estorno cuja liquidação caiu em outra página reconstrói a original pela referência', () => {
    const estorno = linha({
      id: 'est',
      tipo: 'ESTORNO',
      valorFace: 222.22,
      valorLiquido: 182.92,
      criadoEm: '2026-07-02T09:14:00Z',
      liquidacaoEstornadaId: 'liq-em-outra-pagina',
      liquidacaoEstornadaCriadoEm: '2026-07-01T10:00:00Z',
    })

    const [{ exibida, original }] = agruparEstornosComOriginal([estorno])

    expect(exibida).toBe(estorno)
    // valores/cedente do estorno espelham a liquidação; id e data vêm da referência
    expect(original?.id).toBe('liq-em-outra-pagina')
    expect(original?.tipo).toBe('LIQUIDACAO')
    expect(original?.criadoEm).toBe('2026-07-01T10:00:00Z')
    expect(original?.valorFace).toBe(222.22)
    expect(original?.estornada).toBe(true)
  })

  it('liquidação estornada cujo estorno caiu em outra página continua aparecendo', () => {
    const liquidacao = linha({ id: 'liq', estornada: true })

    const resultado = agruparEstornosComOriginal([liquidacao])

    expect(resultado).toEqual([{ exibida: liquidacao }])
  })

  it('recebível re-liquidado após estorno: cada estorno pareia exatamente pela referência', () => {
    const liq1 = linha({ id: 'liq1', estornada: true, valorFace: 100, criadoEm: '2026-07-01T08:00:00Z' })
    const est1 = linha({
      id: 'est1',
      tipo: 'ESTORNO',
      criadoEm: '2026-07-01T09:00:00Z',
      liquidacaoEstornadaId: 'liq1',
      liquidacaoEstornadaCriadoEm: '2026-07-01T08:00:00Z',
    })
    const liq2 = linha({ id: 'liq2', estornada: true, valorFace: 200, criadoEm: '2026-07-01T10:00:00Z' })
    const est2 = linha({
      id: 'est2',
      tipo: 'ESTORNO',
      criadoEm: '2026-07-01T11:00:00Z',
      liquidacaoEstornadaId: 'liq2',
      liquidacaoEstornadaCriadoEm: '2026-07-01T10:00:00Z',
    })

    // ordem do extrato: mais recente primeiro
    const resultado = agruparEstornosComOriginal([est2, liq2, est1, liq1])

    expect(resultado).toHaveLength(2)
    expect(resultado[0].exibida.id).toBe('est2')
    expect(resultado[0].original?.id).toBe('liq2')
    expect(resultado[1].exibida.id).toBe('est1')
    expect(resultado[1].original?.id).toBe('liq1')
  })

  it('estorno legado sem referência aparece sozinho, sem expandir', () => {
    const estorno = linha({ id: 'est', tipo: 'ESTORNO' })

    const resultado = agruparEstornosComOriginal([estorno])

    expect(resultado).toEqual([{ exibida: estorno }])
  })
})
