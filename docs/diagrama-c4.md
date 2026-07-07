# Diagrama C4 — SRM Credit Engine

Diagramas de arquitetura nos níveis 1 (Contexto) e 2 (Container), exigidos explicitamente pelo desafio no nível Sênior (`CLAUDE.md`, seção 6). Renderizados via Mermaid (mesma convenção de [`docs/diagrama-er.md`](./diagrama-er.md)) — GitHub renderiza os blocos abaixo automaticamente ao visualizar este arquivo.

**Nota de implementação**: usamos `flowchart` do Mermaid estilizado como C4 (cores/rótulos `<<person>>`/`<<system>>`/`<<container>>`), em vez do tipo nativo `C4Context`/`C4Container` do Mermaid — o motor de layout do C4 nativo usa caixas de tamanho fixo que não se ajustam ao texto, gerando rótulos sobrepostos. O `flowchart` usa o motor de layout padrão do Mermaid (mais maduro), com caixas que crescem conforme o conteúdo.

Reflete o estado real do sistema em 2026-07-06 (ver `ROADMAP.md`), não um estado aspiracional — inclusive as peças que hoje são mockadas/manuais estão marcadas como tal.

## Nível 1 — Contexto

Quem usa o sistema e com o que ele conversa por fora dos seus próprios limites.

```mermaid
flowchart TB
    operador["<b>👤 Operador de Mesa</b><br/><i>Cadastra recebíveis, dispara liquidações<br/>em lote, consulta o extrato</i>"]
    sre["<b>👤 Time de Observabilidade</b><br/><i>Acompanha métricas, logs estruturados<br/>e a saúde do sistema</i>"]

    srm["<b>🏦 SRM Credit Engine</b><br/><i>[Sistema]</i><br/>Precifica (Strategy Pattern) e liquida lotes de<br/>recebíveis multimoeda de forma transacional<br/>e auditável (ledger append-only)"]

    fonteTaxas["<b>☁️ Fonte de Câmbio/Mercado</b><br/><i>[Sistema Externo]</i><br/>BACEN/B3/FX provider no mundo real.<br/>Hoje SIMULADA por um mock interno<br/>(/mock/fx-provider) com HTTP de verdade"]

    operador -->|"Cadastra, liquida, consulta extrato<br/><i>[HTTPS]</i>"| srm
    sre -->|"Observa métricas, dashboards e logs<br/><i>[HTTP]</i>"| srm
    srm -->|"Busca cotações (câmbio, CDI/SOFR)<br/><i>[HTTP com retry + circuit breaker]</i>"| fonteTaxas

    classDef person fill:#08427B,stroke:#073B6F,color:#fff,rx:6,ry:6
    classDef system fill:#1168BD,stroke:#3C7FC0,color:#fff,rx:6,ry:6
    classDef external fill:#8a8a8a,stroke:#707070,color:#fff,rx:6,ry:6
    class operador,sre person
    class srm system
    class fonteTaxas external
```

**Fora do escopo do diagrama de propósito**: não há nenhum outro sistema externo automatizado (nenhum ERP, nenhum sistema de pagamento, nenhum provedor de auth) — o sistema hoje não tem autenticação/autorização (ver [`docs/criterios-aceite.md`](./criterios-aceite.md), seção Segurança).

## Nível 2 — Container

Zoom pra dentro do `SRM Credit Engine`: as peças deployáveis e como conversam entre si. Mapeia 1:1 com os serviços do `docker-compose.yml` da raiz.

```mermaid
flowchart TB
    operador["<b>👤 Operador de Mesa</b>"]
    sre["<b>👤 Time de Observabilidade</b>"]
    fonteTaxas["<b>☁️ Fonte de Câmbio/Mercado</b><br/><i>[Sistema Externo]</i><br/>Simulada por mock interno<br/>(/mock/fx-provider), HTTP real"]

    subgraph srm["SRM Credit Engine"]
        direction TB
        frontend["<b>📦 Frontend SPA</b><br/><i>[React 19 + TypeScript + Vite,<br/>servido por Nginx]</i><br/>Painel do Operador e<br/>Grid de Transações"]
        backend["<b>📦 API Backend</b><br/><i>[Java 21 + Spring Boot 3.5]</i><br/>API REST, precificação,<br/>liquidação transacional,<br/>relatório, logs ECS"]
        db[("<b>🗄️ PostgreSQL 17</b><br/><i>[Banco relacional]</i><br/>Moedas, cedentes, recebíveis,<br/>liquidações (ledger append-only)")]
        prometheus["<b>📦 Prometheus</b><br/><i>[Prometheus v3]</i><br/>Scrape de métricas a cada 15s"]
        grafana["<b>📦 Grafana</b><br/><i>[Grafana]</i><br/>Dashboards"]
    end

    operador -->|"Usa<br/><i>[HTTPS]</i>"| frontend
    frontend -->|"Chama API REST<br/><i>[JSON via proxy Nginx]</i>"| backend
    backend -->|"Lê/escreve<br/><i>[JDBC]</i>"| db
    prometheus -->|"Scrape de métricas<br/><i>[HTTP]</i>"| backend
    grafana -->|"Consulta métricas<br/><i>[PromQL]</i>"| prometheus
    sre -->|"Visualiza dashboards<br/><i>[HTTPS]</i>"| grafana
    backend -.->|"Busca cotações<br/><i>[HTTP, retry + circuit breaker]</i>"| fonteTaxas

    classDef person fill:#08427B,stroke:#073B6F,color:#fff,rx:6,ry:6
    classDef container fill:#1168BD,stroke:#3C7FC0,color:#fff,rx:6,ry:6
    classDef external fill:#8a8a8a,stroke:#707070,color:#fff,rx:6,ry:6
    classDef boundary fill:none,stroke:#999,stroke-dasharray: 5 5
    class operador,sre person
    class frontend,backend,prometheus,grafana,db container
    class fonteTaxas external
    class srm boundary
    linkStyle 6 stroke:#8a8a8a,stroke-width:2px,stroke-dasharray:5 5;
```

> A aresta tracejada (`API Backend` → `Fonte de Câmbio/Mercado`) sai do **API Backend**, não do PostgreSQL — o layout automático a desenha passando perto do banco só por causa da posição das caixas; o estilo tracejado/cinza é proposital pra reforçar que essa chamada é externa e mockada, diferente das chamadas internas (linhas sólidas).

### Notas sobre containers que não aparecem

- **Sem API Gateway / Load Balancer**: um único backend, uma única instância — não há necessidade hoje (ver `docs/criterios-aceite.md`, Escalabilidade, pra discussão sobre múltiplas réplicas).
- **Sem message broker / fila**: o lote de recebíveis é processado de forma síncrona dentro da própria requisição HTTP (`LiquidacaoBatchService`) — um gap conhecido de escalabilidade pra lotes muito grandes, documentado em `docs/criterios-aceite.md`.
- **Sem Loki/ELK/Graylog**: os logs estruturados (ECS) hoje só vão pro stdout do container (`docker compose logs backend`) — não há um coletor/agregador de logs rodando. ECS foi escolhido justamente por ser o formato nativo de um eventual Grafana Loki, caso esse container seja adicionado no futuro (ver `ROADMAP.md`, Passo 8). O acesso do time de observabilidade aos logs hoje é via CLI direto no host (`docker compose logs`), não uma chamada de container pra container — por isso não aparece como uma seta no diagrama acima.
