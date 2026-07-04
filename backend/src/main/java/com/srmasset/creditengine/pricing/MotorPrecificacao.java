package com.srmasset.creditengine.pricing;

import ch.obermuhlner.math.big.BigDecimalMath;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Fórmula compartilhada por todos os tipos de recebível: {@code VP = ValorFace / (1 + TaxaBase +
 * Spread) ^ PrazoMeses}. Único ponto do sistema que referencia {@code big-math} diretamente — o
 * resto do código trabalha só com {@link BigDecimal}.
 */
@Component
public class MotorPrecificacao {

  public BigDecimal calcularValorPresente(
      BigDecimal valorFace, BigDecimal taxaBase, BigDecimal spread, BigDecimal prazoMeses) {
    BigDecimal base = BigDecimal.ONE.add(taxaBase).add(spread);
    if (base.signum() <= 0) {
      throw new IllegalArgumentException(
          "(1 + taxaBase + spread) deve ser positivo para a fórmula ser válida, obtido: " + base);
    }

    BigDecimal fator = BigDecimalMath.pow(base, prazoMeses, Precisao.CALCULO);

    return valorFace
        .divide(fator, Precisao.CALCULO)
        .setScale(Precisao.ESCALA_VALOR_PRESENTE, Precisao.ARREDONDAMENTO);
  }
}
