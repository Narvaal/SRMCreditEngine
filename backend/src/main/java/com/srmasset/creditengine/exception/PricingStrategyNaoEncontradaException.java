package com.srmasset.creditengine.exception;

/**
 * O código existe no catálogo {@code tipo_recebivel}, mas nenhum {@link
 * com.srmasset.creditengine.pricing.PricingStrategy} está registrado pra ele — bug de configuração
 * (alguém cadastrou um tipo novo sem implementar/registrar a Strategy correspondente), não erro do
 * cliente. Distinta de {@link TipoRecebivelNaoSuportadoException}, que é "código nem existe no
 * catálogo".
 */
public class PricingStrategyNaoEncontradaException extends NegocioException {

  public PricingStrategyNaoEncontradaException(String tipoRecebivelCodigo) {
    super(
        "PRICING_STRATEGY_NAO_ENCONTRADA",
        "Nenhum PricingStrategy registrado para o tipo de recebível: " + tipoRecebivelCodigo);
  }
}
