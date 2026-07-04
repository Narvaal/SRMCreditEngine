import type { UseFormReturn } from 'react-hook-form'
import type { Cedente, Moeda, TipoRecebivel } from '../../api/types'
import { Button, DateField, Input, Select } from '../../components/ui'
import type { RecebivelFormInput, RecebivelFormOutput } from '../../domain/recebivelFormSchema'

interface RecebivelFormProps {
  form: UseFormReturn<RecebivelFormInput, unknown, RecebivelFormOutput>
  onSubmit: () => void
  isSubmitting: boolean
  cedentes: Cedente[]
  tiposRecebivel: TipoRecebivel[]
  moedas: Moeda[]
}

/** Componente puro: só renderiza, toda a lógica de estado vive em usePainelOperadorForm. */
export function RecebivelForm({ form, onSubmit, isSubmitting, cedentes, tiposRecebivel, moedas }: RecebivelFormProps) {
  const { register, formState } = form
  const { errors } = formState

  const amanha = new Date(Date.now() + 86_400_000).toISOString().slice(0, 10)

  return (
    <form onSubmit={onSubmit} className="flex flex-col gap-4">
      <Select label="Cedente" error={errors.cedenteId?.message} {...register('cedenteId')}>
        <option value="">Selecione...</option>
        {cedentes.map((cedente) => (
          <option key={cedente.id} value={cedente.id}>
            {cedente.nome}
          </option>
        ))}
      </Select>

      <Select label="Tipo de recebível" error={errors.tipoRecebivelCodigo?.message} {...register('tipoRecebivelCodigo')}>
        <option value="">Selecione...</option>
        {tiposRecebivel.map((tipo) => (
          <option key={tipo.codigo} value={tipo.codigo}>
            {tipo.nome}
          </option>
        ))}
      </Select>

      <Input
        label="Valor de face"
        type="number"
        step="0.01"
        min="0.01"
        placeholder="0,00"
        error={errors.valorFace?.message}
        {...register('valorFace')}
      />

      <DateField label="Vencimento" min={amanha} error={errors.dataVencimento?.message} {...register('dataVencimento')} />

      <div className="grid grid-cols-2 gap-4">
        <Select label="Moeda do título" error={errors.moedaTitulo?.message} {...register('moedaTitulo')}>
          {moedas.map((moeda) => (
            <option key={moeda.codigo} value={moeda.codigo}>
              {moeda.codigo}
            </option>
          ))}
        </Select>

        <Select label="Moeda de pagamento" error={errors.moedaPagamento?.message} {...register('moedaPagamento')}>
          {moedas.map((moeda) => (
            <option key={moeda.codigo} value={moeda.codigo}>
              {moeda.codigo}
            </option>
          ))}
        </Select>
      </div>

      <Button type="submit" disabled={isSubmitting} className="mt-2">
        {isSubmitting ? 'Enviando...' : 'Liquidar recebível'}
      </Button>
    </form>
  )
}
