#!/usr/bin/env bash
# Roda o teste de carga do extrato via container grafana/k6 (zero instalação local).
# Uso: ./carga-extrato.sh                     (8 VUs, 60s)
#      VUS=16 DURACAO=120s ./carga-extrato.sh
set -euo pipefail
cd "$(dirname "$0")"

docker run --rm --network host \
  -v "$PWD:/scripts:ro" \
  -e BASE_URL="${BASE_URL:-http://localhost:8080}" \
  -e VUS="${VUS:-8}" \
  -e DURACAO="${DURACAO:-60s}" \
  grafana/k6 run /scripts/carga-extrato.js
