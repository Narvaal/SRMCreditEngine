package com.srmasset.creditengine.exception;

import java.util.UUID;

public class RecebivelNaoEncontradoException extends NegocioException {

  public RecebivelNaoEncontradoException(UUID recebivelId) {
    super("RECEBIVEL_NAO_ENCONTRADO", "Recebível não encontrado: " + recebivelId);
  }
}
