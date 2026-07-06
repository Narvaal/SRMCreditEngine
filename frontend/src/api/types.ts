// Espelham os DTOs do backend (com.srmasset.creditengine.dto.*) — ver Swagger em /swagger-ui/index.html.

export interface Moeda {
  codigo: string
  nome: string
  casasDecimais: number
}

export interface TipoRecebivel {
  codigo: string
  nome: string
}

export interface Cedente {
  id: string
  nome: string
  documento: string
}

export interface SimulacaoRecebivelRequest {
  tipoRecebivelCodigo: string
  valorFace: number
  moedaTitulo: string
  dataVencimento: string // yyyy-MM-dd
  moedaPagamento: string
}

export interface SimulacaoRecebivelResponse {
  valorFace: number
  moedaTitulo: string
  taxaBaseUsada: number
  spreadUsado: number
  prazoMesesUsado: number
  valorPresente: number
  moedaPagamento: string
  taxaCambioUsada: number | null
  valorLiquido: number
}

export interface RecebivelRequest {
  cedenteId: string
  tipoRecebivelCodigo: string
  valorFace: number
  moedaTitulo: string
  dataVencimento: string
  moedaPagamento: string
}

export interface LiquidacaoResponse {
  id: string
  recebivelId: string
  cedenteId: string
  tipo: 'LIQUIDACAO' | 'ESTORNO'
  liquidacaoEstornadaId: string | null
  valorFace: number
  moedaTitulo: string
  taxaBaseUsada: number
  spreadUsado: number
  prazoMesesUsado: number
  valorPresente: number
  moedaPagamento: string
  taxaCambioUsada: number | null
  valorLiquido: number
  criadoEm: string
}

export interface LiquidacaoItemResultado {
  sucesso: boolean
  recebivelId: string | null
  liquidacao: LiquidacaoResponse | null
  codigoErro: string | null
  mensagemErro: string | null
}

export interface LoteLiquidacaoResponse {
  totalItens: number
  totalSucesso: number
  totalFalha: number
  itens: LiquidacaoItemResultado[]
}

export interface ExtratoLiquidacaoLinha {
  id: string
  recebivelId: string
  cedenteId: string
  cedenteNome: string
  tipo: 'LIQUIDACAO' | 'ESTORNO'
  moedaTitulo: string
  moedaPagamento: string
  valorFace: number
  valorLiquido: number
  criadoEm: string
  /**
   * Preenchidos só em linhas ESTORNO: referência da liquidação desfeita. Liquidações já
   * estornadas não vêm no extrato — a linha do estorno representa a operação.
   */
  liquidacaoEstornadaId: string | null
  liquidacaoEstornadaCriadoEm: string | null
}

export interface PaginaResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ExtratoLiquidacaoFiltro {
  cedenteId?: string
  moeda?: string
  tipo?: string
  dataInicio?: string
  dataFim?: string
  page: number
  size: number
}

export interface ErroResponse {
  timestamp: string
  status: number
  codigo: string
  mensagem: string
  path: string
  camposInvalidos: { campo: string; mensagem: string }[]
}
