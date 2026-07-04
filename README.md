# SRM Credit Engine

[![CI](https://github.com/Narvaal/SRMCreditEngine/actions/workflows/ci.yml/badge.svg)](https://github.com/Narvaal/SRMCreditEngine/actions/workflows/ci.yml)

Plataforma de cessão de crédito multimoedas. Recebe lotes de recebíveis (duplicatas, cheques pré-datados, etc.), calcula o deságio de cada um com base no risco do ativo e na moeda de liquidação, e registra a liquidação de forma auditável.

Desenvolvido como desafio técnico, nível **Sênior** (foco em Observabilidade, Escalabilidade e Automação). O raciocínio de negócio, decisões de domínio e progresso técnico são documentados em [`ROADMAP.md`](./ROADMAP.md); o uso de IA no desenvolvimento é documentado em [`AI_USAGE.md`](./AI_USAGE.md); o enunciado completo do desafio está em [`CLAUDE.md`](./CLAUDE.md).

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21 + Spring Boot 3.5, build com Gradle (Kotlin DSL) |
| Banco de dados | PostgreSQL, migrations com Flyway |
| Documentação de API | OpenAPI/Swagger (springdoc) |
| Observabilidade | Spring Boot Actuator + Micrometer → Prometheus → Grafana; logs estruturados (JSON, formato ECS) com correlation id por requisição |
| Testes (backend) | JUnit 5, Testcontainers (Postgres) |
| Frontend | TypeScript + React + Vite, Tailwind CSS v4, TanStack Query, React Router, React Hook Form + Zod |
| Testes (frontend) | Vitest + Testing Library |

## Estrutura do repositório

Monorepo:

```
/backend    → API, regras de negócio e persistência (Java / Spring / Gradle)
/frontend   → Painel do Operador e Grid de Transações (TypeScript / React / Vite)
/docs       → Diagrama ER, DDL, diagrama C4, ADRs — conforme o roadmap avança
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
- [x] Cobertura de testes do backend (services de negócio, exception handler) e dos hooks orquestradores do frontend — ver `ROADMAP.md`
- [x] Logs estruturados (JSON/ECS) com correlation id (`requestId`) por requisição, correlacionando todas as linhas de log de uma mesma chamada — ver `ROADMAP.md`

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

> `./gradlew test` inclui um teste de integração de concorrência (`LiquidacaoConcorrenciaIT`) que sobe um Postgres descartável via Testcontainers — requer Docker com API ≥ 1.40. Em ambientes com Docker Engine muito recente, o probe de compatibilidade do Testcontainers 1.21.x pode falhar na inicialização (não é um bug do teste); o mesmo cenário pode ser validado manualmente disparando dois `POST /api/recebiveis/lote` concorrentes contra a mesma moeda.

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

Contratos completos no Swagger UI.

## Como rodar (frontend isolado, modo dev — hot reload)

Pré-requisito: Node.js 22+ e o backend no ar (`docker compose up -d` na raiz, ou `./gradlew bootRun`) — o Vite tem um proxy de dev para `/api` → `localhost:8080` (o `docker-compose` acima já sobe o frontend como build de produção via Nginx; use este modo só quando estiver editando o frontend).

```bash
cd frontend
npm install
npm run dev   # http://localhost:5173
```

Duas telas: **Painel do Operador** (`/painel`) — cadastra e liquida um recebível, com o valor líquido calculado em tempo real conforme o formulário é preenchido — e **Grid de Transações** (`/transacoes`) — histórico paginado com filtros por cedente/moeda/período, refletidos na URL.

## CI/CD

`.github/workflows/ci.yml` — dispara em push/PR para `dev`/`main`/`prod` (e manualmente). 3 jobs: `backend` (`spotlessCheck` + `./gradlew build`, incluindo o teste de integração de concorrência com Testcontainers), `frontend` (`lint` + `build` + `test`), e `docker-compose-smoke-test` (sobe a stack completa via `docker compose up -d --build` e valida que API e frontend respondem de verdade, não só que cada lado builda isolado).

## Documentação

- [`ROADMAP.md`](./ROADMAP.md) — entendimento do problema, decisões de domínio e progresso técnico, passo a passo.
- [`docs/diagrama-er.md`](./docs/diagrama-er.md) — diagrama ER, decisões de tipo/precisão numérica e gaps conhecidos do modelo de dados.
- [`AI_USAGE.md`](./AI_USAGE.md) — uso de IA no desenvolvimento (prompts, correções, análise crítica).
- [`CLAUDE.md`](./CLAUDE.md) — enunciado completo do desafio técnico.
