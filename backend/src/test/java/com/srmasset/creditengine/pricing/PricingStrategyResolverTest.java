package com.srmasset.creditengine.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.srmasset.creditengine.exception.PricingStrategyNaoEncontradaException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricingStrategyResolverTest {

  private final PricingStrategyResolver resolver =
      new PricingStrategyResolver(
          List.of(new DuplicataMercantilPricingStrategy(), new ChequePreDatadoPricingStrategy()));

  @Test
  void resolvePorCodigoCadastrado() {
    assertThat(resolver.resolver("DUPLICATA_MERCANTIL"))
        .isInstanceOf(DuplicataMercantilPricingStrategy.class);
    assertThat(resolver.resolver("CHEQUE_PRE_DATADO"))
        .isInstanceOf(ChequePreDatadoPricingStrategy.class);
  }

  @Test
  void codigoSemStrategyRegistradaLancaExcecaoDeConfiguracao() {
    assertThatThrownBy(() -> resolver.resolver("TIPO_INEXISTENTE"))
        .isInstanceOf(PricingStrategyNaoEncontradaException.class);
  }
}
