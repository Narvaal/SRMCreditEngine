import { describe, expect, it } from 'vitest'
import { camposPrecificacaoSchema, recebivelFormSchema } from './recebivelFormSchema'

describe('camposPrecificacaoSchema', () => {
  const valoresValidos = {
    tipoRecebivelCodigo: 'DUPLICATA_MERCANTIL',
    valorFace: '1000.00',
    dataVencimento: '2026-09-15',
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
})

describe('recebivelFormSchema', () => {
  it('exige cedenteId além dos campos de precificação', () => {
    const resultado = recebivelFormSchema.safeParse({
      tipoRecebivelCodigo: 'DUPLICATA_MERCANTIL',
      valorFace: '1000.00',
      dataVencimento: '2026-09-15',
      moedaTitulo: 'BRL',
      moedaPagamento: 'BRL',
      cedenteId: '',
    })

    expect(resultado.success).toBe(false)
  })
})
