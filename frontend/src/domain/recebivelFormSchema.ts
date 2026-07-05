import { z } from 'zod'

/**
 * 1 quadrilhão — teto de negócio pro valor de face. Fica abaixo do NUMERIC(18,2) do banco
 * (16 dígitos inteiros) e da precisão inteira exata do IEEE-754 (2^53 ≈ 9 quadrilhões).
 */
export const VALOR_FACE_MAXIMO = 1_000_000_000_000_000

/** Horizonte máximo de vencimento, em anos — datas além disso são erro de digitação, não negócio. */
export const VENCIMENTO_HORIZONTE_ANOS = 100

function dataLocalYYYYMMDD(data: Date): string {
  return `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, '0')}-${String(data.getDate()).padStart(2, '0')}`
}

/** Espelha o @Future do backend: só datas estritamente posteriores a hoje (fuso local do operador). */
function dataFutura(dataYYYYMMDD: string): boolean {
  return dataYYYYMMDD > dataLocalYYYYMMDD(new Date())
}

/** Limite superior do vencimento: hoje + VENCIMENTO_HORIZONTE_ANOS. */
export function vencimentoMaximoYYYYMMDD(): string {
  const limite = new Date()
  limite.setFullYear(limite.getFullYear() + VENCIMENTO_HORIZONTE_ANOS)
  return dataLocalYYYYMMDD(limite)
}

export const recebivelFormSchema = z.object({
  cedenteId: z.string().min(1, 'Selecione um cedente'),
  tipoRecebivelCodigo: z.string().min(1, 'Selecione o tipo'),
  valorFace: z.coerce
    .number({ message: 'Informe um valor' })
    .positive('O valor deve ser maior que zero')
    .lte(VALOR_FACE_MAXIMO, 'O valor máximo é 1 quadrilhão.'),
  dataVencimento: z
    .string()
    .min(1, 'Informe o vencimento')
    .refine(dataFutura, 'O vencimento deve ser uma data futura.')
    .refine(
      (data) => data <= vencimentoMaximoYYYYMMDD(),
      `O vencimento não pode passar de ${VENCIMENTO_HORIZONTE_ANOS} anos.`,
    ),
  moedaTitulo: z.string().min(1, 'Selecione a moeda do título'),
  moedaPagamento: z.string().min(1, 'Selecione a moeda de pagamento'),
})

export type RecebivelFormInput = z.input<typeof recebivelFormSchema>
export type RecebivelFormOutput = z.output<typeof recebivelFormSchema>

/** Subconjunto que afeta o preço — cedenteId fica de fora (não influencia o cálculo). */
export const camposPrecificacaoSchema = recebivelFormSchema.omit({ cedenteId: true })
export type CamposPrecificacaoOutput = z.output<typeof camposPrecificacaoSchema>
