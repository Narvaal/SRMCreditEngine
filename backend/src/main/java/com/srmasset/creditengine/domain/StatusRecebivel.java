package com.srmasset.creditengine.domain;

/**
 * 3 estados (não 2): depois de um estorno o recebível pode ser liquidado de novo, e ESTORNADO
 * preserva essa distinção semântica em vez de voltar a PENDENTE em silêncio.
 */
public enum StatusRecebivel {
  PENDENTE,
  LIQUIDADO,
  ESTORNADO
}
