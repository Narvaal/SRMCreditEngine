package com.srmasset.creditengine.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Métricas de negócio expostas ao Prometheus (contadores por moeda de pagamento).
 *
 * <p>O valor monetário vira {@code double} aqui de propósito: métrica é telemetria operacional
 * (tendência, taxa, ordem de grandeza), não contabilidade — a fonte de verdade contábil é o ledger
 * de {@code liquidacao}, que segue 100% em {@code BigDecimal}/NUMERIC.
 */
@Component
@RequiredArgsConstructor
public class MetricasNegocio {

  private final MeterRegistry registry;

  public void registrarLiquidacao(String moedaPagamento, BigDecimal valorLiquido) {
    contador("srm.liquidacoes", moedaPagamento).increment();
    contador("srm.liquidacoes.valor", moedaPagamento).increment(valorLiquido.doubleValue());
  }

  public void registrarEstorno(String moedaPagamento, BigDecimal valorDevolvido) {
    contador("srm.estornos", moedaPagamento).increment();
    contador("srm.estornos.valor", moedaPagamento).increment(valorDevolvido.doubleValue());
  }

  private Counter contador(String nome, String moeda) {
    return registry.counter(nome, "moeda", moeda);
  }
}
