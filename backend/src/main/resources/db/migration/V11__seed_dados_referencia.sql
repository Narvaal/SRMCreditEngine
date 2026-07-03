INSERT INTO moeda (codigo, nome) VALUES
    ('BRL', 'Real Brasileiro'),
    ('USD', 'Dólar Americano');

INSERT INTO tipo_recebivel (codigo, nome) VALUES
    ('DUPLICATA_MERCANTIL', 'Duplicata Mercantil'),
    ('CHEQUE_PRE_DATADO', 'Cheque Pré-datado');

-- Saldo inicial de caixa: artefato de seed/demo, não uma regra de negócio
-- (não existe ainda mecanismo de aporte/capitalização do fundo — gap conhecido,
-- documentado em docs/diagrama-er.md).
INSERT INTO caixa (moeda_codigo, saldo) VALUES
    ('BRL', 1000000.00),
    ('USD', 200000.00);
