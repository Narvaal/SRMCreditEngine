package com.srmasset.creditengine.service;

import com.srmasset.creditengine.exception.ProviderIndisponivelException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sincronização periódica com o provider — desligada por padrão ({@code fx-provider.sync.enabled});
 * o {@code docker-compose} liga via env pra manter o circuit breaker ciclando e visível nas
 * métricas do Prometheus/Grafana sem intervenção manual.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fx-provider.sync.enabled", havingValue = "true")
public class SincronizacaoTaxasScheduler {

  private final SincronizacaoTaxasService sincronizacaoTaxasService;

  @Scheduled(fixedDelayString = "${fx-provider.sync.fixed-delay:60000}")
  public void sincronizarPeriodicamente() {
    try {
      sincronizacaoTaxasService.sincronizar();
    } catch (ProviderIndisponivelException e) {
      // Já logado (WARN estruturado) pelo service — aqui só garantimos que o scheduler não morre.
    }
  }
}
