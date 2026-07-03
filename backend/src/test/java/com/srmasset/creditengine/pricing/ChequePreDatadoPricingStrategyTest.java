package com.srmasset.creditengine.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import com.srmasset.creditengine.domain.Recebivel;
import org.junit.jupiter.api.Test;

class ChequePreDatadoPricingStrategyTest {

  private final ChequePreDatadoPricingStrategy strategy = new ChequePreDatadoPricingStrategy();

  @Test
  void codigoCasaComCatalogo() {
    assertThat(strategy.getTipoRecebivelCodigo()).isEqualTo("CHEQUE_PRE_DATADO");
  }

  @Test
  void spreadEhDoisEMeioPorcentoAoMes() {
    assertThat(strategy.getSpread(Recebivel.builder().build())).isEqualByComparingTo("0.025");
  }
}
