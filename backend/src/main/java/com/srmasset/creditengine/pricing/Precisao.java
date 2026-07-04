package com.srmasset.creditengine.pricing;

import java.math.MathContext;
import java.math.RoundingMode;

/** Constantes de precisão/arredondamento centralizadas — nunca espalhadas pelo código. */
public final class Precisao {

  /** Escala de {@code liquidacao.valor_presente} (NUMERIC(20,6)). */
  public static final int ESCALA_VALOR_PRESENTE = 6;

  /** Escala de {@code liquidacao.prazo_meses_usado} (NUMERIC(9,4)). */
  public static final int ESCALA_PRAZO = 4;

  /** Escala de valores monetários finais, ex: {@code valor_liquido} (NUMERIC(18,2)). */
  public static final int ESCALA_VALOR_MONETARIO = 2;

  /**
   * Bankers' rounding — padrão financeiro/atuarial, reduz viés cumulativo de arredondamento em lote
   * (ao contrário de HALF_UP, que sempre arredonda ".5" pra cima).
   */
  public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;

  /**
   * MathContext usado só no cálculo intermediário (potência fracionária via big-math) — mais casas
   * que qualquer escala final, absorve a incerteza do último dígito de funções transcendentais
   * antes do arredondamento explícito para a escala persistida.
   */
  public static final MathContext CALCULO = new MathContext(40, ARREDONDAMENTO);

  private Precisao() {}
}
