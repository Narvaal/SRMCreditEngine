# Dashboards do Grafana

Ainda vazio — é o único nice-to-have em aberto do `ROADMAP.md`. Dashboards em JSON exportado do Grafana entram aqui; já existem métricas reais pra visualizar (HTTP/JVM via Actuator e o estado do circuit breaker — `resilience4j_circuitbreaker_state` — que cicla sozinho com o sync agendado do docker-compose), mas nenhum painel foi construído. O datasource do Prometheus está provisionado automaticamente (`../datasources/prometheus.yml`).
