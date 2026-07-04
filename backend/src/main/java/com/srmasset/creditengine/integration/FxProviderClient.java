package com.srmasset.creditengine.integration;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client HTTP do provider de taxas, protegido por Resilience4j (config em {@code application.yml},
 * instância {@code fxProvider}).
 *
 * <p>Ordem dos aspectos (default do Resilience4j): Retry é o mais externo — cada tentativa de retry
 * passa pelo CircuitBreaker e conta na janela dele. Com o circuito aberto, {@code
 * CallNotPermittedException} está em {@code ignoreExceptions} do retry, então a falha é imediata
 * (short-circuit de verdade, sem re-tentar contra um circuito aberto).
 *
 * <p>Deliberadamente <b>sem</b> {@code fallbackMethod}: a tradução de falha pra exceção de domínio
 * acontece em {@code SincronizacaoTaxasService}, seguindo o padrão do projeto de tradução explícita
 * via try/catch (mesmo estilo de LiquidacaoService/CedenteService) em vez de fallback via AOP.
 */
@Component
public class FxProviderClient {

  private final RestClient restClient;

  public FxProviderClient(
      RestClient.Builder restClientBuilder, @Value("${fx-provider.base-url}") String baseUrl) {
    this.restClient = restClientBuilder.baseUrl(baseUrl).build();
  }

  @Retry(name = "fxProvider")
  @CircuitBreaker(name = "fxProvider")
  public CotacoesProviderResponse buscarCotacoes() {
    return restClient.get().uri("/cotacoes").retrieve().body(CotacoesProviderResponse.class);
  }
}
