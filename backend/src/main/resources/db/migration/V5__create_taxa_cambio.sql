-- Histórico append-only de taxas de câmbio. Nunca UPDATE/DELETE: uma nova cotação
-- vira uma linha nova, preservando "qual taxa valia em cada instante" para auditoria.
CREATE TABLE taxa_cambio (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moeda_origem    VARCHAR(3) NOT NULL REFERENCES moeda(codigo),
    moeda_destino   VARCHAR(3) NOT NULL REFERENCES moeda(codigo),
    valor           NUMERIC(19,8) NOT NULL CHECK (valor > 0),
    vigente_em      TIMESTAMPTZ NOT NULL,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (moeda_origem <> moeda_destino),
    UNIQUE (moeda_origem, moeda_destino, vigente_em)
);

-- Ordena por vigência decrescente para achar rapidamente "a taxa vigente agora" (LIMIT 1).
CREATE INDEX idx_taxa_cambio_par_vigencia
    ON taxa_cambio (moeda_origem, moeda_destino, vigente_em DESC);
