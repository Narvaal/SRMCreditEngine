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

  it('rejeita valorFace acima de 1 quadrilhão (teto do NUMERIC(18,2) e da precisão do IEEE-754)', () => {
    expect(camposPrecificacaoSchema.safeParse({ ...valoresValidos, valorFace: '1000000000000000' }).success).toBe(true)

    const acima = camposPrecificacaoSchema.safeParse({ ...valoresValidos, valorFace: '1000000000000001' })
    expect(acima.success).toBe(false)
    if (!acima.success) {
      expect(acima.error.issues[0].message).toBe('Informe um valor de até 1 quadrilhão.')
    }
  })

  it('rejeita valorFace com mais de 2 casas decimais com mensagem clara', () => {
    expect(camposPrecificacaoSchema.safeParse({ ...valoresValidos, valorFace: '500000.03' }).success).toBe(true)
    expect(camposPrecificacaoSchema.safeParse({ ...valoresValidos, valorFace: '500,03' }).success).toBe(true)

    const tresCasas = camposPrecificacaoSchema.safeParse({ ...valoresValidos, valorFace: '500000.032' })
    expect(tresCasas.success).toBe(false)
    if (!tresCasas.success) {
      expect(tresCasas.error.issues[0].message).toBe('Informe um valor com até 2 casas decimais.')
    }
  })

  it('rejeita texto não numérico com mensagem própria (campo agora é type=text)', () => {
    const texto = camposPrecificacaoSchema.safeParse({ ...valoresValidos, valorFace: 'abc' })
    expect(texto.success).toBe(false)
    if (!texto.success) {
      expect(texto.error.issues[0].message).toBe('Informe um valor numérico.')
    }
  })

  it('valida as casas decimais na string digitada, não no float — decimais minúsculos não passam', () => {
    // Number('500.0000000000000012...') colapsa pra exatamente 500 no IEEE-754; a validação
    // precisa acontecer antes da conversão pra pegar o que o operador realmente digitou.
    const quaseQuinhentos = camposPrecificacaoSchema.safeParse({
      ...valoresValidos,
      valorFace: '500.0000000000000012355464564',
    })
    expect(quaseQuinhentos.success).toBe(false)
    if (!quaseQuinhentos.success) {
      expect(quaseQuinhentos.error.issues[0].message).toBe('Informe um valor com até 2 casas decimais.')
    }
  })

  it('rejeita vencimento além de 100 anos — erro de digitação, não negócio', () => {
    const alemDoHorizonte = camposPrecificacaoSchema.safeParse({ ...valoresValidos, dataVencimento: '7000-05-01' })
    expect(alemDoHorizonte.success).toBe(false)
    if (!alemDoHorizonte.success) {
      expect(alemDoHorizonte.error.issues[0].message).toBe('Informe uma data de no máximo 100 anos à frente.')
    }
  })

  it('rejeita vencimento no passado ou hoje com mensagem clara — espelha o @Future do backend', () => {
    const passado = camposPrecificacaoSchema.safeParse({ ...valoresValidos, dataVencimento: ONTEM })
    expect(passado.success).toBe(false)
    if (!passado.success) {
      expect(passado.error.issues[0].message).toBe('Informe uma data futura.')
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
