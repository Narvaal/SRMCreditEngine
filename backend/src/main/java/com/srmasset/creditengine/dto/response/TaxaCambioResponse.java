package com.srmasset.creditengine.dto.response;

import com.srmasset.creditengine.domain.TaxaCambio;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TaxaCambioResponse(
    UUID id, String moedaOrigem, String moedaDestino, BigDecimal valor, Instant vigenteEm) {

  public static TaxaCambioResponse de(TaxaCambio taxaCambio) {
    return new TaxaCambioResponse(
        taxaCambio.getId(),
        taxaCambio.getMoedaOrigem().getCodigo(),
        taxaCambio.getMoedaDestino().getCodigo(),
        taxaCambio.getValor(),
        taxaCambio.getVigenteEm());
  }
}
