package com.srmasset.creditengine.pricing;

import com.srmasset.creditengine.exception.PricingStrategyNaoEncontradaException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Spring injeta automaticamente todos os beans {@link PricingStrategy} — sem if/else crescente. */
@Component
public class PricingStrategyResolver {

  private final Map<String, PricingStrategy> estrategiasPorCodigo;

  public PricingStrategyResolver(List<PricingStrategy> estrategias) {
    this.estrategiasPorCodigo =
        estrategias.stream()
            .collect(
                Collectors.toMap(PricingStrategy::getTipoRecebivelCodigo, Function.identity()));
  }

  public PricingStrategy resolver(String tipoRecebivelCodigo) {
    PricingStrategy estrategia = estrategiasPorCodigo.get(tipoRecebivelCodigo);
    if (estrategia == null) {
      throw new PricingStrategyNaoEncontradaException(tipoRecebivelCodigo);
    }
    return estrategia;
  }
}
