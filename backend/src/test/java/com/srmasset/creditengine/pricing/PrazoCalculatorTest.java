package com.srmasset.creditengine.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PrazoCalculatorTest {

  // "Hoje" fixo via Clock injetado — determinístico, não depende de LocalDate.now() real.
  private final Clock hoje01Jan2026 =
      Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
  private final PrazoCalculator calculator = new PrazoCalculator(hoje01Jan2026);

  @Test
  void trintaDias_eUmMes() {
    assertThat(calculator.calcularPrazoMeses(LocalDate.of(2026, 1, 31)))
        .isEqualByComparingTo("1.0000");
  }

  @Test
  void quarentaECincoDias_eUmEMeioMes() {
    assertThat(calculator.calcularPrazoMeses(LocalDate.of(2026, 2, 15)))
        .isEqualByComparingTo("1.5000");
  }

  @Test
  void vencimentoHoje_ePrazoZero() {
    assertThat(calculator.calcularPrazoMeses(LocalDate.of(2026, 1, 1)))
        .isEqualByComparingTo("0.0000");
  }
}
