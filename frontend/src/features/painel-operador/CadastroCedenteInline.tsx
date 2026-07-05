import { useState } from 'react'
import { ApiError } from '../../api/httpClient'
import { Alert, Button, Input } from '../../components/ui'
import { useCadastroCedente } from '../../domain/useCadastroCedente'

interface CadastroCedenteInlineProps {
  /** Chamado com o id do cedente recém-criado — o Painel usa pra auto-selecionar no formulário. */
  onCriado: (cedenteId: string) => void
}

/**
 * Cadastro rápido de cedente sem sair do Painel — colapsado por padrão, expande dois campos.
 * Componente de feature (composição): pode usar domain/, diferente dos componentes de ui/.
 */
export function CadastroCedenteInline({ onCriado }: CadastroCedenteInlineProps) {
  const [aberto, setAberto] = useState(false)
  const [nome, setNome] = useState('')
  const [documento, setDocumento] = useState('')
  const [erroValidacao, setErroValidacao] = useState<string | null>(null)

  const cadastro = useCadastroCedente()
  const erroApi = cadastro.error instanceof ApiError ? cadastro.error : null

  function cadastrar() {
    if (!nome.trim() || !documento.trim()) {
      setErroValidacao('Informe nome e documento do cedente.')
      return
    }
    setErroValidacao(null)
    cadastro.mutate(
      { nome: nome.trim(), documento: documento.trim() },
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
      />
      <Input
        label="Documento"
        name="novoCedenteDocumento"
        value={documento}
        onChange={(e) => setDocumento(e.target.value)}
        placeholder="CNPJ/CPF (só números)"
      />

      {erroValidacao && <Alert tipo="error">{erroValidacao}</Alert>}
      {erroApi && <Alert tipo="error">{erroApi.message || 'Não foi possível cadastrar o cedente.'}</Alert>}

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
