# Teste de carga — Extrato de Liquidação

Fecha o critério **P5** de [`docs/criterios-aceite.md`](../../docs/criterios-aceite.md): o extrato precisa responder bem "em grandes volumes" (requisito 5 do enunciado), e até aqui os índices tinham sido desenhados por raciocínio, nunca validados com volume real.

**Pré-requisito**: stack no ar (`docker compose up -d --build` na raiz). Docker é a única dependência — o k6 roda em container (`grafana/k6`).

```bash
./seed.sh            # 1M de liquidações (~50k estornos) via generate_series — segundos, não horas
./carga-extrato.sh   # k6: 8 VUs por 60s nas 3 consultas do requisito (período, cedente, moeda)
```

`QTD=200000 ./seed.sh` semeia menos; `VUS=16 DURACAO=120s ./carga-extrato.sh` aperta mais. A massa usa cedentes `CARGA-*` (documento sintético identificável); `docker compose down -v` zera tudo.

## Resultados (1,05M de linhas, 8 VUs, 60s)

| Consulta | p95 antes | p95 depois | Threshold |
|---|---|---|---|
| Cedente + período (covering index) | 327 ms | **16 ms** | < 300 ms ✅ |
| Moeda + período | 502 ms | **113 ms** | < 300 ms ✅ |
| Sem filtro, paginação profunda (pior caso) | 2.820 ms | **193 ms** | < 800 ms ✅ |
| `http_req_failed` | **40,6%** (HTTP 500) | **0%** | < 1% ✅ |
| Throughput | 20 req/s | 115 req/s | — |

## Os 2 achados reais (por que "antes" era tão ruim)

A primeira execução falhou 40% das requisições com 500 — o teste encontrou dois problemas que nenhum teste funcional pegaria:

1. **`/dev/shm` de 64MB (default do Docker) estourando sob concorrência** — `ERROR: could not resize shared memory segment ... No space left on device`. O plano do `NOT EXISTS` do extrato (visão de estado final) era um *Parallel Hash Anti Join* com dois seq scans da tabela inteira, e os hashes paralelos de requisições concorrentes disputavam a shared memory do container. Fix: `shm_size: 256mb` no serviço `postgres` do compose.
2. **FK `liquidacao_estornada_id` sem índice** — o lado interno do anti-join era a tabela inteira (1M de linhas), com spill pra disco (`Batches: 16`, `temp written`). Fix: índice parcial `idx_liquidacao_estornada` (migration `V12`, só nas linhas `IS NOT NULL` — ~5% da tabela). O plano virou *Merge Anti Join* com dois index-only scans: o `count(*)` sem filtro caiu de 192ms pra 84ms em execução isolada, sem hash, sem `/dev/shm`, sem temp — e o p95 sob concorrência caiu 14×.

Moral registrada nos critérios de aceite: os índices "desenhados por raciocínio" estavam certos pros filtros (cedente/moeda/período), mas o custo real estava num lugar que só medição encontra.
