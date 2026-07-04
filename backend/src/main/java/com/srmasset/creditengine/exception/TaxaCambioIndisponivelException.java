package com.srmasset.creditengine.exception;

public class TaxaCambioIndisponivelException extends NegocioException {

  public TaxaCambioIndisponivelException(String moedaOrigem, String moedaDestino) {
    super(
        "TAXA_CAMBIO_INDISPONIVEL",
        "Nenhuma taxa de câmbio vigente cadastrada para %s -> %s"
            .formatted(moedaOrigem, moedaDestino));
  }
}
