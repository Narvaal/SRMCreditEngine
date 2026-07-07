#!/usr/bin/env bash
# Popula o banco com volume realista pro teste de carga do Extrato de Liquidação
# (critério P5 de docs/criterios-aceite.md). Direto no Postgres via SQL set-based
# (generate_series) — semear 1M de linhas pela API levaria horas; aqui leva segundos.
#
# Uso: ./seed.sh            (default: 1.000.000 de liquidações)
#      QTD=200000 ./seed.sh (volume menor)
#
# Pré-requisito: stack no ar (docker compose up -d na raiz).
set -euo pipefail

QTD="${QTD:-1000000}"
CONTAINER="${CONTAINER:-srm-postgres}"
DB_USER="${DB_USER:-srm}"
DB_NAME="${DB_NAME:-srm_credit_engine}"

echo ">> Semeando ${QTD} liquidações de carga (cedentes CARGA-*)..."

docker exec -i "$CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 <<SQL
\\timing on
begin;

-- Guarda de re-execução: a massa de carga é criada uma única vez.
do \$\$
begin
  if exists (select 1 from cedente where documento like 'CARGA-%') then
    raise exception 'Massa de carga já existe — rode "docker compose down -v" pra recomeçar do zero.';
  end if;
end
\$\$;

-- 20 cedentes dedicados à carga (documento fora do formato CPF/CNPJ de propósito:
-- massa sintética identificável e removível, nunca confundível com dado de demo).
insert into cedente (nome, documento)
select 'Cedente Carga ' || lpad(i::text, 2, '0'), 'CARGA-' || lpad(i::text, 4, '0')
from generate_series(1, 20) i;

-- Taxas de referência pros snapshots (FKs NOT NULL de liquidacao).
insert into taxa_mercado (moeda_codigo, indicador, valor, vigente_em)
values ('BRL', 'CDI', 0.011000, now());

insert into taxa_cambio (moeda_origem, moeda_destino, valor, vigente_em)
values ('BRL', 'USD', 0.18500000, now());

create temp table carga_ref as
select
  (select id from taxa_mercado order by vigente_em desc limit 1) as taxa_base_ref_id,
  (select id from taxa_cambio where moeda_origem = 'BRL' and moeda_destino = 'USD'
    order by vigente_em desc limit 1) as taxa_cambio_ref_id;

create temp table carga_cedentes as
select id, row_number() over (order by documento) - 1 as rn
from cedente where documento like 'CARGA-%';

-- 1 recebível LIQUIDADO por liquidação (FK real, 1:1 como no fluxo de verdade).
insert into recebivel (cedente_id, tipo_recebivel_codigo, valor_face, moeda_titulo,
                       data_vencimento, status)
select c.id, 'DUPLICATA_MERCANTIL',
       round((100 + random() * 99900)::numeric, 2), 'BRL',
       current_date + 30, 'LIQUIDADO'
from generate_series(1, ${QTD}) g
join carga_cedentes c on c.rn = g % 20;

-- ${QTD} liquidações espalhadas nos últimos 2 anos; ~20% cross-currency (BRL→USD),
-- respeitando o CHECK de consistência moeda × taxa de câmbio.
with massa as (
  select r.id as recebivel_id, r.cedente_id, r.valor_face,
         random() < 0.2 as cross_cur,
         now() - (random() * interval '730 days') as ts
  from recebivel r
  join carga_cedentes c on c.id = r.cedente_id
)
insert into liquidacao (recebivel_id, cedente_id, tipo, valor_face, moeda_titulo,
                        taxa_base_usada, taxa_base_ref_id, spread_usado, prazo_meses_usado,
                        valor_presente, moeda_pagamento, taxa_cambio_usada, taxa_cambio_ref_id,
                        valor_liquido, criado_em)
select m.recebivel_id, m.cedente_id, 'LIQUIDACAO', m.valor_face, 'BRL',
       0.011000, ref.taxa_base_ref_id, 0.015000, 1.5000,
       round(m.valor_face * 0.96, 6),
       case when m.cross_cur then 'USD' else 'BRL' end,
       case when m.cross_cur then 0.18500000 end,
       case when m.cross_cur then ref.taxa_cambio_ref_id end,
       case when m.cross_cur then round(m.valor_face * 0.96 * 0.185, 2)
            else round(m.valor_face * 0.96, 2) end,
       m.ts
from massa m, carga_ref ref;

-- ~5% de estornos referenciando a liquidação original (exercita o NOT EXISTS
-- do extrato com dados que de fato o ativam).
insert into liquidacao (recebivel_id, cedente_id, tipo, liquidacao_estornada_id, valor_face,
                        moeda_titulo, taxa_base_usada, taxa_base_ref_id, spread_usado,
                        prazo_meses_usado, valor_presente, moeda_pagamento, taxa_cambio_usada,
                        taxa_cambio_ref_id, valor_liquido, criado_em)
select l.recebivel_id, l.cedente_id, 'ESTORNO', l.id, l.valor_face,
       l.moeda_titulo, l.taxa_base_usada, l.taxa_base_ref_id, l.spread_usado,
       l.prazo_meses_usado, l.valor_presente, l.moeda_pagamento, l.taxa_cambio_usada,
       l.taxa_cambio_ref_id, l.valor_liquido, l.criado_em + interval '1 hour'
from liquidacao l tablesample bernoulli (5)
join carga_cedentes c on c.id = l.cedente_id
where l.tipo = 'LIQUIDACAO';

commit;

analyze recebivel;
analyze liquidacao;

select tipo, count(*) from liquidacao group by tipo order by tipo;
SQL

echo ">> Massa pronta. Rode ./carga-extrato.sh pra medir."
