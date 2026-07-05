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

// Mensagens sempre no imperativo ("Informe...", "Selecione...", "Use...") — padrão do produto.
export const recebivelFormSchema = z.object({
  cedenteId: z.string().min(1, 'Selecione um cedente.'),
  tipoRecebivelCodigo: z.string().min(1, 'Selecione um tipo de recebível.'),
  // As casas decimais são validadas na STRING digitada, antes de virar number — depois da
  // conversão, "500,0000000000000012" já colapsou pra exatamente 500 no float e o erro some.
  valorFace: z
    .union([z.string(), z.number()], { message: 'Informe um valor.' })
    .transform(String)
    .pipe(
      z
        .string()
        .min(1, 'Informe um valor.')
        .regex(/^-?\d+([.,]\d*)?$/, 'Informe um valor numérico.')
        .regex(/^-?\d+([.,]\d{1,2})?$/, 'Informe um valor com até 2 casas decimais.'),
    )
    .transform((valor) => Number(valor.replace(',', '.')))
    .pipe(
      z
        .number()
        .positive('Informe um valor maior que 0.')
        .lte(VALOR_FACE_MAXIMO, 'Informe um valor de até 1 quadrilhão.'),
    ),
  dataVencimento: z
    .string()
    .min(1, 'Informe o vencimento.')
    .refine(dataFutura, 'Informe uma data futura.')
    .refine(
      (data) => data <= vencimentoMaximoYYYYMMDD(),
      `Informe uma data de no máximo ${VENCIMENTO_HORIZONTE_ANOS} anos à frente.`,
    ),
  moedaTitulo: z.string().min(1, 'Selecione a moeda do título.'),
  moedaPagamento: z.string().min(1, 'Selecione a moeda de pagamento.'),
})

export type RecebivelFormInput = z.input<typeof recebivelFormSchema>
export type RecebivelFormOutput = z.output<typeof recebivelFormSchema>

/** Subconjunto que afeta o preço — cedenteId fica de fora (não influencia o cálculo). */
export const camposPrecificacaoSchema = recebivelFormSchema.omit({ cedenteId: true })
export type CamposPrecificacaoOutput = z.output<typeof camposPrecificacaoSchema>
