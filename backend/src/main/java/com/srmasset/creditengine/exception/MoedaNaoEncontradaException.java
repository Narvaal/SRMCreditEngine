package com.srmasset.creditengine.exception;

public class MoedaNaoEncontradaException extends NegocioException {

  public MoedaNaoEncontradaException(String codigo) {
    super("MOEDA_NAO_ENCONTRADA", "Moeda não cadastrada: " + codigo);
  }
}
