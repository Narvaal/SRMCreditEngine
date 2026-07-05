package com.srmasset.creditengine.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExtratoLiquidacaoLinha(
    UUID id,
    UUID recebivelId,
    UUID cedenteId,
    String cedenteNome,
    String tipo,
    String moedaTitulo,
    String moedaPagamento,
    BigDecimal valorFace,
    BigDecimal valorLiquido,
    Instant criadoEm,
    boolean estornada) {}
