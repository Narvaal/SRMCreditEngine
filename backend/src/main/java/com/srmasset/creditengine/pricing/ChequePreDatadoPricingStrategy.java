package com.srmasset.creditengine.pricing;

import com.srmasset.creditengine.domain.Recebivel;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class ChequePreDatadoPricingStrategy implements PricingStrategy {

  public static final String CODIGO = "CHEQUE_PRE_DATADO";
  private static final BigDecimal SPREAD_MENSAL = new BigDecimal("0.025000");

  @Override
  public String getTipoRecebivelCodigo() {
    return CODIGO;
  }

  @Override
  public BigDecimal getSpread(Recebivel recebivel) {
    return SPREAD_MENSAL;
  }
}
