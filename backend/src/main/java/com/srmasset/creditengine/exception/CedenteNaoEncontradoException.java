package com.srmasset.creditengine.exception;

import java.util.UUID;

public class CedenteNaoEncontradoException extends NegocioException {

  public CedenteNaoEncontradoException(UUID cedenteId) {
    super("CEDENTE_NAO_ENCONTRADO", "Cedente não encontrado: " + cedenteId);
  }
}
