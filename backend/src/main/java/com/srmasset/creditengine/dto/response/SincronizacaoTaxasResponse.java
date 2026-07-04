package com.srmasset.creditengine.dto.response;

import com.srmasset.creditengine.integration.CotacoesProviderResponse;
import java.math.BigDecimal;
import java.time.Instant;

public record SincronizacaoTaxasResponse(
    BigDecimal usdBrl, BigDecimal brlUsd, BigDecimal cdi, BigDecimal sofr, Instant vigenteEm) {

  public static SincronizacaoTaxasResponse de(CotacoesProviderResponse cotacoes) {
    return new SincronizacaoTaxasResponse(
        cotacoes.usdBrl(),
        cotacoes.brlUsd(),
        cotacoes.cdi(),
        cotacoes.sofr(),
        cotacoes.vigenteEm());
  }
}
