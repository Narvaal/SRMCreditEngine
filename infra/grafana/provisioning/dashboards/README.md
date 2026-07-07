# Dashboards do Grafana

Provisionados como código: o `dashboards.yaml` (provider) faz o Grafana carregar todo `.json` deste diretório no boot, na pasta **SRM Credit Engine** da UI.

- **`srm-credit-engine.json`** — dashboard principal, 4 linhas: **Negócio** (liquidações/estornos e valor liquidado por moeda, via `srm_*` de `MetricasNegocio`), **API HTTP** (RPS por rota, latência p95 via histograma, erros 4xx/5xx), **Resiliência** (estado do circuit breaker `fxProvider` atual e ao longo do tempo, failure rate, retries, chamadas ao provider) e **Runtime** (heap, GC, CPU, pool Hikari). O tráfego de fundo vem do sync agendado do docker-compose (1 chamada/60s), que mantém o circuit breaker vivo nas métricas.

Como editar: `allowUiUpdates: false` — mudanças pela UI não persistem. Ajuste no Grafana (localhost:3000, admin/admin), exporte o JSON (Share → Export) e commite por cima do arquivo. O datasource é referenciado pelo uid fixo `prometheus` (`../datasources/prometheus.yml`).
