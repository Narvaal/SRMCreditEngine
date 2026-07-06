# SRM Credit Engine — Desafio Técnico

Plataforma de Cessão de Crédito Multimoedas. Este arquivo documenta o enunciado completo do desafio técnico para orientar o desenvolvimento neste repositório.

## 1. Contexto Empresarial

A **SRM Asset** é uma referência em fundos de investimento, especialmente em FIDCs (Fundos de Investimento em Direitos Creditórios). A operação envolve a aquisição de ativos (duplicatas, contratos, recebíveis) de empresas cedentes, provendo liquidez ao mercado.

Com a globalização do portfólio, o fundo passou a operar com caixa multimoedas (BRL e USD). O time de mesas de operação necessita de um sistema robusto, o **SRM Credit Engine**, para precificar e liquidar esses ativos com segurança e precisão decimal.

**Problema de negócio:** receber um lote de recebíveis, calcular o "deságio" (desconto) baseado no risco do ativo e na moeda de pagamento, e registrar a transação de forma auditável.

## 2. Política de Uso de IA (AI as a Co-Pilot)

O uso de LLMs (ChatGPT, Claude, Gemini, Copilot) é **permitido e encorajado**, sob as seguintes diretrizes:

1. **Autoria intelectual:** domínio de 100% do código entregue. "Foi o Copilot que gerou" não é defesa aceitável para falhas de segurança ou lógica.
2. **Documentação de uso:** incluir `AI_USAGE.md` no repositório descrevendo:
   - Prompts estratégicos utilizados (ex.: geração de massa de dados, refatoração de queries, scaffolding).
   - Trechos onde a IA alucinou ou gerou código inseguro e como foi corrigido.
   - Análise crítica: onde a IA economizou tempo e onde atrapalhou.

> Manter o `AI_USAGE.md` atualizado ao longo do desenvolvimento, não só no final.

## 3. Escopo Técnico — Backend (stack livre, agnóstica)

Escolha de stack livre, desde que adequada a ambiente financeiro (tipagem forte e frameworks maduros são diferenciais).

### Requisitos funcionais

1. **Gestão de Câmbio (Currency Engine)**
   - Armazenar e prover taxas de câmbio (ex.: USD → BRL).
   - Endpoint para atualização manual ou integração (mockada) de taxas.

2. **Motor de Precificação (Strategy Pattern)**
   - Cada tipo de recebível tem uma regra de risco (spread) diferente. Aplicar o padrão **Strategy** para desacoplar a regra do cálculo.
   - Fórmula base: `Valor Presente = Valor Face / (1 + Taxa Base + Spread)^Prazo`
   - Variações de risco (exemplo):
     - Duplicata Mercantil: spread de 1,5% a.m.
     - Cheque Pré-datado: spread de 2,5% a.m.
   - Operação cross-currency (título em BRL, pagamento em USD): aplicar conversão cambial no final.

3. **Persistência e Integridade**
   - Banco de dados relacional (preferencial).
   - Transações financeiras devem respeitar propriedades **ACID**. Nenhuma liquidação pode ficar "pela metade" — atenção a *race conditions*.

4. **API RESTful (API First)**
   - Design claro, verbos HTTP corretos, códigos de status semânticos.
   - Documentação via OpenAPI/Swagger.

5. **Consultas Analíticas**
   - Rota de "Extrato de Liquidação" com filtro por período, cedente e tipo de moeda em grandes volumes.
   - Diferencial: Query Builders ou SQL nativo otimizado para relatórios em vez de ORM puro.

6. **Arquitetura em camadas**
   - Separação de aplicação, negócio e persistência em 3 camadas.
   - Relatórios podem usar só 2 camadas, sem passar pela de negócio.

## 4. Escopo Técnico — Frontend (framework SPA livre)

React, Vue, Angular, Svelte etc.

1. **Painel do Operador**
   - Input dos dados do recebível (Valor, Vencimento, Tipo).
   - Exibição em tempo real do cálculo do valor líquido (simulação).

2. **Grid de Transações**
   - Tabela de histórico com paginação server-side.
   - Filtros dinâmicos.

3. **Arquitetura de Front**
   - Separação clara entre UI (apresentação) e lógica de negócio/estado.
   - Gerenciamento de estado global, se necessário.

## 5. Requisitos Não Funcionais

1. **Tratamento de exceções:** resiliência, erros inesperados tratados de forma controlada, sem interrupção abrupta do fluxo.
2. **Critérios de aceite:** definidos cobrindo usabilidade, segurança, desempenho e escalabilidade.

## 6. System Design, Git Workflow & Expectativas por Senioridade

A complexidade da entrega escala com o nível da vaga. O uso do Git é avaliado como reflexo de organização e capacidade de trabalho em times de alta performance. Cada nível é **acumulativo** em relação ao anterior.

### 🟢 Júnior
- **Foco:** código limpo, funcional, organizado.
- **Git:**
  - Commits atômicos (nada de "finalizado"; ex.: "cria tabela cliente", "adiciona validação cpf").
  - Branching básico — nunca direto na `main`/`master` (ex.: `feature/calculo-desagio`).
- **Entregáveis:**
  - API e frontend rodando localmente.
  - Lógica de cálculo correta.
  - Banco de dados normalizado (diagrama ER básico).
  - Instruções de "Como rodar" claras no README.

### 🟡 Pleno
- **Foco:** padrões de projeto, robustez, fluxo de trabalho.
- **Git:**
  - Conventional Commits obrigatório (`feat: add currency strategy`, `fix: calculation rounding`, `docs: update readme`).
  - Pull Requests mesmo trabalhando sozinho, com descrição do que foi feito.
  - Histórico limpo, sem merges desnecessários ou poluídos.
- **Entregáveis:**
  - Docker e Docker Compose orquestrando app + banco.
  - Tratamento de erros global (Exception Handlers).
  - Validações de input robustas (segurança).
  - Testes unitários cobrindo as regras de precificação (Strategy).

### 🔴 Sênior
- **Foco:** observabilidade, escalabilidade, automação.
- **Git:**
  - Git Hooks (Husky, pre-commit) rodando linters/testes antes de commit/push.
  - Semantic Versioning via Tags (ex.: `v1.0.0`) para a entrega final.
  - Interactive rebase para organizar commits antes do merge (squash de fixes, reordenação lógica).
- **Entregáveis:**
  - Diagrama C4 (Nível 1 e 2 — Contexto e Container).
  - Observabilidade: logs estruturados, métricas (Prometheus/Grafana) ou tracing.
  - CI/CD: pipeline (GitHub Actions ou similar) rodando testes e linter.
  - Resiliência: retries ou circuit breaker em chamadas externas.
  - Concorrência: Optimistic Locking para evitar conflito de liquidação.

### 🟣 Especialista / Staff / Principal
- **Foco:** arquitetura distribuída, governança, gestão de crise.
- **Git:**
  - Estratégia de branching justificada no README (Git Flow, Trunk Based, GitHub Flow).
  - Simulação de gestão de crise: bug crítico foi para a `main` → demonstrar `git revert` seguro, ou `git cherry-pick` simulando hotfix em produção.
- **Entregáveis:**
  - ADR (Architecture Decision Records) para decisões difíceis (ex.: SQL vs NoSQL, monolito vs microserviços).
  - Design de alta escala no README: arquitetura para **1 milhão de transações/minuto** (caching, sharding, consistência eventual).
  - IaC opcional (Terraform ou manifests Kubernetes).
  - Proposta de arquitetura EDA (modelagem de eventos).

## 7. Modelagem de Dados e Scripts

Independente da ferramenta de migração (Flyway, Liquibase etc.) ou ORM, fornecer no README ou em `/docs`:

1. **Diagrama ER:** relacionamentos entre Moedas, Produtos (tipos de recebíveis), Transações e Taxas.
2. **Scripts DDL:** SQL necessário para criar a estrutura do banco.

## 8. Critérios de Avaliação

1. Fundamentação teórica: justificar escolha de linguagem e bibliotecas.
2. Design de código: aderência a SOLID, DRY, KISS.
3. Domínio do Git: histórico rastreável, controle sobre versionamento.
4. Domínio do negócio: modelagem de dados refletindo o problema financeiro (precisão numérica, segurança transacional).
5. Uso da IA: potencializou a engenharia ou mascarou falta de conhecimento?
6. Maturidade de System Design (Sênior+): arquitetura aguenta produção, é segura e observável.

## 9. Entrega

1. Repositório público (GitHub/GitLab).
2. Prazo: 3 a 4 dias úteis (ajustável conforme complexidade entregue).
3. README capricha na documentação de setup, design e decisões.

---

## Notas para o Claude Code neste repositório

- Nível-alvo: **Sênior**. Todos os requisitos explícitos (Júnior+Pleno+Sênior) estão fechados e auditados — ver `ROADMAP.md` (Passos 1–16) pro histórico e a lista do que resta (nice-to-have).
- Stack definida: Java 21 + Spring Boot 3.5 (Gradle/Kotlin DSL) + PostgreSQL/Flyway no backend; React 19 + TypeScript + Vite no frontend. Fundamentação no `README.md`.
- Precisão decimal é regra de domínio: nunca `float`/`double` pra dinheiro — `BigDecimal` em toda a cadeia (potência fracionária via `big-math`, isolada em `MotorPrecificacao`); escalas/arredondamento centralizados em `pricing/Precisao.java`.
- Manter `AI_USAGE.md` atualizado conforme o trabalho avança (seção 2), não só ao final.
- Fluxo Git real: trunk-based sobre `dev` (Conventional Commits atômicos) → PR pra `main` (merge commit, nunca squash) → tag semântica + GitHub Release. `prod` existe só pro exercício de gestão de crise — ver seção "Estratégia de branching" do `README.md`. Nenhum commit direto em `main`.
- Os 3 testes de integração (`*IT`, Testcontainers) falham na inicialização neste ambiente local (Docker Engine incompatível com o probe do Testcontainers 1.21.x) mas passam no CI — o pre-push roda só `--tests '*Test'` por isso. Não tratar essas falhas locais como regressão.
