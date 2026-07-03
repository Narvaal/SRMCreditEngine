CREATE TABLE moeda (
    codigo          CHAR(3) PRIMARY KEY,              -- ISO 4217, ex: 'BRL', 'USD'
    nome            VARCHAR(100) NOT NULL,
    casas_decimais  SMALLINT NOT NULL DEFAULT 2 CHECK (casas_decimais >= 0),
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT now()
);
