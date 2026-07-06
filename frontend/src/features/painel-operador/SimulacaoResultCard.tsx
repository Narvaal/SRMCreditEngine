import type { ApiError } from '../../api/httpClient'
import type { SimulacaoRecebivelResponse } from '../../api/types'
import { Alert, Card, Skeleton } from '../../components/ui'
import { formatarMoeda, formatarPercentual } from '../../lib/formatters'

interface SimulacaoResultCardProps {
  dados: SimulacaoRecebivelResponse | undefined
  carregando: boolean
  erro: ApiError | null
  pronto: boolean
  /** Algum campo preenchido é inválido — o card avisa em vez de pedir pra "preencher". */
  comErro: boolean
}

/** Componente puro: só exibe o que `usePainelOperadorForm` calculou — nenhuma chamada de API aqui. */
export function SimulacaoResultCard({ dados, carregando, erro, pronto, comErro }: SimulacaoResultCardProps) {
  if (!pronto) {
    return (
      <Card className="flex min-h-48 items-center justify-center text-center text-sm text-ink-faint">
        {comErro
          ? 'Corrija os dados destacados no formulário para ver o valor líquido.'
          : 'Preencha os dados do recebível para ver o valor líquido calculado em tempo real.'}
      </Card>
    )
  }

  if (erro) {
    return (
      <Card>
        <Alert tipo="error">{erro.message || 'Não foi possível calcular a simulação.'}</Alert>
      </Card>
    )
  }

  if (carregando && !dados) {
    return (
      <Card className="flex flex-col gap-3">
        <Skeleton className="h-4 w-1/2" />
        <Skeleton className="h-4 w-2/3" />
        <Skeleton className="h-8 w-3/4" />
      </Card>
    )
  }

  if (!dados) return null

  return (
    <Card className={`flex flex-col gap-3 transition-opacity ${carregando ? 'opacity-60' : ''}`}>
      <span className="text-xs tracking-wide text-ink-muted uppercase">Simulação</span>

      <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
        <dt className="text-ink-muted">Taxa base</dt>
        <dd className="tabular-nums">{formatarPercentual(dados.taxaBaseUsada, 4)} a.m.</dd>

        <dt className="text-ink-muted">Spread</dt>
        <dd className="tabular-nums">{formatarPercentual(dados.spreadUsado, 4)} a.m.</dd>

        <dt className="text-ink-muted">Prazo</dt>
        <dd className="tabular-nums">{dados.prazoMesesUsado.toFixed(2)} meses</dd>

        <dt className="text-ink-muted">Valor presente</dt>
        <dd className="tabular-nums">{formatarMoeda(dados.valorPresente, dados.moedaTitulo)}</dd>

        {dados.taxaCambioUsada !== null && (
          <>
            <dt className="text-ink-muted">Taxa de câmbio</dt>
            <dd className="tabular-nums">{dados.taxaCambioUsada}</dd>
          </>
        )}
      </dl>

      <div className="mt-2 border-t border-border pt-3">
        <span className="text-xs text-ink-muted">Valor líquido a pagar</span>
        <p className="tabular-nums text-3xl font-semibold text-brand-400">
          {formatarMoeda(dados.valorLiquido, dados.moedaPagamento)}
        </p>
      </div>
    </Card>
  )
}
