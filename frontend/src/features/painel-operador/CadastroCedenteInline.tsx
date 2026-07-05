import { useState } from 'react'
import { ApiError } from '../../api/httpClient'
import { Alert, Button, Input } from '../../components/ui'
import { useCadastroCedente } from '../../domain/useCadastroCedente'
import {
  documentoSomenteDigitos,
  mascararDocumento,
  mensagemErroDocumento,
  validarDocumento,
  validarRazaoSocial,
} from '../../lib/documento'

interface CadastroCedenteInlineProps {
  /** Chamado com o id do cedente recém-criado — o Painel usa pra auto-selecionar no formulário. */
  onCriado: (cedenteId: string) => void
}

interface ErrosCampos {
  nome?: string
  documento?: string
}

/**
 * Cadastro rápido de cedente sem sair do Painel — colapsado por padrão, expande dois campos.
 * Componente de feature (composição): pode usar domain/, diferente dos componentes de ui/.
 */
export function CadastroCedenteInline({ onCriado }: CadastroCedenteInlineProps) {
  const [aberto, setAberto] = useState(false)
  const [nome, setNome] = useState('')
  const [documento, setDocumento] = useState('')
  const [errosValidacao, setErrosValidacao] = useState<ErrosCampos>({})

  const cadastro = useCadastroCedente()
  const erroApi = cadastro.error instanceof ApiError ? cadastro.error : null

  // Erros de bean-validation do backend chegam por campo em camposInvalidos — exibimos
  // inline igual aos erros de validação local; o Alert fica só pra erros gerais (ex.: 409).
  const errosApiPorCampo = new Map((erroApi?.camposInvalidos ?? []).map((c) => [c.campo, c.mensagem]))
  const erroNome = errosValidacao.nome ?? errosApiPorCampo.get('nome')
  const erroDocumento = errosValidacao.documento ?? errosApiPorCampo.get('documento')
  const erroGeral = erroApi && errosApiPorCampo.size === 0 ? erroApi.message || 'Não foi possível cadastrar o cedente.' : null

  function cadastrar() {
    const erros: ErrosCampos = {}
    const erroRazaoSocial = validarRazaoSocial(nome)
    if (erroRazaoSocial) erros.nome = erroRazaoSocial
    if (!validarDocumento(documento)) erros.documento = mensagemErroDocumento(documento)

    setErrosValidacao(erros)
    if (erros.nome || erros.documento) return

    cadastro.mutate(
      { nome: nome.trim(), documento: documentoSomenteDigitos(documento) },
      {
        onSuccess: (novo) => {
          setAberto(false)
          setNome('')
          setDocumento('')
          onCriado(novo.id)
        },
      },
    )
  }

  if (!aberto) {
    return (
      <Button
        type="button"
        variante="ghost"
        className="self-start px-1 py-0 text-xs"
        onClick={() => setAberto(true)}
      >
        + Cadastrar novo cedente
      </Button>
    )
  }

  return (
    <div className="flex flex-col gap-3 rounded-md border border-border p-3">
      <span className="text-xs tracking-wide text-ink-muted uppercase">Novo cedente</span>
      <Input
        label="Nome"
        name="novoCedenteNome"
        value={nome}
        onChange={(e) => setNome(e.target.value)}
        placeholder="Razão social"
        maxLength={20}
        error={erroNome}
      />
      <Input
        label="Documento"
        name="novoCedenteDocumento"
        value={documento}
        onChange={(e) => setDocumento(mascararDocumento(e.target.value))}
        placeholder="CPF/CNPJ"
        inputMode="numeric"
        error={erroDocumento}
      />

      {erroGeral && <Alert tipo="error">{erroGeral}</Alert>}

      <div className="flex gap-2">
        <Button type="button" className="px-3 py-1.5 text-xs" disabled={cadastro.isPending} onClick={cadastrar}>
          {cadastro.isPending ? 'Cadastrando...' : 'Cadastrar'}
        </Button>
        <Button type="button" variante="ghost" className="px-3 py-1.5 text-xs" onClick={() => setAberto(false)}>
          Cancelar
        </Button>
      </div>
    </div>
  )
}
