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
    ...sobrescreve,
  }
}

describe('agruparEstornosComOriginal', () => {
  it('funde estorno + liquidação estornada do mesmo recebível numa linha só', () => {
    const liquidacao = linha({ id: 'liq', estornada: true, criadoEm: '2026-07-01T10:00:00Z' })
    const estorno = linha({ id: 'est', tipo: 'ESTORNO', criadoEm: '2026-07-02T09:00:00Z' })

    const resultado = agruparEstornosComOriginal([estorno, liquidacao])

    expect(resultado).toHaveLength(1)
    expect(resultado[0].exibida.tipo).toBe('ESTORNO')
    expect(resultado[0].original).toBe(liquidacao)
  })

  it('a linha exibida usa a data do estorno com os dados da liquidação original', () => {
    const liquidacao = linha({
      id: 'liq',
      estornada: true,
      cedenteNome: 'Cedente Original',
      valorFace: 222.22,
      valorLiquido: 182.92,
      criadoEm: '2026-07-01T10:00:00Z',
    })
    const estorno = linha({ id: 'est', tipo: 'ESTORNO', criadoEm: '2026-07-02T09:14:00Z' })

    const [{ exibida }] = agruparEstornosComOriginal([estorno, liquidacao])

    expect(exibida.criadoEm).toBe('2026-07-02T09:14:00Z')
    expect(exibida.cedenteNome).toBe('Cedente Original')
    expect(exibida.valorFace).toBe(222.22)
    expect(exibida.valorLiquido).toBe(182.92)
  })

  it('liquidações comuns passam intactas, sem original', () => {
    const comum = linha({ id: 'liq-comum' })

    const resultado = agruparEstornosComOriginal([comum])

    expect(resultado).toEqual([{ exibida: comum }])
  })

  it('estorno cuja liquidação caiu em outra página aparece sozinho, sem expandir', () => {
    const estorno = linha({ id: 'est', tipo: 'ESTORNO' })

    const resultado = agruparEstornosComOriginal([estorno])

    expect(resultado).toEqual([{ exibida: estorno }])
  })

  it('liquidação estornada cujo estorno caiu em outra página continua aparecendo', () => {
    const liquidacao = linha({ id: 'liq', estornada: true })

    const resultado = agruparEstornosComOriginal([liquidacao])

    expect(resultado).toEqual([{ exibida: liquidacao }])
  })

  it('recebível re-liquidado após estorno: cada estorno pareia com a liquidação certa', () => {
    const liq1 = linha({ id: 'liq1', estornada: true, valorFace: 100, criadoEm: '2026-07-01T08:00:00Z' })
    const est1 = linha({ id: 'est1', tipo: 'ESTORNO', criadoEm: '2026-07-01T09:00:00Z' })
    const liq2 = linha({ id: 'liq2', estornada: true, valorFace: 200, criadoEm: '2026-07-01T10:00:00Z' })
    const est2 = linha({ id: 'est2', tipo: 'ESTORNO', criadoEm: '2026-07-01T11:00:00Z' })

    // ordem do extrato: mais recente primeiro
    const resultado = agruparEstornosComOriginal([est2, liq2, est1, liq1])

    expect(resultado).toHaveLength(2)
    expect(resultado[0].exibida.id).toBe('est2')
    expect(resultado[0].original?.id).toBe('liq2')
    expect(resultado[1].exibida.id).toBe('est1')
    expect(resultado[1].original?.id).toBe('liq1')
  })

  it('não pareia estorno com liquidação de outro recebível', () => {
    const liquidacaoOutroRecebivel = linha({ id: 'liq', estornada: true, recebivelId: 'r2' })
    const estorno = linha({ id: 'est', tipo: 'ESTORNO', recebivelId: 'r1', criadoEm: '2026-07-02T09:00:00Z' })

    const resultado = agruparEstornosComOriginal([estorno, liquidacaoOutroRecebivel])

    expect(resultado).toHaveLength(2)
    expect(resultado.find((t) => t.exibida.id === 'est')?.original).toBeUndefined()
  })
})
