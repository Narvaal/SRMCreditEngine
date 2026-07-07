#!/usr/bin/env bash
# Cenário 1 — Circuit breaker: abre, fica aberto e se recupera (~2–3 min).
#
# Derruba o provider mockado de câmbio (failureRate=1.0) e sincroniza taxas até o
# circuito fxProvider abrir; mantém aberto por DURACAO_ABERTO segundos (503 de
# short-circuit alimentando o painel de 5xx) e então restaura o provider pra ver a
# recuperação open → half-open → closed.
#
# Painéis: "Circuit breaker — estado atual" e "— ao longo do tempo", "Failure rate",
# "Retries por minuto" e "Erros 4xx/5xx por minuto" (503).
#
# Uso: ./cenario1.sh            (60s de circuito aberto)
#      DURACAO_ABERTO=30 ./cenario1.sh

set -euo pipefail
cd "$(dirname "$0")"
source ./_comum.sh

DURACAO_ABERTO="${DURACAO_ABERTO:-60}"

# Mesmo com Ctrl-C o provider volta ao normal — ninguém fica com a stack "quebrada".
restaurar() { curl -s -X PUT "$BASE/mock/fx-provider/config?failureRate=0.0" >/dev/null || true; }
trap restaurar EXIT

checar_stack
titulo "Cenário 1 — ciclo completo do circuit breaker fxProvider"

log "derrubando o provider (failureRate=1.0)"
curl -s -X PUT "$BASE/mock/fx-provider/config?failureRate=1.0" >/dev/null

log "sincronizando até o circuito abrir (cada sync = 3 tentativas com retry)..."
for i in $(seq 1 10); do
  http=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/taxas/sincronizar")
  estado=$(estado_cb)
  log "sync #$i → HTTP $http | circuito: $estado"
  [ "$estado" = "open" ] && break
  sleep 1
done

if [ "$(estado_cb)" != "open" ]; then
  echo "ERRO: circuito não abriu — provider mockado está mesmo em failureRate=1.0?" >&2
  exit 1
fi

log "circuito ABERTO — mantendo por ${DURACAO_ABERTO}s (503 rápidos, sem chamadas reais ao provider)"
fim=$((SECONDS + DURACAO_ABERTO))
while [ $SECONDS -lt $fim ]; do
  http=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/taxas/sincronizar")
  log "sync com circuito aberto → HTTP $http | circuito: $(estado_cb)"
  sleep 5
done

log "restaurando o provider (failureRate=0.0) e aguardando o wait de 10s do circuito"
restaurar
sleep 11

log "2 syncs bons pra fechar o half-open..."
for i in 1 2 3 4; do
  http=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/taxas/sincronizar")
  estado=$(estado_cb)
  log "sync de recuperação #$i → HTTP $http | circuito: $estado"
  [ "$estado" = "closed" ] && break
  sleep 3
done

# Fase extra: provider INSTÁVEL (50% de falha) — algumas tentativas falham e o retry
# salva a chamada, populando a série successful_with_retry (que nunca aparece com o
# provider 100% ok ou 100% morto).
log "fase instável: failureRate=0.5 por 10 syncs — observe successful_with_retry no painel de retries"
curl -s -X PUT "$BASE/mock/fx-provider/config?failureRate=0.5" >/dev/null
for i in $(seq 1 10); do
  http=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/taxas/sincronizar")
  log "sync instável #$i → HTTP $http | circuito: $(estado_cb)"
  sleep 2
done
restaurar

titulo "Fim — circuito: $(estado_cb). Veja o ciclo verde→vermelho→âmbar→verde na state-timeline."
