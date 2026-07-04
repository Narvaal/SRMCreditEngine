package com.srmasset.creditengine.dto.response;

import com.srmasset.creditengine.domain.TaxaMercado;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TaxaMercadoResponse(
    UUID id, String moedaCodigo, String indicador, BigDecimal valor, Instant vigenteEm) {

  public static TaxaMercadoResponse de(TaxaMercado taxaMercado) {
    return new TaxaMercadoResponse(
        taxaMercado.getId(),
        taxaMercado.getMoeda().getCodigo(),
        taxaMercado.getIndicador(),
        taxaMercado.getValor(),
        taxaMercado.getVigenteEm());
  }
}
