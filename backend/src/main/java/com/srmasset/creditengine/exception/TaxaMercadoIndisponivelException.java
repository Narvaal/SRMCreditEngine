package com.srmasset.creditengine.exception;

public class TaxaMercadoIndisponivelException extends NegocioException {

  public TaxaMercadoIndisponivelException(String moeda, String indicador) {
    super(
        "TAXA_MERCADO_INDISPONIVEL",
        "Nenhuma taxa de mercado (%s) vigente cadastrada para %s".formatted(indicador, moeda));
  }
}
