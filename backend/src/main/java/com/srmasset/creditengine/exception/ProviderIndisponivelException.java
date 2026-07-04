package com.srmasset.creditengine.exception;

/**
 * Provider externo de taxas fora do ar (retry esgotado ou circuit breaker aberto). Semântica de
 * degradação: só a <b>atualização</b> de taxas falha — liquidação e simulação continuam operando
 * normalmente com a última taxa vigente persistida (garantido pelo histórico append-only de {@code
 * taxa_cambio}/{@code taxa_mercado}).
 */
public class ProviderIndisponivelException extends NegocioException {

  public ProviderIndisponivelException(String motivo) {
    super(
        "PROVIDER_INDISPONIVEL",
        "Provider de taxas indisponível (%s) — o sistema segue operando com a última taxa vigente persistida"
            .formatted(motivo));
  }
}
