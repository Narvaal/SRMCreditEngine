package com.srmasset.creditengine.exception;

import java.util.UUID;

public class EstornoInvalidoException extends NegocioException {

  public EstornoInvalidoException(UUID liquidacaoId, String motivo) {
    super(
        "ESTORNO_INVALIDO",
        "Não é possível estornar a liquidação %s: %s".formatted(liquidacaoId, motivo));
  }
}
