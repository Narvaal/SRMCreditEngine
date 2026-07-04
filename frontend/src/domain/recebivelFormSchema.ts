import { z } from 'zod'

export const recebivelFormSchema = z.object({
  cedenteId: z.string().min(1, 'Selecione um cedente'),
  tipoRecebivelCodigo: z.string().min(1, 'Selecione o tipo'),
  valorFace: z.coerce.number({ message: 'Informe um valor' }).positive('O valor deve ser maior que zero'),
  dataVencimento: z.string().min(1, 'Informe o vencimento'),
  moedaTitulo: z.string().min(1, 'Selecione a moeda do título'),
  moedaPagamento: z.string().min(1, 'Selecione a moeda de pagamento'),
})

export type RecebivelFormInput = z.input<typeof recebivelFormSchema>
export type RecebivelFormOutput = z.output<typeof recebivelFormSchema>

/** Subconjunto que afeta o preço — cedenteId fica de fora (não influencia o cálculo). */
export const camposPrecificacaoSchema = recebivelFormSchema.omit({ cedenteId: true })
export type CamposPrecificacaoOutput = z.output<typeof camposPrecificacaoSchema>
