#!/usr/bin/env bash
# Cenário 5 — Uma rota por vez: um pico colorido por endpoint (~5 min no default).
#
# Visita as rotas da API em sequência, com uma rajada dedicada por rota (leituras
# com paralelismo leve; escritas sequenciais pra não disputar o caixa) e pausa
# entre elas — no painel de RPS cada rota vira um pico próprio, um atrás do outro,
# e o pool de conexões (checkouts/s) acompanha cada rajada.
#
# Painéis: "Requisições/s por rota" (picos sequenciais), "Latência p95",
# "Pool de conexões (HikariCP)".
#
# Uso: ./cenario5.sh                (rajada ~8s por rota, pausa 20s)
#      RAJADA=10 PAUSA=8 ./cenario5.sh

set -euo pipefail
cd "$(dirname "$0")"
source ./_comum.sh

RAJADA="${RAJADA:-25}"   # iterações por rota (leituras disparam 4 em paralelo por iteração)
PAUSA="${PAUSA:-20}"     # segundos entre rotas
CNPJ_DEMO="11222333000181"

checar_stack
titulo "Cenário 5 — um pico por rota (rajada de $RAJADA iterações, pausa de ${PAUSA}s)"

# Cedente demo (cria ou reusa — mesmo documento dos cenários 2 e 4).
resposta=$(curl -s -X POST "$BASE/api/cedentes" -H 'Content-Type: application/json' \
  -d "{\"nome\":\"Cedente Demo Ltda\",\"documento\":\"$CNPJ_DEMO\"}")
cedente_id=$(echo "$resposta" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("id",""))')
if [ -z "$cedente_id" ]; then
  cedente_id=$(curl -s "$BASE/api/cedentes" | python3 -c "
import json, sys
print(next(c['id'] for c in json.load(sys.stdin) if c['documento'] == '$CNPJ_DEMO'))")
fi

# Rajada de leitura: 4 requisições paralelas por iteração — RPS visível e o pool mexe.
rajada_leitura() {
  local nome="$1" && shift
  log "rota $nome — rajada de leitura"
  for _ in $(seq 1 "$RAJADA"); do
    "$@" & "$@" & "$@" & "$@" &
    wait
    sleep 0.1
  done
  log "pausa de ${PAUSA}s"
  sleep "$PAUSA"
}

hit() { curl -s -o /dev/null "$1"; }

rajada_leitura "/api/moedas" hit "$BASE/api/moedas"
rajada_leitura "/api/tipos-recebivel" hit "$BASE/api/tipos-recebivel"
rajada_leitura "/api/cedentes" hit "$BASE/api/cedentes"
rajada_leitura "/api/cedentes/{id}" hit "$BASE/api/cedentes/$cedente_id"
rajada_leitura "/api/taxas-mercado" hit "$BASE/api/taxas-mercado?moedaCodigo=BRL"
rajada_leitura "/api/taxas-cambio" hit "$BASE/api/taxas-cambio?moedaOrigem=USD&moedaDestino=BRL"
rajada_leitura "/api/relatorios/extrato-liquidacao" hit "$BASE/api/relatorios/extrato-liquidacao?page=0&size=20"

simular() {
  curl -s -o /dev/null -X POST "$BASE/api/recebiveis/simular" -H 'Content-Type: application/json' \
    -d "{\"tipoRecebivelCodigo\":\"DUPLICATA_MERCANTIL\",\"valorFace\":$((RANDOM % 900 + 100)).00,\"moedaTitulo\":\"BRL\",\"dataVencimento\":\"2026-09-30\",\"moedaPagamento\":\"BRL\"}"
}
rajada_leitura "/api/recebiveis/simular" simular

sincronizar() { curl -s -o /dev/null -X POST "$BASE/api/taxas/sincronizar"; }
rajada_leitura "/api/taxas/sincronizar" sincronizar

# Escritas: sequenciais (transações concorrentes no mesmo caixa gerariam 409 de
# Optimistic Locking e sujariam o painel de erros). Guarda os ids pro estorno.
log "rota /api/recebiveis/lote — $RAJADA lotes sequenciais"
liquidacoes=()
for _ in $(seq 1 "$RAJADA"); do
  id=$(curl -s -X POST "$BASE/api/recebiveis/lote" -H 'Content-Type: application/json' -d "{\"itens\":[
    {\"cedenteId\":\"$cedente_id\",\"tipoRecebivelCodigo\":\"DUPLICATA_MERCANTIL\",\"valorFace\":$((RANDOM % 900 + 100)).00,\"moedaTitulo\":\"BRL\",\"dataVencimento\":\"2026-08-30\",\"moedaPagamento\":\"BRL\"}]}" |
    python3 -c 'import json,sys; itens=[i for i in json.load(sys.stdin)["itens"] if i["sucesso"]]; print(itens[0]["liquidacao"]["id"] if itens else "")')
  [ -n "$id" ] && liquidacoes+=("$id")
  sleep 0.2
done
log "pausa de ${PAUSA}s"
sleep "$PAUSA"

log "rota /api/liquidacoes/{id}/estorno — estornando as ${#liquidacoes[@]} liquidações"
for id in "${liquidacoes[@]}"; do
  curl -s -o /dev/null -X POST "$BASE/api/liquidacoes/$id/estorno"
  sleep 0.2
done

titulo "Fim — um pico por rota no painel de RPS, na ordem em que os logs mostraram."
