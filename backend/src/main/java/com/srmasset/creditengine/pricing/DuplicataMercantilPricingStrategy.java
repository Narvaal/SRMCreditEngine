package com.srmasset.creditengine.pricing;

import com.srmasset.creditengine.domain.Recebivel;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class DuplicataMercantilPricingStrategy implements PricingStrategy {

  public static final String CODIGO = "DUPLICATA_MERCANTIL";
  private static final BigDecimal SPREAD_MENSAL = new BigDecimal("0.015000");

  @Override
  public String getTipoRecebivelCodigo() {
    return CODIGO;
  }

  @Override
  public BigDecimal getSpread(Recebivel recebivel) {
    return SPREAD_MENSAL;
  }
}
