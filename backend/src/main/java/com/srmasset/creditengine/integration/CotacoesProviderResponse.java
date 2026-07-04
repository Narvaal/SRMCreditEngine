package com.srmasset.creditengine.integration;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Contrato do "BACEN/FX provider": cotações de câmbio (nas duas direções, evitando decisão de
 * arredondamento de inversa do lado consumidor) e taxas de mercado (CDI/SOFR) vigentes no instante
 * da consulta. Compartilhado entre {@link FxProviderClient} (consumidor) e {@link
 * MockFxProviderController} (implementação simulada).
 */
public record CotacoesProviderResponse(
    BigDecimal usdBrl, BigDecimal brlUsd, BigDecimal cdi, BigDecimal sofr, Instant vigenteEm) {}
