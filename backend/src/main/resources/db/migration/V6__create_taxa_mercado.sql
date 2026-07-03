-- Histórico append-only de taxas de mercado (CDI para BRL, SOFR para USD).
-- Mesma lógica de taxa_cambio: taxa externa, varia no tempo, nunca editada.
CREATE TABLE taxa_mercado (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moeda_codigo    CHAR(3) NOT NULL REFERENCES moeda(codigo),
    indicador       VARCHAR(20) NOT NULL,   -- ex: 'CDI', 'SOFR'
    valor           NUMERIC(9,6) NOT NULL,  -- taxa ao mês, ex: 0.010000 = 1% a.m.
    vigente_em      TIMESTAMPTZ NOT NULL,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (moeda_codigo, indicador, vigente_em)
);

CREATE INDEX idx_taxa_mercado_indicador_vigencia
    ON taxa_mercado (moeda_codigo, indicador, vigente_em DESC);
