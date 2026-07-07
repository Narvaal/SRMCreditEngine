#!/usr/bin/env bash
# Cenário 3 — Carga de requisições (2 min no default).
#
# Rajadas paralelas de leitura (extrato paginado, catálogos) e simulação de
# precificação (read-only, nada é persistido) pra encher os painéis de RPS e
# latência p95 — e mexer nos ponteiros de CPU/heap/pool da linha Runtime.
#
# Painéis: "Requisições/s por rota", "Latência p95 por rota", linha "Runtime".
#
# Uso: ./cenario3.sh                          (120s, 8 workers por rajada)
#      DURACAO=30 CONCORRENCIA=4 ./cenario3.sh

set -euo pipefail
cd "$(dirname "$0")"
source ./_comum.sh

DURACAO="${DURACAO:-120}"
CONCORRENCIA="${CONCORRENCIA:-8}"

checar_stack
titulo "Cenário 3 — ${DURACAO}s de carga com $CONCORRENCIA requisições paralelas por rajada"

uma_requisicao() {
  case $((RANDOM % 4)) in
    0) curl -s -o /dev/null "$BASE/api/relatorios/extrato-liquidacao?page=$((RANDOM % 5))&size=20" ;;
    1) curl -s -o /dev/null "$BASE/api/moedas" ;;
    2) curl -s -o /dev/null "$BASE/api/tipos-recebivel" ;;
    3) curl -s -o /dev/null -X POST "$BASE/api/recebiveis/simular" \
      -H 'Content-Type: application/json' \
      -d "{\"tipoRecebivelCodigo\":\"DUPLICATA_MERCANTIL\",\"valorFace\":$((RANDOM % 9000 + 1000)).00,\"moedaTitulo\":\"BRL\",\"dataVencimento\":\"2026-09-30\",\"moedaPagamento\":\"BRL\"}" ;;
  esac
}

total=0
fim=$((SECONDS + DURACAO))
while [ $SECONDS -lt $fim ]; do
  for _ in $(seq 1 "$CONCORRENCIA"); do
    uma_requisicao &
  done
  wait
  total=$((total + CONCORRENCIA))
  log "$total requisições disparadas ($(( fim - SECONDS ))s restantes)"
done

titulo "Fim — $total requisições. Compare o p95 das rotas de leitura vs. /simular."
