CREATE TABLE recebivel (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cedente_id              UUID NOT NULL REFERENCES cedente(id),
    tipo_recebivel_codigo   VARCHAR(50) NOT NULL REFERENCES tipo_recebivel(codigo),
    lote_importacao_id      UUID REFERENCES lote_importacao(id),
    valor_face              NUMERIC(18,2) NOT NULL CHECK (valor_face > 0),
    moeda_titulo            VARCHAR(3) NOT NULL REFERENCES moeda(codigo),
    data_vencimento         DATE NOT NULL,
    -- 3 estados (não 2): depois de um estorno o recebível pode ser liquidado de novo,
    -- e ESTORNADO preserva essa distinção semântica em vez de voltar a PENDENTE em silêncio.
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
                                CHECK (status IN ('PENDENTE', 'LIQUIDADO', 'ESTORNADO')),
    version                 BIGINT NOT NULL DEFAULT 0,   -- optimistic locking
    criado_em               TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recebivel_cedente ON recebivel (cedente_id);

-- Índice parcial: otimiza a query mais comum (achar recebíveis liquidáveis)
-- sem pagar o custo de indexar linhas já liquidadas/estornadas.
CREATE INDEX idx_recebivel_pendente ON recebivel (cedente_id) WHERE status = 'PENDENTE';
