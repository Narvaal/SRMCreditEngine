import { describe, expect, it } from 'vitest'
import {
  documentoSomenteDigitos,
  mascararDocumento,
  mensagemErroDocumento,
  validarDocumento,
  validarRazaoSocial,
} from './documento'

// CPF/CNPJ com dígitos verificadores corretos (gerados pelo algoritmo módulo 11)
const CPF_VALIDO = '52998224725'
const CNPJ_VALIDO = '11444777000161'

describe('mascararDocumento', () => {
  it('formata progressivamente como CPF enquanto tem até 11 dígitos', () => {
    expect(mascararDocumento('5')).toBe('5')
    expect(mascararDocumento('5299')).toBe('529.9')
    expect(mascararDocumento('5299822')).toBe('529.982.2')
    expect(mascararDocumento(CPF_VALIDO)).toBe('529.982.247-25')
  })

  it('reformata como CNPJ a partir do 12º dígito', () => {
    expect(mascararDocumento('114447770001')).toBe('11.444.777/0001')
    expect(mascararDocumento(CNPJ_VALIDO)).toBe('11.444.777/0001-61')
  })

  it('descarta caracteres não numéricos e limita a 14 dígitos', () => {
    expect(mascararDocumento('a1b1c4d4e4f777')).toBe('114.447.77')
    expect(mascararDocumento('114447770001619999')).toBe('11.444.777/0001-61')
  })
})

describe('validarDocumento', () => {
  it('reconhece CPF com dígito verificador correto, com ou sem máscara', () => {
    expect(validarDocumento(CPF_VALIDO)).toBe('cpf')
    expect(validarDocumento('529.982.247-25')).toBe('cpf')
  })

  it('reconhece CNPJ com dígito verificador correto, com ou sem máscara', () => {
    expect(validarDocumento(CNPJ_VALIDO)).toBe('cnpj')
    expect(validarDocumento('11.444.777/0001-61')).toBe('cnpj')
  })

  it('rejeita dígito verificador errado', () => {
    expect(validarDocumento('52998224726')).toBeNull()
    expect(validarDocumento('11444777000162')).toBeNull()
  })

  it('rejeita sequências de dígitos repetidos mesmo com DV "válido"', () => {
    expect(validarDocumento('11111111111')).toBeNull()
    expect(validarDocumento('00000000000000')).toBeNull()
  })

  it('rejeita tamanhos intermediários e texto livre', () => {
    expect(validarDocumento('a')).toBeNull()
    expect(validarDocumento('123')).toBeNull()
    expect(validarDocumento('')).toBeNull()
  })
})

describe('mensagemErroDocumento', () => {
  it('nomeia CPF quando há exatamente 11 dígitos', () => {
    expect(mensagemErroDocumento('529.982.247-26')).toBe('CPF inválido.')
  })

  it('nomeia CNPJ quando há exatamente 14 dígitos', () => {
    expect(mensagemErroDocumento('11.444.777/0001-62')).toBe('CNPJ inválido.')
  })

  it('usa mensagem genérica para tamanhos incompletos', () => {
    expect(mensagemErroDocumento('a')).toBe('Informe um CPF ou CNPJ válido.')
  })
})

describe('documentoSomenteDigitos', () => {
  it('remove a máscara para envio à API', () => {
    expect(documentoSomenteDigitos('529.982.247-25')).toBe(CPF_VALIDO)
  })
})

describe('validarRazaoSocial', () => {
  it('aceita nomes de 3 a 20 caracteres com letras, números, acentos e espaços', () => {
    expect(validarRazaoSocial('SRM Asset 2')).toBeNull()
    expect(validarRazaoSocial('Açaí do João')).toBeNull()
  })

  it('rejeita nomes muito curtos ou muito longos', () => {
    expect(validarRazaoSocial('ab')).toBe('A razão social deve ter entre 3 e 20 caracteres.')
    expect(validarRazaoSocial('Uma razão social grande demais')).toBe(
      'A razão social deve ter entre 3 e 20 caracteres.',
    )
  })

  it('rejeita caracteres especiais', () => {
    expect(validarRazaoSocial('Empresa & Cia')).toBe('A razão social não pode conter caracteres especiais.')
    expect(validarRazaoSocial('a.a.a')).toBe('A razão social não pode conter caracteres especiais.')
  })

  it('mede o tamanho após o trim', () => {
    expect(validarRazaoSocial('  ab  ')).toBe('A razão social deve ter entre 3 e 20 caracteres.')
  })
})
