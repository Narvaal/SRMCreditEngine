package com.srmasset.creditengine.integration;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provider externo <b>SIMULADO</b> (BACEN/B3/FX no mundo real) — existe pra dar um alvo HTTP de
 * verdade ao retry/circuit breaker de {@link FxProviderClient}, sem depender de serviço externo em
 * dev/CI. Fora do prefixo {@code /api} de propósito: não faz parte da API de negócio.
 *
 * <p>O knob {@code failureRate} (ajustável em runtime via {@code PUT /config}) permite demonstrar o
 * circuit breaker abrindo ao vivo; o contador de chamadas ({@code GET /stats}) permite asserção
 * determinística de que o retry aconteceu (failureRate=1.0 + 3 tentativas ⇒ contador +3).
 */
@Tag(name = "Mock FX Provider (simulação — não é API de negócio)")
@RestController
@RequestMapping("/mock/fx-provider")
public class MockFxProviderController {

  private static final BigDecimal USD_BRL_BASE = new BigDecimal("5.40");
  private static final BigDecimal CDI_BASE = new BigDecimal("0.010000");
  private static final BigDecimal SOFR_BASE = new BigDecimal("0.004500");

  private final AtomicReference<Double> failureRate;
  private final AtomicLong totalChamadas = new AtomicLong();

  public MockFxProviderController(
      @Value("${mock.fx-provider.failure-rate:0.0}") double failureRateInicial) {
    this.failureRate = new AtomicReference<>(clamp(failureRateInicial));
  }

  @GetMapping("/cotacoes")
  public ResponseEntity<CotacoesProviderResponse> cotacoes() {
    totalChamadas.incrementAndGet();
    if (ThreadLocalRandom.current().nextDouble() < failureRate.get()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    // Jitter em torno de valores base — cotações "vivas" o suficiente pra demo, com as escalas
    // das colunas reais (taxa_cambio NUMERIC(19,8), taxa_mercado NUMERIC(9,6)).
    BigDecimal usdBrl = comJitter(USD_BRL_BASE, 0.05, 8);
    BigDecimal brlUsd = BigDecimal.ONE.divide(usdBrl, 8, RoundingMode.HALF_EVEN);
    BigDecimal cdi = comJitter(CDI_BASE, 0.0005, 6);
    BigDecimal sofr = comJitter(SOFR_BASE, 0.0002, 6);

    return ResponseEntity.ok(
        new CotacoesProviderResponse(usdBrl, brlUsd, cdi, sofr, Instant.now()));
  }

  /**
   * Valores fora de [0,1] são clampados em vez de rejeitados — knob de demo, não input de negócio.
   */
  @PutMapping("/config")
  public ResponseEntity<MockFxProviderStats> configurar(@RequestParam double failureRate) {
    this.failureRate.set(clamp(failureRate));
    return stats();
  }

  @GetMapping("/stats")
  public ResponseEntity<MockFxProviderStats> stats() {
    return ResponseEntity.ok(new MockFxProviderStats(totalChamadas.get(), failureRate.get()));
  }

  private static BigDecimal comJitter(BigDecimal base, double amplitude, int escala) {
    double delta = ThreadLocalRandom.current().nextDouble(-amplitude, amplitude);
    return base.add(BigDecimal.valueOf(delta)).setScale(escala, RoundingMode.HALF_EVEN);
  }

  private static double clamp(double valor) {
    return Math.clamp(valor, 0.0, 1.0);
  }

  public record MockFxProviderStats(long totalChamadas, double failureRate) {}
}
