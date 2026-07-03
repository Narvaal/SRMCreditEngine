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
```

## Status atual

- [x] Entendimento do problema e decisões de domínio (ver `ROADMAP.md`)
- [x] Projeto Gradle do backend criado (Spring Boot, PostgreSQL, Flyway, Actuator/Prometheus, OpenAPI, JUnit/Testcontainers)
- [x] Projeto do frontend criado (React + TypeScript + Vite)
- [x] Git hooks (Husky): pre-commit (lint/format), commit-msg (Conventional Commits), pre-push (testes)
- [x] Modelo de dados (Diagrama ER + DDL) e migrations Flyway — ver [`docs/diagrama-er.md`](./docs/diagrama-er.md)
- [ ] Camadas de aplicação / negócio / persistência e motor de precificação (Strategy Pattern)
- [ ] `docker-compose` (API + PostgreSQL + Prometheus + Grafana)
- [ ] Painel do Operador e Grid de Transações (telas reais)
- [ ] CI/CD, testes, git hooks

## Como rodar (backend)

Pré-requisitos: Java 21 e um PostgreSQL acessível (`docker-compose` para orquestrar isso ainda será adicionado — por enquanto, aponte para uma instância local via variáveis de ambiente).

```bash
cd backend

# variáveis de ambiente esperadas (valores default entre parênteses)
export DB_HOST=localhost        # (localhost)
export DB_PORT=5432             # (5432)
export DB_NAME=srm_credit_engine
export DB_USER=srm
export DB_PASSWORD=srm

./gradlew build     # compila e roda os testes
./gradlew bootRun   # sobe a aplicação em http://localhost:8080
```

Com a aplicação no ar:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Métricas Prometheus: `http://localhost:8080/actuator/prometheus`
- Health check: `http://localhost:8080/actuator/health`

> Ainda não há entidades/migrations — a aplicação sobe, mas o modelo de dados é o próximo passo (ver `ROADMAP.md`).

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
