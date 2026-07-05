/**
 * Máscara e validação de CPF/CNPJ — utilitário puro, sem dependência externa.
 * A validação de dígito verificador existe também no backend (@DocumentoValido);
 * aqui ela serve pra dar feedback imediato ao operador, não pra proteger a API.
 */

export function documentoSomenteDigitos(valor: string): string {
  return valor.replace(/\D/g, '')
}

/**
 * Formata progressivamente conforme a digitação: até 11 dígitos assume CPF
 * (000.000.000-00); do 12º em diante reformata como CNPJ (00.000.000/0000-00).
 */
export function mascararDocumento(valor: string): string {
  const digitos = documentoSomenteDigitos(valor).slice(0, 14)

  if (digitos.length <= 11) {
    return digitos
      .replace(/^(\d{3})(\d)/, '$1.$2')
      .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
      .replace(/^(\d{3})\.(\d{3})\.(\d{3})(\d)/, '$1.$2.$3-$4')
  }

  return digitos
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/^(\d{2})\.(\d{3})\.(\d{3})(\d)/, '$1.$2.$3/$4')
    .replace(/^(\d{2})\.(\d{3})\.(\d{3})\/(\d{4})(\d)/, '$1.$2.$3/$4-$5')
}

function digitoVerificador(digitos: string, pesos: number[]): number {
  const soma = pesos.reduce((acc, peso, i) => acc + Number(digitos[i]) * peso, 0)
  const resto = soma % 11
  return resto < 2 ? 0 : 11 - resto
}

function cpfValido(digitos: string): boolean {
  if (digitos.length !== 11 || /^(\d)\1{10}$/.test(digitos)) return false
  const dv1 = digitoVerificador(digitos, [10, 9, 8, 7, 6, 5, 4, 3, 2])
  const dv2 = digitoVerificador(digitos, [11, 10, 9, 8, 7, 6, 5, 4, 3, 2])
  return dv1 === Number(digitos[9]) && dv2 === Number(digitos[10])
}

function cnpjValido(digitos: string): boolean {
  if (digitos.length !== 14 || /^(\d)\1{13}$/.test(digitos)) return false
  const dv1 = digitoVerificador(digitos, [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2])
  const dv2 = digitoVerificador(digitos, [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2])
  return dv1 === Number(digitos[12]) && dv2 === Number(digitos[13])
}

/** Aceita valor com ou sem máscara; retorna o tipo do documento ou null se inválido. */
export function validarDocumento(valor: string): 'cpf' | 'cnpj' | null {
  const digitos = documentoSomenteDigitos(valor)
  if (cpfValido(digitos)) return 'cpf'
  if (cnpjValido(digitos)) return 'cnpj'
  return null
}

/**
 * Mensagem de erro contextual: se o operador digitou o tamanho exato de um CPF/CNPJ,
 * o problema é o dígito verificador — a mensagem nomeia o documento.
 */
export function mensagemErroDocumento(valor: string): string {
  const digitos = documentoSomenteDigitos(valor)
  if (digitos.length === 11) return 'CPF inválido.'
  if (digitos.length === 14) return 'CNPJ inválido.'
  return 'Informe um CPF ou CNPJ válido.'
}

const RAZAO_SOCIAL_CARACTERES_PERMITIDOS = /^[\p{L}\p{N} ]+$/u

/** Valida a razão social; retorna a mensagem de erro ou null se válida. */
export function validarRazaoSocial(valor: string): string | null {
  const nome = valor.trim()
  if (nome.length < 3 || nome.length > 20) {
    return 'A razão social deve ter entre 3 e 20 caracteres.'
  }
  if (!RAZAO_SOCIAL_CARACTERES_PERMITIDOS.test(nome)) {
    return 'A razão social não pode conter caracteres especiais.'
  }
  return null
}
