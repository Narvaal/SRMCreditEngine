import type { Cedente, Moeda } from '../../api/types'
import { DateField, Select } from '../../components/ui'
import type { FiltrosUrlState } from '../../domain/useExtratoFiltrosUrlState'

type FiltrosEditaveis = Omit<FiltrosUrlState, 'page' | 'size'>

interface FiltrosTransacoesProps extends FiltrosEditaveis {
  cedentes: Cedente[]
  moedas: Moeda[]
  onChange: (parcial: Partial<FiltrosEditaveis>) => void
}

/** Componente puro: recebe os valores atuais e um callback — não sabe que a URL existe. */
export function FiltrosTransacoes({ cedenteId, moeda, dataInicio, dataFim, cedentes, moedas, onChange }: FiltrosTransacoesProps) {
  return (
    <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
      <Select name="cedenteId" label="Cedente" value={cedenteId} onChange={(e) => onChange({ cedenteId: e.target.value })}>
        <option value="">Todos</option>
        {cedentes.map((cedente) => (
          <option key={cedente.id} value={cedente.id}>
            {cedente.nome}
          </option>
        ))}
      </Select>

      <Select name="moeda" label="Moeda" value={moeda} onChange={(e) => onChange({ moeda: e.target.value })}>
        <option value="">Todas</option>
        {moedas.map((m) => (
          <option key={m.codigo} value={m.codigo}>
            {m.codigo}
          </option>
        ))}
      </Select>

      <DateField name="dataInicio" label="De" value={dataInicio} onChange={(e) => onChange({ dataInicio: e.target.value })} />
      <DateField name="dataFim" label="Até" value={dataFim} onChange={(e) => onChange({ dataFim: e.target.value })} />
    </div>
  )
}
