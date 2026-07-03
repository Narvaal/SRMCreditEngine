package com.srmasset.creditengine.dto.response;

import java.math.BigDecimal;

/**
 * Espelha {@link LiquidacaoResponse} sem os campos de persistência (id, recebivelId, criadoEm,
 * etc.) — nada aqui foi salvo.
 */
public record SimulacaoRecebivelResponse(
    BigDecimal valorFace,
    String moedaTitulo,
    BigDecimal taxaBaseUsada,
    BigDecimal spreadUsado,
    BigDecimal prazoMesesUsado,
    BigDecimal valorPresente,
    String moedaPagamento,
    BigDecimal taxaCambioUsada,
    BigDecimal valorLiquido) {}
