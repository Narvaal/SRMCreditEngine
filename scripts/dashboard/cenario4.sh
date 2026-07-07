#!/usr/bin/env bash
# Cenário 4 — Tráfego de negócio: liquidações e estornos (~40s no default).
#
# Cria (ou reusa) um cedente demo e, a cada ciclo, liquida um lote de 2 recebíveis
# (BRL→BRL e cross-currency BRL→USD, valores aleatórios) e estorna a primeira
# liquidação — alimentando as métricas srm_* instrumentadas no LiquidacaoService.
# O Grid de Transações do frontend também ganha dados de demonstração.
#
# Painéis: "Liquidações/Estornos (acumulado)", "Operações por minuto",
# "Valor liquidado por minuto (moeda de pagamento)".
#
# Uso: ./cenario4.sh        (10 ciclos, pausa de 4s)
#      N=25 PAUSA=2 ./cenario4.sh

set -euo pipefail
cd "$(dirname "$0")"
source ./_comum.sh

N="${N:-10}"
PAUSA="${PAUSA:-4}"
CNPJ_DEMO="11222333000181"

checar_stack
titulo "Cenário 4 — $N ciclos de lote (2 liquidações) + 1 estorno"

# Cria o cedente demo; se já existe (409), busca o id pelo documento no catálogo.
resposta=$(curl -s -X POST "$BASE/api/cedentes" -H 'Content-Type: application/json' \
  -d "{\"nome\":\"Cedente Demo Ltda\",\"documento\":\"$CNPJ_DEMO\"}")
cedente_id=$(echo "$resposta" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("id",""))')
if [ -z "$cedente_id" ]; then
  cedente_id=$(curl -s "$BASE/api/cedentes" | python3 -c "
import json, sys
print(next(c['id'] for c in json.load(sys.stdin) if c['documento'] == '$CNPJ_DEMO'))")
fi
log "cedente demo: $cedente_id"

for i in $(seq 1 "$N"); do
  v1="$((RANDOM % 4500 + 500)).00"
  v2="$((RANDOM % 4500 + 500)).00"
  lote=$(curl -s -X POST "$BASE/api/recebiveis/lote" -H 'Content-Type: application/json' -d "{\"itens\":[
    {\"cedenteId\":\"$cedente_id\",\"tipoRecebivelCodigo\":\"DUPLICATA_MERCANTIL\",\"valorFace\":$v1,\"moedaTitulo\":\"BRL\",\"dataVencimento\":\"2026-08-30\",\"moedaPagamento\":\"BRL\"},
    {\"cedenteId\":\"$cedente_id\",\"tipoRecebivelCodigo\":\"CHEQUE_PRE_DATADO\",\"valorFace\":$v2,\"moedaTitulo\":\"BRL\",\"dataVencimento\":\"2026-09-15\",\"moedaPagamento\":\"USD\"}]}")
  liquidacao_id=$(echo "$lote" | python3 -c '
import json, sys
itens = json.load(sys.stdin)["itens"]
ok = [i for i in itens if i["sucesso"]]
print(ok[0]["liquidacao"]["id"] if ok else "")')
  if [ -z "$liquidacao_id" ]; then
    log "ciclo $i/$N → lote sem sucesso (saldo de caixa esgotado?): $(echo "$lote" | head -c 200)"
  else
    estorno=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/api/liquidacoes/$liquidacao_id/estorno")
    log "ciclo $i/$N → lote R\$ $v1 + US\$ (de R\$ $v2) liquidado; estorno da 1ª → HTTP $estorno"
  fi
  sleep "$PAUSA"
done

acumulado=$(curl -s "${PROMETHEUS_URL:-http://localhost:9090}/api/v1/query" \
  --data-urlencode 'query=sum(srm_liquidacoes_total)' |
  python3 -c 'import json,sys; r=json.load(sys.stdin)["data"]["result"]; print(r[0]["value"][1] if r else "?")' \
  2>/dev/null || echo "?")
titulo "Fim — sum(srm_liquidacoes_total) no Prometheus: $acumulado. Veja a linha Negócio subindo."
