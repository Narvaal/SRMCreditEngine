package com.srmasset.creditengine.exception;

import java.math.BigDecimal;

public class SaldoInsuficienteException extends NegocioException {

  public SaldoInsuficienteException(
      String moeda, BigDecimal saldoDisponivel, BigDecimal valorNecessario) {
    super(
        "SALDO_INSUFICIENTE",
        "Saldo insuficiente em caixa %s: disponível %s, necessário %s"
            .formatted(moeda, saldoDisponivel, valorNecessario));
  }
}
