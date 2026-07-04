CREATE TABLE cedente (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome        VARCHAR(255) NOT NULL,
    documento   VARCHAR(20) NOT NULL UNIQUE,   -- CNPJ
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);
