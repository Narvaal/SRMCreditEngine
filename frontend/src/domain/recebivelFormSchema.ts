import { z } from 'zod'

/** Espelha o @Future do backend: só datas estritamente posteriores a hoje (fuso local do operador). */
function dataFutura(dataYYYYMMDD: string): boolean {
  const hoje = new Date()
  const hojeYYYYMMDD = `${hoje.getFullYear()}-${String(hoje.getMonth() + 1).padStart(2, '0')}-${String(hoje.getDate()).padStart(2, '0')}`
  return dataYYYYMMDD > hojeYYYYMMDD
}

export const recebivelFormSchema = z.object({
  cedenteId: z.string().min(1, 'Selecione um cedente'),
  tipoRecebivelCodigo: z.string().min(1, 'Selecione o tipo'),
  valorFace: z.coerce.number({ message: 'Informe um valor' }).positive('O valor deve ser maior que zero'),
  dataVencimento: z
    .string()
    .min(1, 'Informe o vencimento')
    .refine(dataFutura, 'O vencimento deve ser uma data futura.'),
  moedaTitulo: z.string().min(1, 'Selecione a moeda do título'),
  moedaPagamento: z.string().min(1, 'Selecione a moeda de pagamento'),
})

export type RecebivelFormInput = z.input<typeof recebivelFormSchema>
export type RecebivelFormOutput = z.output<typeof recebivelFormSchema>

/** Subconjunto que afeta o preço — cedenteId fica de fora (não influencia o cálculo). */
export const camposPrecificacaoSchema = recebivelFormSchema.omit({ cedenteId: true })
export type CamposPrecificacaoOutput = z.output<typeof camposPrecificacaoSchema>
