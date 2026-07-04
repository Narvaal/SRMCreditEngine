package com.srmasset.creditengine.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import com.srmasset.creditengine.domain.Recebivel;
import org.junit.jupiter.api.Test;

class DuplicataMercantilPricingStrategyTest {

  private final DuplicataMercantilPricingStrategy strategy =
      new DuplicataMercantilPricingStrategy();

  @Test
  void codigoCasaComCatalogo() {
    assertThat(strategy.getTipoRecebivelCodigo()).isEqualTo("DUPLICATA_MERCANTIL");
  }

  @Test
  void spreadEhUmEMeioPorcentoAoMes() {
    assertThat(strategy.getSpread(Recebivel.builder().build())).isEqualByComparingTo("0.015");
  }
}
