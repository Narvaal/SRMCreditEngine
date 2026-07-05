import { describe, expect, it } from 'vitest'
import { calcularDesagioPercentual, fimDoDiaSeguinteISO, formatarMoeda, inicioDoDiaISO, simboloMoeda } from './formatters'

describe('formatarMoeda', () => {
  it('formata em BRL', () => {
    expect(formatarMoeda(1234.5, 'BRL')).toContain('1.234,50')
  })

  it('formata em USD', () => {
    expect(formatarMoeda(875.46, 'USD')).toContain('875,46')
  })
})

describe('simboloMoeda', () => {
  it('extrai o símbolo pt-BR da moeda', () => {
    expect(simboloMoeda('BRL')).toBe('R$')
    expect(simboloMoeda('USD')).toBe('US$')
  })
})

describe('inicioDoDiaISO / fimDoDiaSeguinteISO', () => {
  it('inicioDoDiaISO mantém o mesmo dia', () => {
    expect(inicioDoDiaISO('2026-07-03')).toBe('2026-07-03T00:00:00.000Z')
  })

  it('fimDoDiaSeguinteISO avança um dia — garante que o filtro "criado_em < dataFim" (exclusivo no backend) inclui o dia inteiro escolhido', () => {
    expect(fimDoDiaSeguinteISO('2026-07-03')).toBe('2026-07-04T00:00:00.000Z')
  })

  it('fimDoDiaSeguinteISO atravessa corretamente a virada de mês', () => {
    expect(fimDoDiaSeguinteISO('2026-07-31')).toBe('2026-08-01T00:00:00.000Z')
  })
})

describe('calcularDesagioPercentual', () => {
  it('calcula a diferença percentual entre valor de face e valor líquido', () => {
    expect(calcularDesagioPercentual(1000, 900)).toBeCloseTo(0.1)
  })

  it('não divide por zero quando valorFace é 0', () => {
    expect(calcularDesagioPercentual(0, 0)).toBe(0)
  })
})
