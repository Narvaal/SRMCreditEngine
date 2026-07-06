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
    /**
     * Preenchidos só em linhas ESTORNO: referência da liquidação desfeita (o frontend funde as
     * duas). Liquidações já estornadas não aparecem no extrato — a linha do estorno as representa.
     */
    UUID liquidacaoEstornadaId,
    Instant liquidacaoEstornadaCriadoEm) {}
