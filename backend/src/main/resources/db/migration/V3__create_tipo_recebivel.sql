-- Catálogo puro: sem coluna de spread. O código ('codigo') é a chave usada pelo
-- Strategy correspondente no motor de precificação para selecionar a regra de risco;
-- o valor do spread mora exclusivamente na implementação do Strategy no código,
-- evitando duas fontes de verdade (banco vs. código) sobre a mesma regra de negócio.
CREATE TABLE tipo_recebivel (
    codigo      VARCHAR(50) PRIMARY KEY,   -- ex: 'DUPLICATA_MERCANTIL', 'CHEQUE_PRE_DATADO'
    nome        VARCHAR(100) NOT NULL,
    ativo       BOOLEAN NOT NULL DEFAULT true,
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);
