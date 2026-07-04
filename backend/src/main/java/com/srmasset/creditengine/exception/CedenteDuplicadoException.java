package com.srmasset.creditengine.exception;

public class CedenteDuplicadoException extends NegocioException {

  public CedenteDuplicadoException(String documento) {
    super("CEDENTE_DUPLICADO", "Já existe um cedente cadastrado com o documento: " + documento);
  }
}
