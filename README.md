# SRM Credit Engine

[![CI](https://github.com/Narvaal/SRMCreditEngine/actions/workflows/ci.yml/badge.svg)](https://github.com/Narvaal/SRMCreditEngine/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/Narvaal/SRMCreditEngine)](https://github.com/Narvaal/SRMCreditEngine/releases/latest)

Plataforma de cessão de crédito multimoedas. Recebe lotes de recebíveis (duplicatas, cheques pré-datados, etc.), calcula o deságio de cada um com base no risco do ativo e na moeda de liquidação, e registra a liquidação de forma auditável.

Desenvolvido como desafio técnico, nível **Sênior** (foco em Observabilidade, Escalabilidade e Automação). O raciocínio de negócio, decisões de domínio e progresso técnico são documentados em [`ROADMAP.md`](./ROADMAP.md); o uso de IA no desenvolvimento é documentado em [`AI_USAGE.md`](./AI_USAGE.md); o enunciado completo do desafio está em [`CLAUDE.md`](./CLAUDE.md).

> **Escopo**: os itens do nível Especialista (ADRs, design para 1 milhão de tx/minuto, IaC, proposta EDA) estão deliberadamente fora desta entrega — com duas exceções incorporadas por serem baratas e complementares ao fluxo Sênior: a estratégia de branching justificada (seção abaixo) e a simulação de gestão de crise com `git cherry-pick` (`ROADMAP.md`, Passo 12).

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21 + Spring Boot 3.5, build com Gradle (Kotlin DSL) |
| Banco de dados | PostgreSQL, migrations com Flyway |
| Documentação de API | OpenAPI/Swagger (springdoc) |
| Observabilidade | Spring Boot Actuator + Micrometer → Prometheus → Grafana; logs estruturados (JSON, formato ECS) com correlation id por requisição |
| Resiliência | Resilience4j (retry + circuit breaker) na integração externa de taxas |
| Testes (backend) | JUnit 5, Testcontainers (Postgres) |
| Frontend | TypeScript + React + Vite, Tailwind CSS v4, TanStack Query, React Router, React Hook Form + Zod |
| Testes (frontend) | Vitest + Testing Library |

### Fundamentação teórica (por que esta stack)

Cada escolha amarrada ao problema — um motor financeiro multimoedas, onde precisão decimal, atomicidade e concorrência são domínio, não detalhe:

- **Java 21 + Spring Boot 3.5**: o desafio pede tipagem forte e frameworks maduros pra ambiente financeiro — Java entrega os dois com o ecossistema mais testado do mercado bancário. Os quatro pilares do domínio são recursos de primeira classe da plataforma, não bibliotecas coladas: `BigDecimal` pra aritmética decimal exata, `@Transactional` com semântica ACID real, Bean Validation na borda da API, e Optimistic Locking (`@Version`) de série no JPA. Spring Boot 3.5 ainda traz logging estruturado nativo e Micrometer/Prometheus sem código extra.
- **`ch.obermuhlner:big-math`**: a fórmula de valor presente tem expoente fracionário (prazo em meses) e o `BigDecimal.pow()` nativo só aceita expoente inteiro — cair pra `double` quebraria a precisão que é requisito. big-math resolve potência fracionária inteiramente em `BigDecimal`; a dependência fica isolada num único ponto (`MotorPrecificacao`).
- **PostgreSQL + Flyway**: banco relacional era pedido explícito; Postgres soma `NUMERIC` com precisão/escala exatas por coluna (documentadas no diagrama ER), constraints ricas (CHECK, UNIQUE compostas) que fazem o banco defender invariantes de negócio, e MVCC que casa com o Optimistic Locking da aplicação. Flyway versiona o schema como código — as migrations são a fonte de verdade do DDL.
- **Gradle (Kotlin DSL)**: build script tipado e conciso; wrapper commitado garante build reprodutível no CI.
- **React 19 + TypeScript + Vite**: TypeScript espelha os DTOs do backend em tipos (`frontend/src/api/types.ts`), pegando drift de contrato em compile-time; React pela maturidade/ecossistema; Vite por dev-server com HMR instantâneo e build enxuto.
- **TanStack Query + React Hook Form + Zod, sem Redux**: a resposta ao "gerenciamento de estado global, se necessário" do enunciado foi *não é necessário* — três fontes de verdade bem delimitadas (dados de servidor no React Query, formulário no RHF, filtros/paginação na URL) eliminam o store global. Zod valida na borda do frontend com o mesmo espírito do Bean Validation no backend.
- **Monorepo**: 1 repositório público (como pede a entrega), 1 pipeline de CI orquestrando os dois lados, e histórico correlacionando mudanças de API com mudanças de UI.

## Estrutura do repositório

Monorepo:

```
/backend    → API, regras de negócio e persistência (Java / Spring / Gradle)
/frontend   → Painel do Operador e Grid de Transações (TypeScript / React / Vite)
/docs       → Diagrama ER, diagrama C4 (Nível 1 e 2) e critérios de aceite (DDL versionado em backend/src/main/resources/db/migration)
/infra      → Configuração de Prometheus e Grafana (provisionamento, scrape config)
/.github/workflows → pipeline de CI (GitHub Actions)
docker-compose.yml → orquestra Frontend + API + PostgreSQL + Prometheus + Grafana
```

## Status atual

- [x] Entendimento do problema e decisões de domínio (ver `ROADMAP.md`)
- [x] Projeto Gradle do backend criado (Spring Boot, PostgreSQL, Flyway, Actuator/Prometheus, OpenAPI, JUnit/Testcontainers)
- [x] Projeto do frontend criado (React + TypeScript + Vite)
- [x] Git hooks (Husky): pre-commit (lint/format), commit-msg (Conventional Commits), pre-push (testes)
- [x] Modelo de dados (Diagrama ER + DDL) e migrations Flyway — ver [`docs/diagrama-er.md`](./docs/diagrama-er.md)
- [x] `docker-compose` (API + PostgreSQL + Prometheus + Grafana) — validado de ponta a ponta
- [x] Camadas de aplicação / negócio / persistência e motor de precificação (Strategy Pattern) — API funcional de ponta a ponta, ver `ROADMAP.md`
- [x] Painel do Operador (simulação em tempo real) e Grid de Transações (paginação/filtros server-side) — ver `ROADMAP.md`
- [x] CI/CD (GitHub Actions: lint + testes de backend e frontend + smoke test do `docker-compose` completo) e frontend containerizado (Nginx) no `docker-compose`
- [x] Logs estruturados (JSON/ECS) com correlation id (`requestId`) por requisição, correlacionando todas as linhas de log de uma mesma chamada — ver `ROADMAP.md`
- [x] Cobertura de testes completa: services de negócio, controllers (`@WebMvcTest`), relatório (Testcontainers), exception handler no backend; hooks orquestradores e componentes de composição no frontend — ver `ROADMAP.md`
- [x] Diagrama C4 (Nível 1 e 2) e critérios de aceite documentados (usabilidade, segurança, desempenho, escalabilidade) — ver [`docs/diagrama-c4.md`](./docs/diagrama-c4.md) e [`docs/criterios-aceite.md`](./docs/criterios-aceite.md)
- [x] Primeiro release: PR `dev → main` + tag semântica [`v1.0.0`](https://github.com/Narvaal/SRMCreditEngine/releases/tag/v1.0.0)
- [x] Simulação de gestão de crise: hotfix (`backend/Dockerfile` não-root) `git cherry-pick` de `main` pra `prod`, tag [`v1.0.1`](https://github.com/Narvaal/SRMCreditEngine/releases/tag/v1.0.1) — ver "Estratégia de branching" abaixo e `ROADMAP.md`
- [x] Resiliência: retry + circuit breaker (Resilience4j) na integração com o provider externo de taxas (mockado), com degradação graciosa — ver seção "Resiliência" abaixo e `ROADMAP.md`
- [x] Estorno pela UI (Grid, com confirmação e flag de já-estornada), cadastro de cedente inline no Painel, e containers 100% não-root (backend + frontend) — ver `ROADMAP.md`, Passo 15

## Como rodar (stack completa: API + banco + observabilidade)

Pré-requisito: Docker + Docker Compose.

```bash
docker compose up -d --build
```

Sobe 5 containers: `postgres` (aplica as 11 migrations Flyway automaticamente no boot da API), `backend`, `frontend` (build de produção, servido por Nginx), `prometheus` e `grafana`.

| Serviço | URL | Notas |
|---|---|---|
| Frontend | http://localhost:8081 | Nginx serve o build estático e faz proxy de `/api/*` pro backend |
| API | http://localhost:8080 | |
| Swagger UI | http://localhost:8080/swagger-ui/index.html | |
| Health check | http://localhost:8080/actuator/health | |
| Métricas (Prometheus scrape) | http://localhost:8080/actuator/prometheus | |
| Prometheus | http://localhost:9090 | target `srm-credit-engine` já configurado |
| Grafana | http://localhost:3000 | login `admin` / `admin`; datasource do Prometheus já provisionado |

Para derrubar tudo (incluindo os volumes de dados): `docker compose down -v`.

Logs estruturados (JSON, formato ECS) no console do backend: `docker compose logs -f backend`. Cada linha carrega `requestId` (correlaciona todas as linhas de uma mesma requisição HTTP) e, nas linhas de negócio, campos próprios (`recebivelId`, `valorLiquido`, `totalSucesso` etc.) como chaves estruturadas, não só texto.

## Como rodar (backend isolado, sem Docker)

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
./gradlew bootRun   # sobe a aplicação em http://localhost:8080, aplicando as migrations automaticamente
```

> `./gradlew test` inclui 3 testes de integração (`*IT`: concorrência com Optimistic Locking, relatório com SQL nativo e resiliência do circuit breaker) que sobem um Postgres descartável via Testcontainers. Em ambientes com Docker Engine muito recente, o probe de compatibilidade do Testcontainers 1.21.x pode falhar na inicialização (não é um bug dos testes — eles rodam no CI). Por isso o hook de pre-push roda só os testes de unidade/slice (`--tests '*Test'`), deixando os `*IT` para o CI — um hook que sempre falha viraria um hook sempre pulado.

## Principais endpoints

| Método | Rota | O quê |
|---|---|---|
| `POST` | `/api/cedentes` | Cadastra cedente |
| `POST` | `/api/recebiveis/lote` | Recebe um lote, cria e liquida cada recebível numa transação própria — resposta sempre `200` com resultado por item |
| `POST` | `/api/liquidacoes/{id}/estorno` | Estorna uma liquidação (nunca edita a original) |
| `GET`/`POST` | `/api/taxas-cambio` | Consulta/registra taxa de câmbio vigente |
| `GET`/`POST` | `/api/taxas-mercado` | Consulta/registra taxa de mercado (CDI/SOFR) vigente |
| `GET` | `/api/relatorios/extrato-liquidacao` | Extrato paginado/filtrado (cedente, moeda, período) — 2 camadas, SQL nativo |
| `POST` | `/api/recebiveis/simular` | Read-only: calcula o valor líquido sem persistir nada (usado pelo Painel do Operador) |
| `GET` | `/api/moedas`, `/api/tipos-recebivel` | Catálogos (BRL/USD, Duplicata Mercantil/Cheque Pré-datado) |
| `POST` | `/api/taxas/sincronizar` | Busca cotações no provider externo (mockado) via client com retry + circuit breaker e persiste — `503 PROVIDER_INDISPONIVEL` se o provider estiver fora |
| `GET`/`PUT` | `/mock/fx-provider/*` | Provider externo **simulado** (fora do `/api`): cotações, knob de falha (`/config?failureRate=`) e contador de chamadas (`/stats`) |

Contratos completos no Swagger UI.

## Resiliência (retry + circuit breaker)

A sincronização de taxas passa por um client HTTP protegido por **Resilience4j** (retry com backoff exponencial + circuit breaker, instância `fxProvider` no `application.yml`). Provider fora do ar degrada só a *atualização* de taxas — liquidação e simulação continuam com a última taxa vigente persistida (histórico append-only). Pra ver o circuito abrindo ao vivo:

```bash
curl -X PUT "http://localhost:8080/mock/fx-provider/config?failureRate=1.0"   # derruba o provider
curl -X POST http://localhost:8080/api/taxas/sincronizar                      # 503 após 3 retries; repetir abre o circuito
curl -s http://localhost:8080/actuator/prometheus | grep circuitbreaker_state # open = 1.0
curl -X PUT "http://localhost:8080/mock/fx-provider/config?failureRate=0.0"   # provider volta; circuito fecha após o wait (10s) + 2 syncs bons
```

No `docker-compose`, um sync agendado (`FX_PROVIDER_SYNC_ENABLED=true`, a cada 60s) mantém o circuito ciclando e visível no Prometheus/Grafana sem intervenção manual.

## Como rodar (frontend isolado, modo dev — hot reload)

Pré-requisito: Node.js 22+ e o backend no ar (`docker compose up -d` na raiz, ou `./gradlew bootRun`) — o Vite tem um proxy de dev para `/api` → `localhost:8080` (o `docker-compose` acima já sobe o frontend como build de produção via Nginx; use este modo só quando estiver editando o frontend).

```bash
cd frontend
npm install
npm run dev   # http://localhost:5173
```

Duas telas: **Painel do Operador** (`/painel`) — cadastra e liquida um recebível, com o valor líquido calculado em tempo real conforme o formulário é preenchido e cadastro de cedente inline (o cedente novo já sai selecionado) — e **Grid de Transações** (`/transacoes`) — histórico paginado com filtros por cedente/moeda/período refletidos na URL, e estorno direto na tabela (com confirmação; liquidações já estornadas ficam marcadas e sem ação).

## CI/CD

`.github/workflows/ci.yml` — dispara em push/PR para `dev`/`main`/`prod` (e manualmente). 3 jobs: `backend` (`spotlessCheck` + `./gradlew build`, incluindo os 3 testes de integração com Testcontainers — concorrência, relatório e resiliência), `frontend` (`lint` + `build` + `test`), e `docker-compose-smoke-test` (sobe a stack completa via `docker compose up -d --build` e valida que API e frontend respondem de verdade, não só que cada lado builda isolado).

## Estratégia de branching

Três branches, papéis diferentes — nenhum deploy automatizado existe hoje atrás de nenhuma delas (o CI acima só valida, não publica em lugar nenhum):

- **`dev`** — branch de trabalho. Todo o desenvolvimento acontece aqui, com Conventional Commits atômicos.
- **`main`** — branch de release. Recebe `dev` via Pull Request quando há um incremento coeso pronto pra virar versão (ex.: [PR #1](https://github.com/Narvaal/SRMCreditEngine/pull/1), a primeira entrega completa). Cada promoção relevante ganha uma tag semântica (`v1.0.0`, ...).
- **`prod`** — existe especificamente pra demonstrar o exercício de gestão de crise pedido no nível Especialista do desafio (`CLAUDE.md`, seção 6: simular um bug crítico e reagir com `git revert` seguro ou `git cherry-pick` de hotfix). Não representa um ambiente real rodando em produção — é um exercício de processo, documentado como tal (ver `ROADMAP.md`, Passo 12, pra um caso real de hotfix cherry-picked de `main` pra `prod`, `v1.0.1`).

## Documentação

- [`ROADMAP.md`](./ROADMAP.md) — entendimento do problema, decisões de domínio e progresso técnico, passo a passo.
- [`docs/diagrama-er.md`](./docs/diagrama-er.md) — diagrama ER, decisões de tipo/precisão numérica e gaps conhecidos do modelo de dados.
- [`docs/diagrama-c4.md`](./docs/diagrama-c4.md) — diagrama C4 (Nível 1 — Contexto, Nível 2 — Container).
- [`docs/criterios-aceite.md`](./docs/criterios-aceite.md) — critérios de aceite formais (usabilidade, segurança, desempenho, escalabilidade), com status e evidência por item.
- [`AI_USAGE.md`](./AI_USAGE.md) — uso de IA no desenvolvimento (prompts, correções, análise crítica).
- [`CLAUDE.md`](./CLAUDE.md) — enunciado completo do desafio técnico.
