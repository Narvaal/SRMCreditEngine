-- Ledger append-only: nunca UPDATE/DELETE. Correção de erro = nova linha de ESTORNO
-- referenciando a original (e, se necessário, um novo lançamento correto). Ver ROADMAP.md.
CREATE TABLE liquidacao (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recebivel_id                UUID NOT NULL REFERENCES recebivel(id),
    cedente_id                  UUID NOT NULL REFERENCES cedente(id),   -- denormalizado p/ relatório (Extrato de Liquidação)
    tipo                        VARCHAR(20) NOT NULL CHECK (tipo IN ('LIQUIDACAO', 'ESTORNO')),
    liquidacao_estornada_id     UUID REFERENCES liquidacao(id),

    -- Snapshot dos insumos do cálculo: valor guardado diretamente (leitura sem join)
    -- + FK para a fonte (rastreabilidade/auditoria) onde aplicável.
    valor_face                  NUMERIC(18,2) NOT NULL CHECK (valor_face > 0),
    moeda_titulo                VARCHAR(3) NOT NULL REFERENCES moeda(codigo),

    taxa_base_usada             NUMERIC(9,6) NOT NULL,
    taxa_base_ref_id            UUID NOT NULL REFERENCES taxa_mercado(id),
    spread_usado                NUMERIC(9,6) NOT NULL,
    prazo_meses_usado           NUMERIC(9,4) NOT NULL,
    valor_presente              NUMERIC(20,6) NOT NULL,

    moeda_pagamento             VARCHAR(3) NOT NULL REFERENCES moeda(codigo),
    taxa_cambio_usada           NUMERIC(19,8),
    taxa_cambio_ref_id          UUID REFERENCES taxa_cambio(id),

    -- Sempre positivo; a direção do efeito no caixa (débito vs. crédito de volta)
    -- é inferida por "tipo", não por sinal — mesma convenção de valor_face > 0.
    valor_liquido               NUMERIC(18,2) NOT NULL CHECK (valor_liquido > 0),
    criado_em                   TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- (1 + taxa) precisa ser positivo, senão a exponenciação com prazo fracionário
    -- gera resultado indefinido.
    CHECK (1 + taxa_base_usada + spread_usado > 0),

    -- Regra de negócio: moeda de título igual à de pagamento ⟺ sem conversão cambial;
    -- moedas diferentes ⟺ taxa de câmbio obrigatória.
    CHECK (
        (moeda_pagamento = moeda_titulo AND taxa_cambio_usada IS NULL AND taxa_cambio_ref_id IS NULL)
        OR
        (moeda_pagamento <> moeda_titulo AND taxa_cambio_usada IS NOT NULL AND taxa_cambio_ref_id IS NOT NULL)
    ),

    -- LIQUIDACAO nunca aponta para outra linha; ESTORNO sempre aponta para a que reverte.
    CHECK (
        (tipo = 'LIQUIDACAO' AND liquidacao_estornada_id IS NULL)
        OR
        (tipo = 'ESTORNO' AND liquidacao_estornada_id IS NOT NULL)
    )
);

-- Covering index: a query mais comum do Extrato de Liquidação (cedente + período)
-- pode ser respondida via index-only scan.
CREATE INDEX idx_liquidacao_cedente_periodo
    ON liquidacao (cedente_id, criado_em DESC)
    INCLUDE (moeda_titulo, moeda_pagamento, tipo, valor_liquido);

CREATE INDEX idx_liquidacao_periodo ON liquidacao (criado_em DESC);
CREATE INDEX idx_liquidacao_moeda_periodo ON liquidacao (moeda_pagamento, criado_em DESC);
CREATE INDEX idx_liquidacao_recebivel ON liquidacao (recebivel_id);
