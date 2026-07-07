# Cenários de demonstração do dashboard

Scripts que provocam, sob demanda, cada situação observável no dashboard **SRM Credit Engine** (http://localhost:3000/d/srm-credit-engine, `admin`/`admin`). Deixe o dashboard aberto (refresh de 10s já é o default) e rode um cenário por vez.

**Pré-requisito**: stack no ar (`docker compose up -d --build` na raiz). Só usam `bash`, `curl` e `python3`. `BASE_URL` muda o alvo (default `http://localhost:8080`).

| Script | Duração | O que provoca | Painéis que reagem |
|---|---|---|---|
| `./cenario1.sh` | ~3 min | Derruba o provider de câmbio até o circuit breaker **abrir**, mantém aberto (`DURACAO_ABERTO`, default 60s), restaura pra ver open → half-open → closed e fecha com uma fase de provider **instável** (50% de falha) que popula `successful_with_retry` | Estado do circuit breaker (stat + timeline), failure rate, retries (todas as séries), erros 5xx |
| `./cenario2.sh` | ~2 min | Três fases de erro separadas por pausa — só 400, depois só 404, depois só 409 (`FASE`/`PAUSA`, default 20s/15s) | Erros 4xx/5xx por minuto: um morro por status, sem um esmagar o outro |
| `./cenario3.sh` | 2 min | Carga paralela de leituras + simulações read-only (`DURACAO`/`CONCORRENCIA`, default 120s/8) | Requisições/s por rota, latência p95, linha Runtime (CPU/heap/Hikari) |
| `./cenario4.sh` | ~40 s | Tráfego de negócio real: `N` ciclos (default 10) de lote com 2 liquidações (BRL e cross-currency USD) + 1 estorno | Linha Negócio inteira (acumulados, operações/min, valor por moeda) — e o Grid do frontend ganha dados |
| `./cenario5.sh` | ~5 min | Uma rota por vez: rajada dedicada por endpoint com pausa entre elas (`RAJADA`/`PAUSA`, default 25/20s); escritas (lote → estorno) sequenciais pra não disputar o caixa | Requisições/s por rota com um **pico separado por rota**, latência p95, pool de conexões acompanhando cada rajada |

Combinação clássica pra uma demo completa: `cenario4.sh` num terminal (negócio), `cenario3.sh` em outro (carga) e, no final, `cenario1.sh` pra ver a timeline do circuito mudar de cor.

O `cenario1.sh` tem `trap` de saída: mesmo interrompido com Ctrl-C, restaura o provider (`failureRate=0.0`) — ninguém fica com a stack quebrada.
