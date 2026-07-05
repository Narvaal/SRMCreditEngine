const formatadoresPorMoeda = new Map<string, Intl.NumberFormat>()

/** Formata um valor monetário respeitando a moeda (BRL/USD) — cache de Intl.NumberFormat por moeda. */
export function formatarMoeda(valor: number, moeda: string): string {
  let formatador = formatadoresPorMoeda.get(moeda)
  if (!formatador) {
    formatador = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: moeda })
    formatadoresPorMoeda.set(moeda, formatador)
  }
  return formatador.format(valor)
}

/**
 * Máscara de digitação pt-BR pro valor monetário: pontos de milhar automáticos, vírgula como
 * separador decimal. Um ponto digitado em posição de decimal (até 2 dígitos no fim) é tratado
 * como vírgula — quem digita "500.03" no teclado numérico quer 500,03, não 50003. Não limita as
 * casas decimais de propósito: excesso vira erro visível do Zod, nunca truncamento silencioso.
 */
export function mascararValorBR(entrada: string): string {
  const comDecimalNormalizado = entrada.replace(/\.(?=\d{0,2}$)/, ',')
  const limpo = comDecimalNormalizado.replace(/[^\d,]/g, '')
  const [inteiro, ...decimais] = limpo.split(',')
  const inteiroComMilhar = inteiro.replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  return limpo.includes(',') ? `${inteiroComMilhar},${decimais.join('')}` : inteiroComMilhar
}

/** Símbolo da moeda na convenção pt-BR (BRL → "R$", USD → "US$") — extraído do próprio Intl. */
export function simboloMoeda(moeda: string): string {
  const partes = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: moeda }).formatToParts(0)
  return partes.find((parte) => parte.type === 'currency')?.value ?? moeda
}

export function formatarPercentual(valor: number, casasDecimais = 2): string {
  return `${(valor * 100).toFixed(casasDecimais)}%`
}

const formatadorData = new Intl.DateTimeFormat('pt-BR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

export function formatarDataHora(isoString: string): string {
  return formatadorData.format(new Date(isoString))
}

/**
 * O backend filtra `criado_em < dataFim` (exclusivo — ver ExtratoLiquidacaoRepository). Um "até"
 * escolhido pelo operador deve incluir o dia inteiro, então convertemos para o início do dia
 * seguinte antes de mandar pra API.
 */
export function fimDoDiaSeguinteISO(dataYYYYMMDD: string): string {
  const data = new Date(`${dataYYYYMMDD}T00:00:00Z`)
  data.setUTCDate(data.getUTCDate() + 1)
  return data.toISOString()
}

export function inicioDoDiaISO(dataYYYYMMDD: string): string {
  return new Date(`${dataYYYYMMDD}T00:00:00Z`).toISOString()
}

/** Deságio: quanto do valor de face foi descontado, em %. Aritmética de exibição — não repete regra de negócio. */
export function calcularDesagioPercentual(valorFace: number, valorLiquido: number): number {
  if (valorFace === 0) return 0
  return (valorFace - valorLiquido) / valorFace
}
