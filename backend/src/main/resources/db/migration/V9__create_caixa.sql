-- Saldo controlado por moeda. Débito do saldo e registro da liquidação acontecem
-- na mesma transação atômica; "version" habilita Optimistic Locking para proteger
-- contra duas liquidações concorrentes disputando o mesmo caixa (ver ROADMAP.md).
CREATE TABLE caixa (
    moeda_codigo    VARCHAR(3) PRIMARY KEY REFERENCES moeda(codigo),
    saldo           NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (saldo >= 0),
    version         BIGINT NOT NULL DEFAULT 0,
    atualizado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);
