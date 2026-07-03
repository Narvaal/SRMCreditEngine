package com.srmasset.creditengine.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MotorPrecificacaoTest {

  private final MotorPrecificacao motor = new MotorPrecificacao();

  @Test
  @DisplayName("prazo zero: VP = valor de face, sem desconto")
  void prazoZero_naoDescontaNada() {
    BigDecimal vp =
        motor.calcularValorPresente(
            new BigDecimal("1000.00"),
            new BigDecimal("0.01"),
            new BigDecimal("0.015"),
            BigDecimal.ZERO);

    assertThat(vp).isEqualByComparingTo("1000.000000");
  }

  @Test
  @DisplayName("expoente fracionário 0,5: base = 1,44 = 1,2^2, então 1,44^0,5 = 1,2 exatamente")
  void expoenteFracionario_meioMes() {
    // base = 1 + 0 + 0.44 = 1.44 = 1.2^2  =>  1.44^0.5 = 1.2 (raiz exata, verificável na mão)
    BigDecimal vp =
        motor.calcularValorPresente(
            new BigDecimal("1200.00"),
            BigDecimal.ZERO,
            new BigDecimal("0.44"),
            new BigDecimal("0.5"));

    assertThat(vp).isEqualByComparingTo("1000.000000");
  }

  @Test
  @DisplayName(
      "expoente fracionário 1,5: 1,44^1,5 = 1,44 * sqrt(1,44) = 1,44 * 1,2 = 1,728 exatamente")
  void expoenteFracionario_umEMeioMes() {
    BigDecimal vp =
        motor.calcularValorPresente(
            new BigDecimal("1728.00"),
            BigDecimal.ZERO,
            new BigDecimal("0.44"),
            new BigDecimal("1.5"));

    assertThat(vp).isEqualByComparingTo("1000.000000");
  }

  @Test
  @DisplayName("expoente inteiro: big-math deve concordar com BigDecimal.pow(int) nativo")
  void expoenteInteiro_concordaComPowNativo() {
    BigDecimal valorFace = new BigDecimal("100000.00");
    BigDecimal taxaBase = new BigDecimal("0.01");
    BigDecimal spread = new BigDecimal("0.015");
    BigDecimal base = BigDecimal.ONE.add(taxaBase).add(spread); // 1.025

    // Ground truth independente: BigDecimal.pow(int) nativo, só funciona pra expoente inteiro.
    BigDecimal fatorNativo = base.pow(3);
    BigDecimal vpEsperado =
        valorFace
            .divide(fatorNativo, Precisao.CALCULO)
            .setScale(Precisao.ESCALA_VALOR_PRESENTE, Precisao.ARREDONDAMENTO);

    BigDecimal vp = motor.calcularValorPresente(valorFace, taxaBase, spread, new BigDecimal("3"));

    assertThat(vp).isCloseTo(vpEsperado, within(new BigDecimal("0.000001")));
  }

  @Test
  @DisplayName("cenário de negócio: duplicata mercantil sempre desconta (VP < valor de face)")
  void cenarioDuplicataMercantil_semprePositivoEMenorQueValorFace() {
    BigDecimal valorFace = new BigDecimal("50000.00");
    BigDecimal vp =
        motor.calcularValorPresente(
            valorFace,
            new BigDecimal("0.010000"),
            new BigDecimal("0.015000"),
            new BigDecimal("1.5000"));

    assertThat(vp).isPositive();
    assertThat(vp).isLessThan(valorFace);
  }

  @Test
  @DisplayName(
      "(1 + taxaBase + spread) não positivo deve falhar explicitamente, nunca gerar resultado indefinido")
  void baseNaoPositiva_lancaExcecao() {
    assertThatThrownBy(
            () ->
                motor.calcularValorPresente(
                    new BigDecimal("1000"),
                    new BigDecimal("-2"),
                    BigDecimal.ZERO,
                    new BigDecimal("1.5")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
