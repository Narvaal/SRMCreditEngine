package com.srmasset.creditengine.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Converte {@code data_vencimento} em meses fracionários usando a convenção decidida (dias_corridos
 * / 30 — mês fixo de 30 dias, não o calendário real). {@link Clock} é injetado (não {@code
 * LocalDate.now()} direto) para o cálculo ser determinístico em teste.
 */
@Component
public class PrazoCalculator {

  private static final BigDecimal DIAS_POR_MES = new BigDecimal("30");

  private final Clock clock;

  public PrazoCalculator(Clock clock) {
    this.clock = clock;
  }

  /**
   * Escala 4 casas — casa exatamente com {@code liquidacao.prazo_meses_usado} (NUMERIC(9,4)),
   * garantindo que o prazo usado no cálculo é idêntico ao prazo persistido para
   * auditoria/reconstituição.
   */
  public BigDecimal calcularPrazoMeses(LocalDate dataVencimento) {
    LocalDate hoje = LocalDate.now(clock);
    long dias = ChronoUnit.DAYS.between(hoje, dataVencimento);
    return BigDecimal.valueOf(dias)
        .divide(DIAS_POR_MES, Precisao.ESCALA_PRAZO, RoundingMode.HALF_EVEN);
  }
}
