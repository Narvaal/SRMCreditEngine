import { describe, expect, it } from 'vitest'
import { camposPrecificacaoSchema, recebivelFormSchema } from './recebivelFormSchema'

function dataLocalYYYYMMDD(deslocamentoDias: number): string {
  const data = new Date()
  data.setDate(data.getDate() + deslocamentoDias)
  return `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}-${String(data.getDate()).padStart(2, '0')}`
}

const AMANHA = dataLocalYYYYMMDD(1)
const HOJE = dataLocalYYYYMMDD(0)
const ONTEM = dataLocalYYYYMMDD(-1)

describe('camposPrecificacaoSchema', () => {
  const valoresValidos = {
    tipoRecebivelCodigo: 'DUPLICATA_MERCANTIL',
    valorFace: '1000.00',
    dataVencimento: AMANHA,
    moedaTitulo: 'BRL',
    moedaPagamento: 'BRL',
  }

  it('aceita valores válidos sem cedenteId (não afeta o preço) e coage valorFace pra number', () => {
    const resultado = camposPrecificacaoSchema.safeParse(valoresValidos)

    expect(resultado.success).toBe(true)
    if (resultado.success) {
      expect(resultado.data.valorFace).toBe(1000)
    }
  })

  it('rejeita valorFace zero ou negativo', () => {
    expect(camposPrecificacaoSchema.safeParse({ ...valoresValidos, valorFace: '0' }).success).toBe(false)
    expect(camposPrecificacaoSchema.safeParse({ ...valoresValidos, valorFace: '-10' }).success).toBe(false)
  })

  it('rejeita campos obrigatórios vazios', () => {
    expect(camposPrecificacaoSchema.safeParse({ ...valoresValidos, tipoRecebivelCodigo: '' }).success).toBe(false)
    expect(camposPrecificacaoSchema.safeParse({ ...valoresValidos, moedaTitulo: '' }).success).toBe(false)
    expect(camposPrecificacaoSchema.safeParse({ ...valoresValidos, dataVencimento: '' }).success).toBe(false)
  })

  it('rejeita vencimento no passado ou hoje com mensagem clara — espelha o @Future do backend', () => {
    const passado = camposPrecificacaoSchema.safeParse({ ...valoresValidos, dataVencimento: ONTEM })
    expect(passado.success).toBe(false)
    if (!passado.success) {
      expect(passado.error.issues[0].message).toBe('O vencimento deve ser uma data futura.')
    }

    expect(camposPrecificacaoSchema.safeParse({ ...valoresValidos, dataVencimento: HOJE }).success).toBe(false)
  })
})

describe('recebivelFormSchema', () => {
  it('exige cedenteId além dos campos de precificação', () => {
    const resultado = recebivelFormSchema.safeParse({
      tipoRecebivelCodigo: 'DUPLICATA_MERCANTIL',
      valorFace: '1000.00',
      dataVencimento: AMANHA,
      moedaTitulo: 'BRL',
      moedaPagamento: 'BRL',
      cedenteId: '',
    })

    expect(resultado.success).toBe(false)
  })
})
