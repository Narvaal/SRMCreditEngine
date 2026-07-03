package com.srmasset.creditengine.exception;

import com.srmasset.creditengine.domain.StatusRecebivel;
import java.util.UUID;

public class RecebivelJaLiquidadoException extends NegocioException {

  public RecebivelJaLiquidadoException(UUID recebivelId, StatusRecebivel statusAtual) {
    super(
        "RECEBIVEL_JA_LIQUIDADO",
        "Recebível %s não está PENDENTE (status atual: %s)".formatted(recebivelId, statusAtual));
  }
}
