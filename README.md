# SRM Credit Engine

Plataforma de cessão de crédito multimoedas. Recebe lotes de recebíveis (duplicatas, cheques pré-datados, etc.), calcula o deságio de cada um com base no risco do ativo e na moeda de liquidação, e registra a liquidação de forma auditável.

Desenvolvido como desafio técnico, nível **Sênior** (foco em Observabilidade, Escalabilidade e Automação). O raciocínio de negócio, decisões de domínio e progresso técnico são documentados em [`ROADMAP.md`](./ROADMAP.md); o uso de IA no desenvolvimento é documentado em [`AI_USAGE.md`](./AI_USAGE.md); o enunciado completo do desafio está em [`CLAUDE.md`](./CLAUDE.md).

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21 + Spring Boot 3.5, build com Gradle (Kotlin DSL) |
| Banco de dados | PostgreSQL, migrations com Flyway |
| Documentação de API | OpenAPI/Swagger (springdoc) |
| Observabilidade | Spring Boot Actuator + Micrometer → Prometheus → Grafana |
| Testes (backend) | JUnit 5, Testcontainers (Postgres) |
| Frontend | TypeScript + React, build com Vite |

## Estrutura do repositório

Monorepo:

```
/backend    → API, regras de negócio e persistência (Java / Spring / Gradle)
/frontend   → Painel do Operador e Grid de Transações (TypeScript / React / Vite)
/docs       → Diagrama ER, DDL, diagrama C4, ADRs — conforme o roadmap avança
/infra      → Configuração de Prometheus e Grafana (provisionamento, scrape config)
docker-compose.yml → orquestra API + PostgreSQL + Prometheus + Grafana
```

## Status atual

- [x] Entendimento do problema e decisões de domínio (ver `ROADMAP.md`)
- [x] Projeto Gradle do backend criado (Spring Boot, PostgreSQL, Flyway, Actuator/Prometheus, OpenAPI, JUnit/Testcontainers)
- [x] Projeto do frontend criado (React + TypeScript + Vite)
- [x] Git hooks (Husky): pre-commit (lint/format), commit-msg (Conventional Commits), pre-push (testes)
- [x] Modelo de dados (Diagrama ER + DDL) e migrations Flyway — ver [`docs/diagrama-er.md`](./docs/diagrama-er.md)
- [x] `docker-compose` (API + PostgreSQL + Prometheus + Grafana) — validado de ponta a ponta
- [x] Camadas de aplicação / negócio / persistência e motor de precificação (Strategy Pattern) — API funcional de ponta a ponta, ver `ROADMAP.md`
- [ ] Painel do Operador e Grid de Transações (telas reais)
- [ ] CI/CD

## Como rodar (stack completa: API + banco + observabilidade)

Pré-requisito: Docker + Docker Compose.

```bash
docker compose up -d --build
```

Sobe 4 containers: `postgres` (aplica as 11 migrations Flyway automaticamente no boot da API), `backend`, `prometheus` e `grafana`.

| Serviço | URL | Notas |
|---|---|---|
| API | http://localhost:8080 | |
| Swagger UI | http://localhost:8080/swagger-ui/index.html | |
| Health check | http://localhost:8080/actuator/health | |
| Métricas (Prometheus scrape) | http://localhost:8080/actuator/prometheus | |
| Prometheus | http://localhost:9090 | target `srm-credit-engine` já configurado |
| Grafana | http://localhost:3000 | login `admin` / `admin`; datasource do Prometheus já provisionado |

Para derrubar tudo (incluindo os volumes de dados): `docker compose down -v`.

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

Contratos completos no Swagger UI.

## Como rodar (frontend)

Pré-requisito: Node.js 22+.

```bash
cd frontend
npm install
npm run dev   # http://localhost:5173
```

Ainda é o scaffold padrão do Vite — as telas do Painel do Operador e do Grid de Transações são o próximo passo do frontend (ver `ROADMAP.md`).

## Documentação

- [`ROADMAP.md`](./ROADMAP.md) — entendimento do problema, decisões de domínio e progresso técnico, passo a passo.
- [`docs/diagrama-er.md`](./docs/diagrama-er.md) — diagrama ER, decisões de tipo/precisão numérica e gaps conhecidos do modelo de dados.
- [`AI_USAGE.md`](./AI_USAGE.md) — uso de IA no desenvolvimento (prompts, correções, análise crítica).
- [`CLAUDE.md`](./CLAUDE.md) — enunciado completo do desafio técnico.
