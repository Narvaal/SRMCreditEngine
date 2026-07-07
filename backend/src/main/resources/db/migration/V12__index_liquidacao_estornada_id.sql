-- Achado do teste de carga com 1M de linhas (scripts/carga): o extrato filtra
-- "visão de estado final" com NOT EXISTS sobre liquidacao_estornada_id, e sem
-- índice o Postgres resolve com hash anti-join de dois seq scans da tabela
-- inteira — lento, e o hash paralelo disputa /dev/shm sob concorrência.
-- Parcial porque só a minoria das linhas é estorno (~5% na massa de carga):
-- o lado interno do anti-join vira um índice pequeno em vez da tabela toda.
CREATE INDEX idx_liquidacao_estornada
    ON liquidacao (liquidacao_estornada_id)
    WHERE liquidacao_estornada_id IS NOT NULL;
