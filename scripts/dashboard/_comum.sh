#!/usr/bin/env bash
# Helpers compartilhados pelos cenários de demonstração do dashboard.
# Não executar diretamente — os cenários fazem `source` deste arquivo.

BASE="${BASE_URL:-http://localhost:8080}"

titulo() { printf '\n\033[1;32m== %s ==\033[0m\n' "$*"; }
log() { printf '\033[0;36m[%s]\033[0m %s\n' "$(date +%H:%M:%S)" "$*"; }

checar_stack() {
  if ! curl -sf "$BASE/actuator/health" >/dev/null 2>&1; then
    echo "ERRO: backend não responde em $BASE — suba a stack com: docker compose up -d --build" >&2
    exit 1
  fi
}

# Estado atual do circuit breaker fxProvider (closed | open | half_open | ...).
estado_cb() {
  curl -s "$BASE/actuator/prometheus" |
    grep '^resilience4j_circuitbreaker_state{' |
    grep ' 1.0$' |
    sed 's/.*state="\([a-z_]*\)".*/\1/'
}
