# Diagrama C4 — SRM Credit Engine

Diagramas de arquitetura nos níveis 1 (Contexto) e 2 (Container), exigidos explicitamente pelo desafio no nível Sênior (`CLAUDE.md`, seção 6). Renderizados via Mermaid (mesma convenção de [`docs/diagrama-er.md`](./diagrama-er.md)) — GitHub renderiza os blocos abaixo automaticamente ao visualizar este arquivo.

Reflete o estado real do sistema em 2026-07-04 (ver `ROADMAP.md`), não um estado aspiracional — inclusive as peças que hoje são mockadas/manuais estão marcadas como tal.

## Nível 1 — Contexto

Quem usa o sistema e com o que ele conversa por fora dos seus próprios limites.

```mermaid
C4Context
    title Diagrama de Contexto (C4 Nível 1) — SRM Credit Engine

    Person(operador, "Operador de Mesa", "Cadastra recebíveis, dispara liquidações em lote, consulta o extrato")
    Person(sre, "Time de Observabilidade", "Acompanha métricas, logs estruturados e a saúde do sistema")

    System(srm, "SRM Credit Engine", "Precifica (Strategy Pattern) e liquida lotes de recebíveis multimoeda de forma transacional e auditável (ledger append-only)")

    System_Ext(fonteTaxas, "Fonte de Câmbio/Mercado", "BACEN/B3/FX provider no mundo real. Hoje MOCKADA: taxas de câmbio e CDI/SOFR entram via POST manual (Swagger/API), sem integração automática")

    Rel(operador, srm, "Cadastra cedentes/recebíveis, liquida lotes, consulta extrato", "HTTPS")
    Rel(sre, srm, "Observa métricas Prometheus, dashboards Grafana e logs JSON estruturados", "HTTP")
    Rel(srm, fonteTaxas, "Registra a taxa vigente (câmbio e mercado)", "POST manual, mockado")
```

**Fora do escopo do diagrama de propósito**: não há nenhum outro sistema externo automatizado (nenhum ERP, nenhum sistema de pagamento, nenhum provedor de auth) — o sistema hoje não tem autenticação/autorização (ver [`docs/criterios-aceite.md`](./criterios-aceite.md), seção Segurança).

## Nível 2 — Container

Zoom pra dentro do `SRM Credit Engine`: as peças deployáveis e como conversam entre si. Mapeia 1:1 com os serviços do `docker-compose.yml` da raiz.

```mermaid
C4Container
    title Diagrama de Container (C4 Nível 2) — SRM Credit Engine

    Person(operador, "Operador de Mesa", "")
    Person(sre, "Time de Observabilidade", "")

    System_Boundary(srm, "SRM Credit Engine") {
        Container(frontend, "Frontend SPA", "React 19 + TypeScript + Vite, build servido por Nginx", "Painel do Operador (simulação em tempo real) e Grid de Transações (paginação/filtros server-side)")
        Container(backend, "API Backend", "Java 21 + Spring Boot 3.5 (Gradle)", "API REST, motor de precificação, liquidação transacional (Optimistic Locking), relatório de 2 camadas, logs estruturados ECS")
        ContainerDb(db, "PostgreSQL 17", "Banco relacional", "Moedas, cedentes, recebíveis, taxas (histórico append-only), liquidações (ledger append-only) — 11 migrations Flyway")
        Container(prometheus, "Prometheus", "Prometheus v3", "Faz scrape de métricas do backend a cada 15s")
        Container(grafana, "Grafana", "Grafana", "Dashboards sobre as métricas do Prometheus")
    }

    System_Ext(fonteTaxas, "Fonte de Câmbio/Mercado", "Mockada — POST manual via Swagger/API")

    Rel(operador, frontend, "Usa no navegador", "HTTPS")
    Rel(frontend, backend, "Chama a API REST", "JSON via proxy Nginx /api")
    Rel(backend, db, "Lê/escreve (JPA + SQL nativo)", "JDBC")
    Rel(prometheus, backend, "Scrape de métricas", "HTTP")
    Rel(grafana, prometheus, "Consulta métricas", "PromQL")
    Rel(sre, grafana, "Visualiza dashboards", "HTTPS")
    Rel(backend, fonteTaxas, "Registra taxa vigente", "POST manual")
```

### Notas sobre containers que não aparecem

- **Sem API Gateway / Load Balancer**: um único backend, uma única instância — não há necessidade hoje (ver `docs/criterios-aceite.md`, Escalabilidade, pra discussão sobre múltiplas réplicas).
- **Sem message broker / fila**: o lote de recebíveis é processado de forma síncrona dentro da própria requisição HTTP (`LiquidacaoBatchService`) — um gap conhecido de escalabilidade pra lotes muito grandes, documentado em `docs/criterios-aceite.md`.
- **Sem Loki/ELK/Graylog**: os logs estruturados (ECS) hoje só vão pro stdout do container (`docker compose logs backend`) — não há um coletor/agregador de logs rodando. ECS foi escolhido justamente por ser o formato nativo de um eventual Grafana Loki, caso esse container seja adicionado no futuro (ver `ROADMAP.md`, Passo 8). O acesso do time de observabilidade aos logs hoje é via CLI direto no host (`docker compose logs`), não uma chamada de container pra container — por isso não aparece como uma seta no diagrama acima.
