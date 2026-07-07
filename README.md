# SRM Credit Engine

[![CI](https://github.com/Narvaal/SRMCreditEngine/actions/workflows/ci.yml/badge.svg)](https://github.com/Narvaal/SRMCreditEngine/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/Narvaal/SRMCreditEngine)](https://github.com/Narvaal/SRMCreditEngine/releases/latest)

Uma empresa que tem dinheiro a receber (duplicatas, cheques pré-datados) pode vender esses títulos pra um fundo e receber à vista, com um desconto — o **deságio**. Este sistema faz esse negócio funcionar: calcula o deságio de cada título conforme o risco e o prazo, liquida o pagamento em **BRL ou USD**, e registra tudo de forma auditável — nada é editado ou apagado, correções viram estornos.

Construído para o desafio técnico da SRM Asset, no nível **Sênior**. Todos os requisitos do enunciado (Júnior + Pleno + Sênior) foram entregues, além dos nice-to-haves — o detalhe de cada entrega está no [`ROADMAP.md`](./ROADMAP.md).

## Demonstração em 5 minutos

Pré-requisito: Docker + Docker Compose.

```bash
docker compose up -d --build
```

Sobe 5 containers (app, banco e observabilidade), com migrations e dashboards aplicados automaticamente:

| Serviço | URL | Acesso |
|---|---|---|
| **Frontend** (o produto) | http://localhost:8081 | — |
| API + Swagger | http://localhost:8080/swagger-ui/index.html | — |
| Grafana (dashboard pronto) | http://localhost:3000 | `admin` / `admin` |
| Prometheus | http://localhost:9090 | — |

Roteiro sugerido:

1. **Painel do Operador** (frontend): preencha um recebível e veja o valor líquido calculado **em tempo real**, antes de liquidar. Dá pra cadastrar um cedente novo sem sair da tela.
2. Clique em **Registrar** e veja a liquidação acontecer.
3. **Grid de Transações**: a operação aparece no histórico (filtros e paginação direto na URL). Clique em **Estornar** — confirmação em modal, resultado em toast, e a linha do estorno passa a mostrar a operação de origem ("Ver origem").
4. Abra o **dashboard do Grafana**: negócio (liquidações, estornos, valor por moeda), API, circuit breaker e runtime — tudo já provisionado.
5. Com o dashboard aberto, rode `./scripts/dashboard/cenario1.sh` e veja o **circuit breaker** abrir e se recuperar ao vivo. Há [5 cenários prontos](./scripts/dashboard/README.md) (erros, carga, tráfego de negócio, um pico por rota).

Para derrubar tudo: `docker compose down -v`.

## O que foi entregue, por nível do desafio

| Nível | Entregas |
|---|---|
| 🟢 **Júnior** | API + frontend funcionais; cálculo de deságio com precisão decimal (`BigDecimal` de ponta a ponta); banco normalizado com [diagrama ER e DDL](./docs/diagrama-er.md); este README |
| 🟡 **Pleno** | Docker Compose com stack completa; [Strategy Pattern](./backend/src/main/java/com/srmasset/creditengine/pricing) na precificação por tipo de recebível; exception handler global (erros sempre no mesmo envelope JSON); validação de entrada nas duas pontas (Bean Validation + Zod); testes unitários das regras de precificação |
| 🔴 **Sênior** | [Diagrama C4](./docs/diagrama-c4.md); logs estruturados (JSON/ECS) com correlation id por requisição; métricas + [dashboard do Grafana provisionado como código](./infra/grafana/provisioning/dashboards/README.md); CI/CD (lint, testes e smoke test da stack completa); resiliência com retry + circuit breaker na integração externa; Optimistic Locking contra liquidações concorrentes; Git hooks, tags semânticas e rebase interativo |

**Além do pedido**: grid com visão de estado final (estorno expande a operação de origem), métricas de negócio no dashboard, [cenários de demonstração](./scripts/dashboard/README.md), [teste de carga real](./scripts/carga/README.md) (1M de linhas — a primeira execução achou e corrigiu 2 problemas reais de performance), [critérios de aceite formais](./docs/criterios-aceite.md) com gaps honestos, e dois itens do nível Especialista — estratégia de branching justificada e [simulação de gestão de crise](./ROADMAP.md) com hotfix via `cherry-pick` (`v1.0.1`).

## Mapa pra avaliação

Onde encontrar a evidência de cada critério do enunciado (seção 8 do [`CLAUDE.md`](./CLAUDE.md)):

| Critério | Onde está |
|---|---|
| Fundamentação teórica da stack | Seção "Stack" logo abaixo |
| Design de código (SOLID, DRY, KISS) | 3 camadas + relatório em 2 camadas ([C4](./docs/diagrama-c4.md)); Strategy em [`pricing/`](./backend/src/main/java/com/srmasset/creditengine/pricing); 133 testes de backend (≈88% de linhas, JaCoCo no CI) + 121 de frontend |
| Domínio do Git | Seção "Git na prática" + [histórico de releases](https://github.com/Narvaal/SRMCreditEngine/releases) e PRs |
| Domínio do negócio | [Diagrama ER](./docs/diagrama-er.md) (precisão por coluna, ledger append-only); Optimistic Locking testado com concorrência real |
| Uso da IA | [`AI_USAGE.md`](./AI_USAGE.md) — prompts, erros da IA e análise crítica, mantido a cada entrega |
| System Design (Sênior+) | [Critérios de aceite](./docs/criterios-aceite.md) com status e evidência por item; observabilidade abaixo |

## Stack

| Camada | Escolha | Por quê (resumo) |
|---|---|---|
| Backend | Java 21 + Spring Boot 3.5 | Tipagem forte e o ecossistema mais maduro pra domínio financeiro (ACID, `BigDecimal`, locking) |
| Banco | PostgreSQL + Flyway | `NUMERIC` com precisão exata, constraints ricas; schema versionado como código |
| Frontend | React 19 + TypeScript + Vite | Tipos espelhando os DTOs da API pegam quebra de contrato em compile-time |
| Estado no front | TanStack Query + RHF + Zod (sem Redux) | Três fontes de verdade bem delimitadas dispensam store global |
| Observabilidade | Actuator → Prometheus → Grafana | Métricas sem código extra; dashboard versionado no repo |
| Resiliência | Resilience4j | Retry + circuit breaker na única chamada externa (provider de câmbio) |

<details>
<summary><b>Fundamentação completa (por que cada escolha, amarrada ao problema)</b></summary>

Cada escolha amarrada ao problema — um motor financeiro multimoedas, onde precisão decimal, atomicidade e concorrência são domínio, não detalhe:

- **Java 21 + Spring Boot 3.5**: o desafio pede tipagem forte e frameworks maduros pra ambiente financeiro — Java entrega os dois com o ecossistema mais testado do mercado bancário. Os quatro pilares do domínio são recursos de primeira classe da plataforma, não bibliotecas coladas: `BigDecimal` pra aritmética decimal exata, `@Transactional` com semântica ACID real, Bean Validation na borda da API, e Optimistic Locking (`@Version`) de série no JPA. Spring Boot 3.5 ainda traz logging estruturado nativo e Micrometer/Prometheus sem código extra.
- **`ch.obermuhlner:big-math`**: a fórmula de valor presente tem expoente fracionário (prazo em meses) e o `BigDecimal.pow()` nativo só aceita expoente inteiro — cair pra `double` quebraria a precisão que é requisito. big-math resolve potência fracionária inteiramente em `BigDecimal`; a dependência fica isolada num único ponto (`MotorPrecificacao`).
- **PostgreSQL + Flyway**: banco relacional era pedido explícito; Postgres soma `NUMERIC` com precisão/escala exatas por coluna (documentadas no diagrama ER), constraints ricas (CHECK, UNIQUE compostas) que fazem o banco defender invariantes de negócio, e MVCC que casa com o Optimistic Locking da aplicação. Flyway versiona o schema como código — as migrations são a fonte de verdade do DDL.
- **Gradle (Kotlin DSL)**: build script tipado e conciso; wrapper commitado garante build reprodutível no CI.
- **React 19 + TypeScript + Vite**: TypeScript espelha os DTOs do backend em tipos (`frontend/src/api/types.ts`), pegando drift de contrato em compile-time; React pela maturidade/ecossistema; Vite por dev-server com HMR instantâneo e build enxuto.
- **TanStack Query + React Hook Form + Zod, sem Redux**: a resposta ao "gerenciamento de estado global, se necessário" do enunciado foi *não é necessário* — três fontes de verdade bem delimitadas (dados de servidor no React Query, formulário no RHF, filtros/paginação na URL) eliminam o store global. Zod valida na borda do frontend com o mesmo espírito do Bean Validation no backend.
- **Monorepo**: 1 repositório público (como pede a entrega), 1 pipeline de CI orquestrando os dois lados, e histórico correlacionando mudanças de API com mudanças de UI.

</details>

```
/backend    → API, regras de negócio e persistência (Java / Spring / Gradle)
/frontend   → Painel do Operador e Grid de Transações (TypeScript / React / Vite)
/docs       → Diagrama ER, diagrama C4 e critérios de aceite
/infra      → Prometheus e Grafana (datasource e dashboard provisionados)
/scripts    → Cenários de demonstração do dashboard e teste de carga (k6)
```

## Rodando fora do Docker (desenvolvimento)

<details>
<summary><b>Backend isolado (Gradle)</b></summary>

Pré-requisitos: Java 21 e um PostgreSQL acessível.

```bash
cd backend

# variáveis de ambiente esperadas (valores default entre parênteses)
export DB_HOST=localhost        # (localhost)
export DB_PORT=5432             # (5432)
export DB_NAME=srm_credit_engine
export DB_USER=srm
export DB_PASSWORD=srm

./gradlew build     # compila e roda os testes
./gradlew bootRun   # sobe em http://localhost:8080, aplicando as migrations
```

> `./gradlew test` inclui 3 testes de integração (`*IT`: concorrência com Optimistic Locking, relatório com SQL nativo e resiliência do circuit breaker) que sobem um Postgres descartável via Testcontainers. Em ambientes com Docker Engine muito recente, o probe de compatibilidade do Testcontainers 1.21.x pode falhar na inicialização (não é um bug dos testes — eles rodam no CI). Por isso o hook de pre-push roda só os testes de unidade/slice (`--tests '*Test'`), deixando os `*IT` para o CI.

</details>

<details>
<summary><b>Frontend isolado (Vite, hot reload)</b></summary>

Pré-requisito: Node.js 22+ e o backend no ar (`docker compose up -d` na raiz, ou `./gradlew bootRun`) — o Vite tem proxy de dev para `/api` → `localhost:8080`.

```bash
cd frontend
npm install
npm run dev   # http://localhost:5173
```

</details>

Os contratos da API estão todos no Swagger (`/swagger-ui/index.html`), com um snapshot OpenAPI versionado em [`docs/openapi.json`](./docs/openapi.json) — importável direto em clientes REST como Bruno, Postman ou Insomnia; o DDL do banco está versionado em `backend/src/main/resources/db/migration/`.

## Observabilidade e resiliência

- **Logs estruturados** (JSON, formato ECS) com `requestId` correlacionando todas as linhas de uma mesma requisição: `docker compose logs -f backend`.
- **Dashboard do Grafana** provisionado como código, em 4 linhas: negócio (liquidações/estornos e valor por moeda — métricas instrumentadas no domínio), API (RPS, latência p95, erros), resiliência (estado do circuit breaker ao vivo) e runtime (JVM, pool de conexões).
- **Resiliência**: a integração com o provider de câmbio (simulado) tem retry com backoff + circuit breaker; se o provider cai, só a atualização de taxas degrada — liquidação e simulação seguem com a última taxa vigente.
- Pra ver tudo isso reagindo, use os [cenários de demonstração](./scripts/dashboard/README.md) — do circuit breaker abrindo a um pico de requisições por rota.
- O CI valida a stack completa a cada push, incluindo Prometheus, Grafana e o dashboard provisionado.

## Git na prática

Trunk-based sobre `dev` com **Conventional Commits** atômicos; cada entrega coesa vira **PR pra `main`** (merge commit) com **tag semântica e Release** ([v1.0.0 → v1.4.0](https://github.com/Narvaal/SRMCreditEngine/releases)). Hooks de pre-commit (lint), commit-msg (convenção) e pre-push (testes); histórico organizado com **rebase interativo** antes dos merges. A branch `prod` existe só pra demonstrar o exercício de gestão de crise do enunciado — um hotfix real levado de `main` pra `prod` via `git cherry-pick` (`v1.0.1`, detalhes no [`ROADMAP.md`](./ROADMAP.md), Passo 12).

## IA como copiloto

O projeto foi construído com Claude Code como copiloto, seguindo a política do enunciado: as decisões e a responsabilidade são do autor. O [`AI_USAGE.md`](./AI_USAGE.md) registra, entrega por entrega, os prompts estratégicos, os casos em que a IA errou (e como foi corrigido) e a análise crítica de onde ela ajudou ou atrapalhou.

## Documentação

- [`ROADMAP.md`](./ROADMAP.md) — a história completa: decisões de domínio e cada passo técnico (1–17).
- [`docs/diagrama-er.md`](./docs/diagrama-er.md) — modelo de dados, precisão numérica e gaps conhecidos.
- [`docs/diagrama-c4.md`](./docs/diagrama-c4.md) — arquitetura (Contexto e Container).
- [`docs/criterios-aceite.md`](./docs/criterios-aceite.md) — usabilidade, segurança, desempenho e escalabilidade, item a item.
- [`docs/openapi.json`](./docs/openapi.json) — contrato OpenAPI da API (exportado de `/v3/api-docs`), pronto pra importar no Bruno/Postman/Insomnia.
- [`AI_USAGE.md`](./AI_USAGE.md) — uso de IA no desenvolvimento.
- [`CLAUDE.md`](./CLAUDE.md) — o enunciado completo do desafio.
