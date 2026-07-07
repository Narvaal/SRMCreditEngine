#!/usr/bin/env bash
# Cenário 2 — Erros 4xx em fases: um morro por status (~2 min no default).
#
# Três fases separadas por pausa, cada uma disparando só um tipo de erro:
# 400 (payload/param inválido) → 404 (cedente inexistente) → 409 (estorno de
# liquidação inexistente). No painel de erros cada status vira um pico próprio,
# em vez de uma mistura onde o 400 esmaga os outros.
#
# Painéis: "Erros 4xx/5xx por minuto" (um morro por série) e "Requisições/s por rota".
#
# Uso: ./cenario2.sh              (20s por fase, 15s de pausa)
#      FASE=10 PAUSA=5 ./cenario2.sh

set -euo pipefail
cd "$(dirname "$0")"
source ./_comum.sh

FASE="${FASE:-20}"
PAUSA="${PAUSA:-15}"

checar_stack
titulo "Cenário 2 — três fases de erro (400 → 404 → 409), ${FASE}s cada"

rodar_fase() {
  local nome="$1" && shift
  log "fase $nome por ${FASE}s..."
  local fim=$((SECONDS + FASE))
  while [ $SECONDS -lt $fim ]; do
    "$@"
    sleep 0.3
  done
  log "fase $nome encerrada — pausa de ${PAUSA}s pro pico se destacar"
  sleep "$PAUSA"
}

erros_400() {
  curl -s -o /dev/null -X POST "$BASE/api/cedentes" \
    -H 'Content-Type: application/json' -d '{"nome":"x","documento":"123"}'
  curl -s -o /dev/null "$BASE/api/relatorios/extrato-liquidacao?page=-1"
}

erros_404() {
  curl -s -o /dev/null "$BASE/api/cedentes/$(cat /proc/sys/kernel/random/uuid)"
}

erros_409() {
  curl -s -o /dev/null -X POST \
    "$BASE/api/liquidacoes/$(cat /proc/sys/kernel/random/uuid)/estorno"
}

rodar_fase "400" erros_400
rodar_fase "404" erros_404
rodar_fase "409" erros_409

titulo "Fim — três morros no painel de erros: HTTP 400, depois 404, depois 409."
