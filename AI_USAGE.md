# AI_USAGE.md

Registro do uso de IA (Claude Code) no desenvolvimento do SRM Credit Engine, conforme a política de uso de IA do desafio (ver `CLAUDE.md`, seção 2). Este documento é atualizado incrementalmente ao longo do desenvolvimento, não apenas ao final.

## Ferramenta utilizada

- **Claude Code** (Anthropic), modelo Sonnet 5 — sessão via CLI.

## Prompts estratégicos utilizados

| Data | Prompt (resumo) | Objetivo | Resultado |
|------|------------------|----------|-----------|
| 2026-07-03 | "escreva um claude md com essas informações [enunciado completo do desafio]" | Registrar o enunciado do desafio como contexto persistente do projeto, para orientar sessões futuras de desenvolvimento assistido por IA | `CLAUDE.md` criado com o enunciado completo estruturado por seção, mais notas práticas de orientação (stack, precisão decimal, workflow de Git por senioridade) |
| 2026-07-03 | "crie tmb o AI_USAGE.md" | Criar o arquivo de rastreabilidade de uso de IA exigido pelo desafio | Este arquivo |
| 2026-07-03 | Sequência de perguntas pedindo pra explicar o problema de negócio "da forma mais simples possível" (fluxo de deságio, ordem da conversão de moeda, o que é um lote de recebíveis) | Construir entendimento do domínio antes de tomar qualquer decisão técnica (metodologia SDD) | Explicações em linguagem simples com exemplos numéricos concretos, sem gerar código — usadas como base para as decisões registradas em seguida |
| 2026-07-03 | Fechamento guiado das perguntas em aberto do `ROADMAP.md` (granularidade da liquidação por lote vs. por item; origem da Taxa Base — mercado vs. interna; convenção de prazo em dias vs. meses; existência de estorno/cancelamento; controle de saldo de caixa por moeda) | Eliminar as ambiguidades de negócio do enunciado antes de especificar modelo de dados e API | 5 decisões de domínio registradas no `ROADMAP.md`, cada uma com a justificativa de negócio por trás: (1) liquidação independente por recebível, não por lote; (2) Taxa Base é taxa de mercado (CDI/SOFR) com histórico, spread é política interna da SRM; (3) prazo em meses, mês fixo = 30 dias, convenção simplificada e assumida conscientemente; (4) histórico append-only, correções via estorno, nunca reescrita do passado; (5) caixa com saldo controlado por moeda, habilitando o cenário real de Optimistic Locking do nível Sênior |
| 2026-07-03 | "faça um plano e me apresente" (modelo de dados) — rascunho inicial de 8 entidades revisado por um agente de planejamento dedicado, com instrução explícita para criticar tipos numéricos, índices, constraints e apontar entidades/relacionamentos faltantes sob a ótica de um sistema financeiro real | Validar o esquema de dados antes de implementar as migrations, evitando erros de precisão numérica ou modelagem que só apareceriam depois de código de aplicação já escrito em cima | O agente encontrou problemas reais no rascunho: `NUMERIC` sem precisão/escala definida (risco real de inconsistência num sistema financeiro), `recebivel.status` binário incompleto (não suportava o fluxo de estorno já decidido), e CHECK constraints de integridade ausentes (ex.: invariante moeda-título × taxa-de-câmbio). Também sugeriu 3 extensões de escopo (tabela `lote_importacao`, ponteiro redundante em `recebivel`, spread no banco vs. no código) — cada uma foi levada de volta pro usuário decidir via pergunta objetiva, não aceita automaticamente. Onde a IA ajudou: achou a lacuna de `NUMERIC` genérico e o `status` binário incompleto, que eu não tinha notado no rascunho. Onde exigiu filtro humano: sugeriu adicionar `movimento_caixa` e `liquidacao.criado_por` "de graça" — decidi adiar as duas por escopo (a segunda nem faz sentido ainda, não existe sistema de usuário) em vez de aceitar por padrão só porque a IA recomendou. |

| 2026-07-03 | "o docker compose já tá pronto, se não tiver faça ele subir o app e banco de dados" | Orquestrar API + PostgreSQL + Prometheus + Grafana num único `docker compose up`, item já pendente no checklist do README | `docker-compose.yml` + `backend/Dockerfile` (build multi-stage) + `infra/prometheus/prometheus.yml` + provisionamento automático do datasource do Grafana. Validado de verdade: build da imagem, subida dos 4 containers, migrations aplicando no boot, `/actuator/health` respondendo, Prometheus com o target `up`, Grafana com o datasource já configurado — e um ciclo completo `down -v` + `up -d` do zero pra garantir que não era só cache local funcionando por acaso |

> Novas entradas devem ser adicionadas à tabela conforme prompts relevantes forem usados (scaffolding, geração de massa de dados, refatoração de queries, geração de testes, etc.).

## Trechos onde a IA alucinou ou gerou código inseguro

_Nenhum código de aplicação foi gerado até o momento — o repositório contém apenas documentação inicial. Esta seção será preenchida assim que houver implementação, com trecho, causa da falha e correção aplicada._

## Análise crítica

**Onde a IA economizou tempo:**
- Scaffolding completo de backend (Gradle/Spring Boot) e frontend (Vite/React/TS), incluindo escolha e resolução de versões atuais de todas as dependências via consulta direta ao Maven Central/npm registry — trabalho mecânico que seria repetitivo fazer manualmente.
- Configuração de Husky/lint-staged/commitlint (3 hooks) escrita, testada e commitada de ponta a ponta numa única sessão, incluindo a validação real de cada hook (não só "deveria funcionar").
- Revisão do rascunho de modelo de dados por um agente dedicado encontrou lacunas reais de precisão numérica (`NUMERIC` sem escala) e de modelagem (`status` binário incompleto) antes de qualquer linha de DDL ser escrita — mais barato corrigir no papel do que depois de entidades JPA já existirem em cima.

**Onde a IA exigiu filtro humano (não foi aceito por padrão):**
- O agente de planejamento do modelo de dados sugeriu 3 extensões de escopo "de graça" (tabela `movimento_caixa`, coluna `liquidacao.criado_por`, ponteiro redundante `recebivel.ultima_liquidacao_id`) que, se aceitas sem questionar, teriam inflado o modelo com complexidade não pedida pelo desafio — duas foram adiadas explicitamente como gap documentado, uma foi decidida via pergunta objetiva ao usuário em vez de assumida.
- Ao gerar o `.gitignore` inicial, esqueci de cobrir `node_modules/` na raiz do monorepo (só cobri `frontend/node_modules/`) — isso quase resultou em commitar ~100 pacotes de `node_modules` do Husky/commitlint no histórico do Git. Percebido antes do commit (`git status --short` mostrando centenas de arquivos inesperados) e corrigido no `.gitignore` antes de qualquer commit sujar o repositório.

**Decisões deliberadamente humanas, não apenas aceitas de sugestão da IA:**
- Toda decisão de domínio (Passo 1 do `ROADMAP.md`) foi conduzida por perguntas do usuário pedindo explicação simples antes de decidir — a IA nunca decidiu regra de negócio sozinha, só explicou trade-offs para decisão humana.
- Escolha de stack (Gradle vs. Maven, monorepo vs. multi-repo, PostgreSQL vs. MySQL) e escopo do modelo de dados (spread no código vs. no banco, persistir lote ou não) foram sempre confirmados via pergunta explícita ao usuário antes de implementar, nunca assumidos silenciosamente.
