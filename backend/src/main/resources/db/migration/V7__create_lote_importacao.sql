-- Rastreia de qual submissão (lote) cada recebível veio, para auditoria.
-- A liquidação em si é sempre por recebível, nunca por lote (ver ROADMAP.md).
CREATE TABLE lote_importacao (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    origem      VARCHAR(50) NOT NULL DEFAULT 'API',
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);
