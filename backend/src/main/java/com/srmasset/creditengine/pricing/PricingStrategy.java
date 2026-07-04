package com.srmasset.creditengine.pricing;

import com.srmasset.creditengine.domain.Recebivel;
import java.math.BigDecimal;

/**
 * Strategy Pattern: desacopla a <b>regra de risco</b> (o spread de cada tipo de recebível) do
 * <b>cálculo</b> em si (a fórmula de valor presente, compartilhada por todos os tipos em {@link
 * MotorPrecificacao}). Uma implementação por {@code tipo_recebivel.codigo}.
 */
public interface PricingStrategy {

  /**
   * Casa com {@code tipo_recebivel.codigo} — chave de dispatch do {@link PricingStrategyResolver}.
   */
  String getTipoRecebivelCodigo();

  /**
   * Recebe a entidade (não um getter sem parâmetro) para não fechar a porta a um spread condicional
   * no futuro (ex: por faixa de valor de face) sem quebrar a interface — hoje toda implementação
   * retorna uma constante fixa.
   */
  BigDecimal getSpread(Recebivel recebivel);
}
